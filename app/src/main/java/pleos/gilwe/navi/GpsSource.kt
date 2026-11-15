package pleos.gilwe.navi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 차량 GPS 위치 (Android LocationManager). 내비 위치(NaviHelper onCurrentLocationInfo)와 함께
 * 위치 × 시간 조립에 쓴다. 둘 중 더 최근 값을 쓰고, 둘 다 끊기면 스케줄러가 마지막 속도로 추정한다.
 */
class GpsSource(context: Context) {
    private val ctx = context.applicationContext
    private val lm = ctx.getSystemService(LocationManager::class.java)
    private val _fix = MutableStateFlow<Location?>(null)
    val fix: StateFlow<Location?> = _fix.asStateFlow()
    /** 마지막 위치를 받은 기기 시각 (Location.time 은 GPS 시각이라 기기 시계와 어긋날 수 있음) */
     var receivedAt = 0L
        private set
    private var running = false

    private val listener = LocationListener { loc -> receivedAt = System.currentTimeMillis(); _fix.value = loc }

    fun granted() = ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start() {
        if (running || !granted()) return
        runCatching {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper())
            running = true
        }.onFailure { Log.w("GpsSource", "location updates failed", it) }
    }

    fun stop() {
        if (!running) return
        runCatching { lm.removeUpdates(listener) }
        running = false
    }
}
