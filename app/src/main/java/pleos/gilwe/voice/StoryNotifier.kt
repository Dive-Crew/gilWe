package pleos.gilwe.voice

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import pleos.gilwe.MainActivity
import pleos.gilwe.R

/**
 * 내비 화면이 앞에 있을 때 "지금 어떤 이야기를 듣는지"를 알려 주는 알림.
 * 이야기 낭독이 시작될 때마다 헤드업 알림으로 띄우고, 누르면 길 위 주행 화면으로 돌아온다.
 */
class StoryNotifier(context: Context) {

    private val ctx = context.applicationContext
    private val nm = ctx.getSystemService(NotificationManager::class.java)

    init {
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "길 위 이야기", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "주행 중 재생되는 이야기 안내"
                setSound(null, null) // 소리는 낭독(TTS)이 담당
                enableVibration(false)
            },
        )
    }

    fun canPost(): Boolean =
        Build.VERSION.SDK_INT < 33 || ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun showStory(label: String, title: String, place: String) {
        if (!canPost()) return
        val open = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_gilwe)
            .setContentTitle("길 위 · $label")
            .setContentText("$title — $place")
            .setCategory(Notification.CATEGORY_NAVIGATION)
            .setContentIntent(open)
            .setOnlyAlertOnce(false)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(ID, n) }.onFailure { Log.w("StoryNotifier", "notify failed", it) }
    }

    fun clear() = nm.cancel(ID)

    private companion object {
        const val CHANNEL = "gilwe_story"
        const val ID = 42
    }
}
