package pleos.gilwe.ui

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * 내비 화면 위에 그리는 길 위 오버레이 (Figma 02 주행과 듣기).
 * 내비가 앞에 있을 때 길 위 목적지 칩(오른쪽 위)과 이야기 카드(아래)를 내비 지도 위에 띄운다.
 * 회전 안내·남은 시간은 내비가 이미 그리므로 겹치지 않게 내비 UI가 비어 있는 자리에만 둔다.
 * SYSTEM_ALERT_WINDOW 가 없으면 아무것도 하지 않고, 헤드업 알림(StoryNotifier)만 남는다.
 */
class NaviOverlay(context: Context, private val vm: AppViewModel) {

    private val ctx = context.applicationContext
    private val wm = ctx.getSystemService(WindowManager::class.java)
    private val views = mutableListOf<View>()
    private var owner: OverlayOwner? = null

    val showing get() = views.isNotEmpty()

    fun canShow(): Boolean = Settings.canDrawOverlays(ctx)

    fun show() {
        if (showing || !canShow()) return
        val o = OverlayOwner().also { it.start() }
        owner = o
        val d = ctx.resources.displayMetrics.density
        fun px(dp: Int) = (dp * d).toInt()
        // 내비 앱 창: 오른쪽 위 2D 버튼, 오른쪽 아래 소리·설정 버튼, 왼쪽 아래 도착 시간 패널을 피해서 배치
        add(o, Gravity.TOP or Gravity.END, x = px(176), y = px(110)) {
            val lang by vm.screenLang.collectAsState()
            val trip by vm.trip.collectAsState()
            // 칩을 누르면 길 위 주행 화면으로 (오버레이는 onStart 에서 내려간다)
            trip?.let {
                DestinationChip(
                    lang, it,
                    Modifier.clip(CardShape).clickable {
                        ctx.startActivity(
                            android.content.Intent(ctx, pleos.gilwe.MainActivity::class.java)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                        )
                    },
                )
            }
        }
        add(o, Gravity.BOTTOM or Gravity.END, x = px(150), y = px(160)) {
            val lang by vm.screenLang.collectAsState()
            val audio by vm.audioLang.collectAsState()
            val trip by vm.trip.collectAsState()
            trip?.let { StoryCard(lang, it, audio, vm::toggleStoryPause, vm::skipToNextStory, Modifier.width(900.dp), upNextWidth = 300.dp, onRetryLocation = vm::retryLocation) }
        }
    }

    fun hide() {
        views.forEach { v -> runCatching { wm.removeView(v) } }
        views.clear()
        owner?.stop()
        owner = null
    }

    // ---------- 내비가 앞에 있을 때만 보이기 ----------

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val usage = ctx.getSystemService(android.app.usage.UsageStatsManager::class.java)
    var armed = false
        private set
    private var lastQuery = 0L
    private var foreground: String? = null

    /** 기본 내비 앱 패키지 (지도 앱 카테고리로 찾고, 못 찾으면 Pleos Connect 기본 내비) */
    private val naviPackage: String by lazy {
        val i = android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_APP_MAPS)
        runCatching { ctx.packageManager.queryIntentActivities(i, 0).firstOrNull()?.activityInfo?.packageName }.getOrNull()
            ?: "ai.umos.maps.android.navigation.app"
    }

    /** 사용 기록 접근이 허용돼 있어야 앞에 있는 앱을 알 수 있다. 없으면 길 위가 뒤로 가 있는 동안 항상 보인다 */
    private fun canReadForeground(): Boolean {
        val ops = ctx.getSystemService(android.app.AppOpsManager::class.java)
        val mode = runCatching {
            ops.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), ctx.packageName)
        }.getOrDefault(android.app.AppOpsManager.MODE_ERRORED)
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    /** 길 위가 뒤로 갔을 때 호출: 앞에 있는 앱이 내비일 때만 오버레이를 띄우고, 다른 앱이면 내린다 */
    fun arm() {
        if (armed || !canShow()) return
        armed = true
        if (!canReadForeground()) { show(); return }
        lastQuery = System.currentTimeMillis() - 10_000
        foreground = null
        handler.post(poll)
    }

    /** 길 위가 다시 앞으로 오면 호출 */
    fun disarm() {
        armed = false
        handler.removeCallbacks(poll)
        hide()
    }

    private val poll = object : Runnable {
        override fun run() {
            if (!armed) return
            val now = System.currentTimeMillis()
            runCatching {
                val events = usage.queryEvents(lastQuery, now)
                val e = android.app.usage.UsageEvents.Event()
                while (events.hasNextEvent()) {
                    events.getNextEvent(e)
                    if (e.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) foreground = e.packageName
                }
            }.onFailure { Log.w("NaviOverlay", "usage query failed", it) }
            lastQuery = now
            val naviInFront = foreground == naviPackage
            if (naviInFront && !showing) show() else if (!naviInFront && showing) hide()
            vm.overlayShowing = showing
            handler.postDelayed(this, 700)
        }
    }

    private fun add(o: OverlayOwner, gravity: Int, x: Int, y: Int, content: @Composable () -> Unit) {
        val v = ComposeView(ctx).apply {
            setViewTreeLifecycleOwner(o)
            setViewTreeSavedStateRegistryOwner(o)
            setViewTreeViewModelStoreOwner(o)
            setContent { GilweTheme { content() } }
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            this.gravity = gravity
            this.x = x
            this.y = y
        }
        runCatching { wm.addView(v, lp); views += v }.onFailure { Log.w("NaviOverlay", "addView failed", it) }
    }

    /** 액티비티가 멈춰 있어도 오버레이의 Compose 가 계속 그려지도록 별도 수명 주기를 둔다 */
    private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry(this)
        private val saved = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry
        override val viewModelStore = ViewModelStore()

        fun start() {
            saved.performRestore(null)
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun stop() {
            registry.currentState = Lifecycle.State.DESTROYED
            viewModelStore.clear()
        }
    }
}
