package net.opendasharchive.openarchive

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import coil.Coil
import coil.ImageLoader
import coil.util.Logger
import com.orm.SugarApp
import info.guardianproject.netcipher.proxy.OrbotHelper
import net.opendasharchive.openarchive.core.di.coreModule
import net.opendasharchive.openarchive.core.di.featuresModule
import net.opendasharchive.openarchive.core.di.retrofitModule
import net.opendasharchive.openarchive.core.di.unixSocketModule
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.features.settings.passcode.PasscodeManager
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.Theme
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class SaveApp : SugarApp() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(applicationContext, initDebugger = true)
        registerActivityLifecycleCallbacks(PasscodeManager())
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@SaveApp)
            modules(
                coreModule,
                featuresModule,
                retrofitModule,
                unixSocketModule
            )
        }

        val imageLoader = ImageLoader.Builder(this)
            .logger(object : Logger {
                override var level = Log.VERBOSE

                override fun log(
                    tag: String,
                    priority: Int,
                    message: String?,
                    throwable: Throwable?
                ) {
                    Timber.tag("Coil").log(priority, throwable, message)
                }
            })
            .build()

        Coil.setImageLoader(imageLoader)
        Prefs.load(this)

        if (Prefs.useTor) initNetCipher()

        Theme.set(Prefs.theme)

        CleanInsightsManager.init(this)

        createSnowbirdNotificationChannel()
    }

    private fun initNetCipher() {
        AppLogger.d("Initializing NetCipher client")
        val oh = OrbotHelper.get(this)

        if (BuildConfig.DEBUG) {
            oh.skipOrbotValidation()
        }

//        oh.init()
    }

    private fun createSnowbirdNotificationChannel() {
        val silentChannel = NotificationChannel(
            SNOWBIRD_SERVICE_CHANNEL_SILENT,
            "Raven Service",
            NotificationManager.IMPORTANCE_LOW
        )

        val chimeChannel = NotificationChannel(
            SNOWBIRD_SERVICE_CHANNEL_CHIME,
            "Raven Service",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(chimeChannel)
        notificationManager.createNotificationChannel(silentChannel)
    }

    companion object {
        const val SNOWBIRD_SERVICE_ID = 2601
        const val SNOWBIRD_SERVICE_CHANNEL_CHIME = "snowbird_service_channel_chime"
        const val SNOWBIRD_SERVICE_CHANNEL_SILENT = "snowbird_service_channel_silent"

        const val TOR_SERVICE_ID = 2602
        const val TOR_SERVICE_CHANNEL = "tor_service_channel"
    }
}