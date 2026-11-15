package pleos.gilwe.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class GearState { PARK, DRIVE, REVERSE, NEUTRAL, UNKNOWN }

/**
 * 길 위가 보는 차량 상태.
 * - gearReadable=false 면 기어를 읽지 못한 것 → 주행 진행은 시뮬레이션, 체크인은 사용자 확인으로 대체.
 */
data class VehicleState(
    val gear: GearState = GearState.UNKNOWN,
    val gearReadable: Boolean = false,
) {
    val isParked get() = gear == GearState.PARK
    val isDriving get() = gear == GearState.DRIVE || gear == GearState.REVERSE
}

/**
 * Pleos Vehicle SDK 2.3.2 — Gear API.
 * - D단: 경로 안내 화면에서 이야기 진행
 * - P단: 도착 후 자동 체크인(스탬프 수집)
 * 모든 호출은 실패할 수 있다고 보고(퍼미션·차종·에뮬레이터), 실패하면 gearReadable=false 로 둔다.
 */
class PleosVehicle(context: Context) {

    private val appContext = context.applicationContext
    private var vehicle: Vehicle? = null

    private val _state = MutableStateFlow(VehicleState())
    val state: StateFlow<VehicleState> = _state.asStateFlow()

    /** 시연용: 실제 기어 대신 강제로 넣는 값 (null = 실제 값) */
    private val _override = MutableStateFlow<GearState?>(null)
    val override: StateFlow<GearState?> = _override.asStateFlow()
    private var realGear = VehicleState()

    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) {
            publish(VehicleState(gear.toGearState(), gearReadable = true))
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "gear listener failed", e)
            publish(realGear.copy(gearReadable = false))
        }
    }

    fun start() {
        if (vehicle != null) return
        val v = runCatching { Vehicle(appContext).also { it.initialize() } }
            .onFailure { Log.w(TAG, "Vehicle init failed", it) }
            .getOrNull() ?: return
        vehicle = v
        runCatching {
            v.getGear().getCurrentGearSelectionState(
                { gear -> publish(VehicleState(gear.toGearState(), gearReadable = true)) },
                { e -> Log.w(TAG, "gear read failed", e); publish(realGear.copy(gearReadable = false)) },
            )
            v.getGear().registerGearSelection(gearListener)
        }.onFailure { Log.w(TAG, "gear api unavailable", it) }
    }

    fun stop() {
        val v = vehicle ?: return
        runCatching { v.getGear().unregisterGearSelection(gearListener) }
        runCatching { v.release() }
        vehicle = null
    }

    fun setOverride(gear: GearState?) {
        _override.value = gear
        publish(realGear)
    }

    private fun publish(real: VehicleState) {
        realGear = real
        val o = _override.value
        _state.update { if (o != null) VehicleState(o, gearReadable = true) else real }
    }

    private fun VehicleGear?.toGearState(): GearState = when (this) {
        VehicleGear.GEAR_PARK -> GearState.PARK
        VehicleGear.GEAR_REVERSE -> GearState.REVERSE
        VehicleGear.GEAR_NEUTRAL -> GearState.NEUTRAL
        VehicleGear.GEAR_UNKNOWN, null -> GearState.UNKNOWN
        else -> GearState.DRIVE // D, 1~9단
    }

    private companion object {
        const val TAG = "GilweVehicle"
    }
}
