package net.opendasharchive.openarchive

import android.content.Context
import android.content.Intent
import android.os.Build
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.decoder.SimpleProgressiveJpegConfig
import com.orm.SugarApp
import info.guardianproject.netcipher.proxy.OrbotHelper
import net.opendasharchive.openarchive.upload.UploadService
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.Theme
import timber.log.Timber

class SaveApp : SugarApp() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

    }

    override fun onCreate() {
        super.onCreate()

        val config = ImagePipelineConfig.newBuilder(this)
            .setProgressiveJpegConfig(SimpleProgressiveJpegConfig())
            .setResizeAndRotateEnabledForNetwork(true)
            .setDownsampleEnabled(true)
            .build()

        Fresco.initialize(this, config)
        Prefs.load(this)

        if (Prefs.useTor) initNetCipher()

        Theme.set(Prefs.theme)

        CleanInsightsManager.init(this)

        // enable timber logging library for debug builds
        if(BuildConfig.DEBUG){
            Timber.plant(Timber.DebugTree())
        }
    }

    /**
     * This needs to be called from the foreground (from an activity in the foreground),
     * otherwise, `#startForegroundService` will crash!
     * See
     * https://developer.android.com/guide/components/foreground-services#background-start-restrictions
     */
    fun startUploadService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, UploadService::class.java))
        } else {
            startService(Intent(this, UploadService::class.java))
        }
    }

    fun stopUploadService() {
        stopService(Intent(this, UploadService::class.java))
    }

    private fun initNetCipher() {
        Timber.d( "Initializing NetCipher client")
        val oh = OrbotHelper.get(this)

        if (BuildConfig.DEBUG) {
            oh.skipOrbotValidation()
        }

        oh.init()
    }
}