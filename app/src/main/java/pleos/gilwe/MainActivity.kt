package pleos.gilwe

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import pleos.gilwe.ui.AppViewModel
import pleos.gilwe.ui.GilweRoot
import pleos.gilwe.ui.GilweTheme
import pleos.gilwe.ui.NaviOverlay
import pleos.gilwe.ui.Screen

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()
    private val app get() = application as GilweApp
    private lateinit var overlay: NaviOverlay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overlay = NaviOverlay(this, vm)
        setContent {
            GilweTheme {
                GilweRoot(vm, onExit = { finish() })
            }
        }
        qa(intent)
        // 내비 위 오버레이가 떠 있는 동안 안내가 끝나거나 도착하면 오버레이를 내리고 길 위를 다시 앞으로
        lifecycleScope.launch {
            combine(vm.trip, vm.screen) { t, s -> t != null && s == Screen.Drive }.collect { driving ->
                if (!driving && overlay.armed) {
                    overlay.disarm()
                    vm.overlayShowing = false
                    if (vm.screen.value != Screen.Home) {
                        startActivity(Intent(this@MainActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        qa(intent)
    }

    /** 디버그 빌드 전용 화면 검수 진입점 (AppViewModel.qaOpen) */
    private fun qa(i: Intent?) {
        if (!BuildConfig.DEBUG) return
        val screen = i?.getStringExtra("qa_screen") ?: return
        vm.qaOpen(screen, i.getStringExtra("qa_id"), i.getStringExtra("qa_lang"))
    }

    override fun onStart() {
        super.onStart()
        overlay.disarm()
        vm.overlayShowing = false
        // 기어는 내비 화면이 앞에 있을 때도 계속 읽어야 해서 onStop 에서 멈추지 않는다 (onDestroy 에서 해제)
        app.vehicle.start()
        app.restriction.start()
        if (!app.notifier.canPost() && android.os.Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        if (!app.gps.granted()) requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION), 2)
    }

    override fun onStop() {
        app.restriction.stop()
        // 내비가 앞으로 나오면 내비 지도 위에 길 위 이야기 카드·목적지 칩을 띄운다
        if (vm.trip.value?.naviGuided == true && vm.screen.value == Screen.Drive && overlay.canShow()) {
            overlay.arm()
            vm.overlayShowing = overlay.showing
        }
        super.onStop()
    }

    override fun onDestroy() {
        overlay.disarm()
        if (isFinishing) app.vehicle.stop()
        super.onDestroy()
    }
}
