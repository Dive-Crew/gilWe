package pleos.gilwe.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import pleos.gilwe.MainActivity
import pleos.gilwe.R

/**
 * 경로 안내 중 길 위를 살려 두는 포그라운드 서비스.
 * 내비 화면이 앞에 있으면 길 위는 백그라운드가 되고, 시스템이 프로세스를 얼려(freeze)
 * 내비 이벤트 수신과 이야기 진행이 멈춘다. 이야기 낭독(미디어 재생) 동안 포그라운드로 유지한다.
 */
class TripService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val place = intent?.getStringExtra(EXTRA_PLACE).orEmpty()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "길 위 경로 안내", NotificationManager.IMPORTANCE_LOW).apply {
                description = "안내 중 이야기 재생 상태"
                setShowBadge(false)
            },
        )
        val open = PendingIntent.getActivity(
            this, 1,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_gilwe)
            .setContentTitle("길 위 · $place")
            .setContentText("이야기와 함께 안내 중")
            .setOngoing(true)
            .setContentIntent(open)
            .build()
        runCatching {
            if (Build.VERSION.SDK_INT >= 29) startForeground(ID, n, types())
            else startForeground(ID, n)
        }.onFailure { Log.w(TAG, "startForeground failed", it); stopSelf() }
        return START_NOT_STICKY
    }

    /** 낭독(미디어) + 위치(내비가 앞에 있어도 GPS 로 다음 장소까지 시간 계산). 위치 권한이 없으면 미디어만 */
    private fun types(): Int {
        val loc = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK or (if (loc) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0)
    }

    companion object {
        private const val TAG = "TripService"
        private const val CHANNEL = "gilwe_trip"
        private const val ID = 41
        private const val EXTRA_PLACE = "place"

        fun start(ctx: Context, place: String) {
            runCatching {
                ctx.startForegroundService(Intent(ctx, TripService::class.java).putExtra(EXTRA_PLACE, place))
            }.onFailure { Log.w(TAG, "start failed", it) }
        }

        fun stop(ctx: Context) {
            runCatching { ctx.stopService(Intent(ctx, TripService::class.java)) }
        }
    }
}
