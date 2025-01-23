package net.opendasharchive.openarchive.features.internetarchive.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import net.opendasharchive.openarchive.core.presentation.theme.Theme
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.internetarchive.presentation.components.IAResult
import net.opendasharchive.openarchive.features.internetarchive.presentation.components.getSpace
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.ComposeAppBar
import net.opendasharchive.openarchive.features.main.MainActivity

@Deprecated("use jetpack compose")
class InternetArchiveActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val (space, isNewSpace) = intent.extras.getSpace(Space.Type.INTERNET_ARCHIVE)

        setContent {

            Theme {
                Scaffold(
                    topBar = {
                        ComposeAppBar(
                            title = if (isNewSpace) "Add Internet Archive" else "Edit Internet Archive",
                            onNavigationAction = { finish(IAResult.Cancelled) }
                        )
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)) {
                        InternetArchiveScreen(space, isNewSpace) {
                            finish(it)
                        }
                    }
                }
            }


        }
    }

    private fun finish(result: IAResult) {
        when (result) {
            IAResult.Saved -> {
                startActivity(Intent(this, MainActivity::class.java))
                // measureNewBackend(Space.Type.INTERNET_ARCHIVE)
            }

            IAResult.Deleted -> Space.navigate(this)
            IAResult.Cancelled -> onBackPressed()
        }
    }
}

//fun Activity.measureNewBackend(type: Space.Type) {
//    CleanInsightsManager.getConsent(this) {
//        CleanInsightsManager.measureEvent(
//            "backend",
//            "new",
//            type.friendlyName
//        )
//    }
//}
