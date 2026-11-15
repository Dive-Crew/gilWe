package pleos.gilwe.navi

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DrivingInfo
import ai.pleos.playground.navi.data.NavigationScreenRequest
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 길 위 목적지(코스)의 내비 좌표 */
data class NaviTarget(val lat: Double, val lng: Double, val name: String, val address: String)

/** 내비가 알려 준 안내 상태 */
data class NaviState(
    /** NaviHelper 서비스에 연결됨 */
    val connected: Boolean = false,
    /** 길 위가 요청한 경로를 내비가 안내 중 */
    val guiding: Boolean = false,
    /** 경로 탐색 시작 시 받은 전체 거리(m) */
    val totalMeters: Int = 0,
    /** 목적지까지 남은 거리(m), 모르면 null */
    val remainMeters: Int? = null,
    val remainSeconds: Int? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    /** 마지막으로 내비에서 현재 위치를 받은 시각 (위치 신호 없음 판단용) */
    val lastLocationAt: Long? = null,
    val lastError: String? = null,
) {
    /** 0~1 진행률, 내비 정보가 없으면 null */
    val progress: Float?
        get() {
            val r = remainMeters ?: return null
            if (totalMeters <= 0) return null
            return (1f - r.toFloat() / totalMeters).coerceIn(0f, 1f)
        }
}

sealed interface NaviEvent {
    data object RouteReady : NaviEvent
    data object Arrived : NaviEvent
    data object Cancelled : NaviEvent
    data class Failed(val code: String) : NaviEvent
}

/**
 * Pleos NaviHelper SDK 3.0.5 연동.
 * - requestRoute(): 기본 내비에 목적지를 넘겨 실제 경로 안내를 시작
 * - 경로 시작·주행 정보·도착 이벤트를 받아 길 위 이야기 진행과 체크인에 사용
 * 서비스가 없거나 연결이 실패하면 connected=false 로 남고, 앱은 시간 기반 시뮬레이션으로 동작한다.
 */
class PleosNavi(context: Context) {

    private val appContext = context.applicationContext
    private var helper: NaviHelper? = null

    private val _state = MutableStateFlow(NaviState())
    val state: StateFlow<NaviState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<NaviEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<NaviEvent> = _events.asSharedFlow()

    /** 길 위가 경로를 요청했는지. 다른 앱이 건 경로 이벤트와 구분하기 위함 */
    private var ours = false
    /** 새 경로를 요청하고 시작 이벤트를 기다리는 중. 이때 오는 취소는 이전 경로가 교체되며 생긴 것이라 무시한다 */
    private var awaitingStart = false

    private val listener = object : NaviHelperEventListener {
        override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
            Log.i(TAG, "onRouteStarted $routeStartInfo")
            if (!ours) return
            awaitingStart = false
            _state.update {
                it.copy(
                    guiding = true,
                    totalMeters = routeStartInfo.distance,
                    remainMeters = routeStartInfo.distance,
                    remainSeconds = routeStartInfo.duration,
                    lastError = null,
                )
            }
        }

        override fun onRoutingCompleted() {
            Log.i(TAG, "onRoutingCompleted")
            if (ours) _events.tryEmit(NaviEvent.RouteReady)
        }

        override fun onDrivingInfo(drivingInfo: DrivingInfo) {
            if (!ours) return
            val d = drivingInfo.destination
            _state.update {
                it.copy(
                    guiding = true,
                    remainMeters = d.distance,
                    remainSeconds = d.duration,
                    totalMeters = if (it.totalMeters < d.distance) d.distance else it.totalMeters,
                )
            }
        }

        override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) {
            _state.update { it.copy(lat = currentLocationInfo.latitude, lng = currentLocationInfo.longitude, lastLocationAt = System.currentTimeMillis()) }
        }

        override fun onNavigationStatus(navigationStatus: NavigationStatus) {
            Log.i(TAG, "onNavigationStatus $navigationStatus")
            if (ours && navigationStatus != NavigationStatus.ROUTE_DRIVE && _state.value.guiding &&
                navigationStatus != NavigationStatus.INITIALIZING
            ) {
                _state.update { it.copy(guiding = false) }
            }
        }

        override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) {
            Log.i(TAG, "onDestinationArrived $destinationArrivedInfo")
            if (!ours) return
            ours = false
            _state.update { it.copy(guiding = false, remainMeters = 0, remainSeconds = 0) }
            _events.tryEmit(NaviEvent.Arrived)
        }

        override fun onRouteCancelled() {
            Log.i(TAG, "onRouteCancelled")
            if (!ours) return
            if (awaitingStart) return
            ours = false
            _state.update { it.copy(guiding = false) }
            _events.tryEmit(NaviEvent.Cancelled)
        }

        override fun onError(errorCode: NaviErrorCode) {
            Log.w(TAG, "onError $errorCode")
            _state.update { it.copy(lastError = errorCode.toString()) }
            if (ours) _events.tryEmit(NaviEvent.Failed(errorCode.toString()))
        }
    }

    fun start() {
        if (helper != null) return
        val h = runCatching { NaviHelper(appContext) }.onFailure { Log.w(TAG, "NaviHelper create failed", it) }.getOrNull() ?: return
        helper = h
        runCatching {
            h.initialize(
                {
                    Log.i(TAG, "NaviHelper connected")
                    runCatching { h.addListener(listener) }.onFailure { Log.w(TAG, "addListener failed", it) }
                    _state.update { it.copy(connected = true) }
                },
                { e ->
                    Log.w(TAG, "NaviHelper connect failed", e)
                    _state.update { it.copy(connected = false, lastError = e.message) }
                },
            )
        }.onFailure { Log.w(TAG, "NaviHelper init failed", it) }
    }

    /** 기본 내비에 목적지를 설정하고 안내를 시작. 연결되지 않았으면 false */
    fun startGuidance(t: NaviTarget): Boolean {
        val h = helper ?: return false
        if (!_state.value.connected) return false
        return runCatching {
            ours = true
            awaitingStart = true
            _state.update { it.copy(guiding = false, totalMeters = 0, remainMeters = null, remainSeconds = null, lastError = null) }
            h.requestRoute(
                RouteInfo(
                    latitude = t.lat,
                    longitude = t.lng,
                    displayLongitude = null,
                    displayLatitude = null,
                    poiName = t.name,
                    poiId = "",
                    poiSubId = "0",
                    address = t.address,
                ),
            )
            true
        }.onFailure { ours = false; Log.w(TAG, "requestRoute failed", it) }.getOrDefault(false)
    }

    /** 내비에 현재 위치를 요청 (결과는 onCurrentLocationInfo) */
    fun requestLocation() {
        if (!_state.value.connected) return
        runCatching { helper?.getCurrentLocationInfo() }.onFailure { Log.w(TAG, "getCurrentLocationInfo failed", it) }
    }

    /** 길 위가 건 안내만 취소 */
    fun cancelGuidance() {
        if (!ours) return
        awaitingStart = false
        ours = false
        runCatching { helper?.cancelRoute() }.onFailure { Log.w(TAG, "cancelRoute failed", it) }
        _state.update { it.copy(guiding = false, remainMeters = null, totalMeters = 0) }
    }

    /** 내비 화면을 앞으로 띄움 (길 위 이야기는 뒤에서 계속 재생) */
    fun showNavigation(): Boolean {
        val h = helper ?: return false
        return runCatching { h.requestNavigationScreen(NavigationScreenRequest.Foreground); true }
            .onFailure { Log.w(TAG, "requestNavigationScreen failed", it) }.getOrDefault(false)
    }

    fun stop() {
        val h = helper ?: return
        runCatching { h.removeListener(listener) }
        runCatching { h.release() }
        helper = null
        _state.value = NaviState()
    }

    companion object {
        private const val TAG = "GilweNavi"

        /**
         * 코스 id → 내비 목적지 = 경로 미리보기에 안내하는 주차장 (Courses.parking).
         * 좌표: OpenStreetMap 주차장 위치. 화성행궁은 수원시 안내 주소(행궁로 18), 남이섬은 가평 선착장.
         */
        val targets = mapOf(
            "changdeokgung" to NaviTarget(37.57085, 126.99608, "종묘 공영주차장", "서울 종로구 종로 157"),
            "namhansanseong" to NaviTarget(37.47711, 127.18699, "남한산성 중앙주차장", "경기 광주시 남한산성면 남한산성로"),
            "hwaseong" to NaviTarget(37.28086, 127.01465, "화성행궁 공영주차장", "경기 수원시 팔달구 행궁로 18"),
            "folkvillage" to NaviTarget(37.25667, 127.11577, "한국민속촌 주차장", "경기 용인시 기흥구 민속촌로 90"),
            "nami" to NaviTarget(37.80335, 127.52572, "가평 선착장 주차장", "경기 가평군 가평읍 북한강변로 1024"),
        )
    }
}
