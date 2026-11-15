package pleos.gilwe

import android.app.Application
import pleos.gilwe.car.DrivingRestriction
import pleos.gilwe.data.StampStore
import pleos.gilwe.navi.PleosNavi
import pleos.gilwe.navi.GpsSource
import pleos.gilwe.vehicle.PleosVehicle
import pleos.gilwe.voice.StoryNotifier
import pleos.gilwe.voice.StoryVoice

class GilweApp : Application() {
    lateinit var vehicle: PleosVehicle
        private set
    lateinit var restriction: DrivingRestriction
        private set
    lateinit var stamps: StampStore
        private set
    lateinit var voice: StoryVoice
        private set
    lateinit var navi: PleosNavi
        private set
    lateinit var notifier: StoryNotifier
        private set
    lateinit var gps: GpsSource
        private set

    override fun onCreate() {
        super.onCreate()
        vehicle = PleosVehicle(this)
        restriction = DrivingRestriction(this)
        stamps = StampStore(this)
        voice = StoryVoice(this)
        // 내비 연동은 앱 전체 수명 동안 유지: 내비 화면이 앞에 있어도 이벤트를 받아 이야기를 이어간다
        navi = PleosNavi(this).also { it.start() }
        notifier = StoryNotifier(this)
        gps = GpsSource(this)
    }
}
