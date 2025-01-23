package net.opendasharchive.openarchive.features.settings

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.Theme
import net.opendasharchive.openarchive.databinding.FragmentSettingsBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.internetarchive.presentation.InternetArchiveActivity
import net.opendasharchive.openarchive.features.settings.passcode.PasscodeRepository
import net.opendasharchive.openarchive.features.settings.passcode.passcode_setup.PasscodeSetupActivity
import net.opendasharchive.openarchive.features.spaces.SpacesActivity
import net.opendasharchive.openarchive.services.gdrive.GDriveActivity
import net.opendasharchive.openarchive.services.webdav.WebDavActivity
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.Theme
import net.opendasharchive.openarchive.util.extensions.Position
import net.opendasharchive.openarchive.util.extensions.getVersionName
import net.opendasharchive.openarchive.util.extensions.openBrowser
import net.opendasharchive.openarchive.util.extensions.scaled
import net.opendasharchive.openarchive.util.extensions.setDrawable
import net.opendasharchive.openarchive.util.extensions.styleAsLink
import org.koin.android.ext.android.inject
import kotlin.math.roundToInt

class SettingsFragment : PreferenceFragmentCompat() {

    private val passcodeRepository by inject<PasscodeRepository>()


    private var passcodePreference: SwitchPreferenceCompat? = null

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val passcodeEnabled = result.data?.getBooleanExtra("passcode_enabled", false) ?: false
            passcodePreference?.isChecked = passcodeEnabled
        } else {
            passcodePreference?.isChecked = false
        }
    }

//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return ComposeView(requireContext()).apply {
//            // Dispose of the Composition when the view's LifecycleOwner
//            // is destroyed
//            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
//            setContent {
//                Theme {
//                    SettingsScreen()
//                }
//            }
//        }
//    }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        setPreferencesFromResource(R.xml.prefs_general, rootKey)


        passcodePreference = findPreference(Prefs.PASSCODE_ENABLED)

        passcodePreference?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            if (enabled) {
                // Launch PasscodeSetupActivity
                val intent = Intent(context, PasscodeSetupActivity::class.java)
                activityResultLauncher.launch(intent)
            } else {
                // Show confirmation dialog
                AlertDialog.Builder(requireContext())
                    .setTitle("Disable Passcode")
                    .setMessage("Are you sure you want to disable the passcode?")
                    .setPositiveButton("Yes") { _, _ ->
                        passcodeRepository.clearPasscode()
                        passcodePreference?.isChecked = false

                        // Update the FLAG_SECURE dynamically
                        (activity as? BaseActivity)?.updateScreenshotPrevention()
                    }
                    .setNegativeButton("No") { _, _ ->
                        passcodePreference?.isChecked = true
                    }
                    .show()
            }
            // Return false to avoid the preference updating immediately
            false
        }

        findPreference<Preference>(Prefs.PROHIBIT_SCREENSHOTS)?.setOnPreferenceClickListener { _ ->
            if (activity is BaseActivity) {
                // make sure this gets settings change gets applied instantly
                // (all other activities rely on the hook in BaseActivity.onResume())
                (activity as BaseActivity).updateScreenshotPrevention()
            }

            true
        }

        getPrefByKey<Preference>(R.string.pref_media_servers)?.setOnPreferenceClickListener {
            startActivity(Intent(context, SpacesActivity::class.java))
            true
        }

        getPrefByKey<Preference>(R.string.pref_media_folders)?.setOnPreferenceClickListener {
            startActivity(Intent(context, FoldersActivity::class.java))
            true
        }

        findPreference<Preference>("proof_mode")?.setOnPreferenceClickListener {
            startActivity(Intent(context, ProofModeSettingsActivity::class.java))
            true
        }

        findPreference<Preference>(Prefs.USE_TOR)?.setOnPreferenceChangeListener { _, newValue ->
            //Prefs.useTor = (newValue as Boolean)
            //torViewModel.updateTorServiceState()
            false
        }

        findPreference<Preference>(Prefs.THEME)?.setOnPreferenceChangeListener { _, newValue ->
            Theme.set(Theme.get(newValue as? String))

            true
        }

        findPreference<Preference>(Prefs.UPLOAD_WIFI_ONLY)?.setOnPreferenceChangeListener { _, newValue ->
            val intent =
                Intent(Prefs.UPLOAD_WIFI_ONLY).apply { putExtra("value", newValue as Boolean) }
            // Replace with shared ViewModel + LiveData
            // LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
            true
        }

        val packageManager = requireActivity().packageManager
        val versionText = packageManager.getVersionName(requireActivity().packageName)

        findPreference<Preference>("app_version")?.summary = versionText
    }

    private fun <T: Preference> getPrefByKey(key: Int): T? {
        return findPreference(getString(key))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.setPadding(0, 16.dpToPx(), 0, 0)
    }

    fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()


//    mBinding.btAbout.text = getString(R.string.action_about, getString(R.string.app_name))
//    mBinding.btAbout.styleAsLink()
//    mBinding.btAbout.setOnClickListener {
//        context?.openBrowser("https://open-archive.org/save")
//    }
//
//    mBinding.btPrivacy.styleAsLink()
//    mBinding.btPrivacy.setOnClickListener {
//        context?.openBrowser("https://open-archive.org/privacy")
//    }
//
//    val activity = activity
//
//    if (activity != null) {
//        mBinding.version.text = getString(
//            R.string.version__,
//            activity.packageManager.getVersionName(activity.packageName)
//        )
//    }
}