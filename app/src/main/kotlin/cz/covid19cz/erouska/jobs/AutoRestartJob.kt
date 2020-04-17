package cz.covid19cz.erouska.jobs

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import cz.covid19cz.erouska.BuildConfig
import cz.covid19cz.erouska.service.CovidService
import cz.covid19cz.erouska.utils.L
import org.joda.time.DateTime

class AutoRestartJob : BroadcastReceiver() {

    companion object {
        const val EXTRAKEY_START = "AUTO_RESTART_START"
        const val EXTRAKEY_CANCEL = "AUTO_RESTART_CANCEL"

        private const val ACTION = "EROUSKA_ALARM"

        private val SCHEME = "erouska" + if (BuildConfig.FLAVOR == "dev") "-dev" else ""
        private val URI = Uri.parse("$SCHEME://auto-restart")

        const val INTERVAL = 24 * 60 * 60 * 1000L // 24 hours
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION && intent.data == URI) {
            context?.let { ctx ->
                // do this stuff only when service is running!
                //
                // if it's not running, it should actually not eve get here, however, it's better to
                // check it to avoid possible problems like:
                // `Unable to start receiver  java.lang.IllegalStateException: Not allowed to start service`
                if (CovidService.isRunning(ctx)) {
                    L.d("Auto-restart of service - stopping")
                    ctx.startService(
                        CovidService.stopService(ctx).putExtra(EXTRAKEY_CANCEL, false)
                    )

                    Handler().postDelayed({
                        L.d("Auto-restart of service - starting")
                        ctx.startService(
                            CovidService.startService(ctx).putExtra(EXTRAKEY_START, false)
                        )
                    }, 2000);
                }
            }
        }
    }

    private var pendingIntent: PendingIntent? = null

    fun setUp(context: Context, alarmManager: AlarmManager) {
        val intent = Intent(context, AutoRestartJob::class.java)
            .setAction(ACTION)
            .setData(URI)

        pendingIntent = PendingIntent.getBroadcast(
            context,
            123,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val first = DateTime.now().plusDays(1).withHourOfDay(2).withMinuteOfHour(0)
        L.d("Planning auto-restart with interval $INTERVAL millis, first: $first")

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            first.millis,
            INTERVAL,
            pendingIntent
        )
    }

    fun cancel(alarmManager: AlarmManager) {
        L.d("Cancelling auto-restart")
        pendingIntent?.let { alarmManager.cancel(it) }
    }
}