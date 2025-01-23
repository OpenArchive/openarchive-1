package net.opendasharchive.openarchive.features.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.permissionx.guolindev.PermissionX
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivitySettingsContainerBinding
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.util.Hbks
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.ProofModeHelper
import org.witness.proofmode.crypto.pgp.PgpUtils
import timber.log.Timber
import java.io.IOException
import java.util.UUID
import javax.crypto.SecretKey

class ProofModeSettingsActivity: BaseActivity() {

    class Fragment: PreferenceFragmentCompat() {

        private val enrollBiometrics = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            findPreference<SwitchPreferenceCompat>(Prefs.USE_PROOFMODE_KEY_ENCRYPTION)?.let {
                MainScope().launch {
                    enableProofModeKeyEncryption(it)
                }
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.prefs_proof_mode, rootKey)

            findPreference<Preference>("share_proofmode")?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    shareKey(requireActivity())
                    true
                }

            findPreference<Preference>(Prefs.USE_PROOFMODE)?.setOnPreferenceChangeListener { preference, newValue ->
                if (newValue as Boolean) {
                    PermissionX.init(this)
                        .permissions(Manifest.permission.READ_PHONE_STATE)
                        .onExplainRequestReason { _, _ ->
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", activity?.packageName, null)
                            intent.data = uri
                            activity?.startActivity(intent)
                        }
                        .request { allGranted, _, _ ->
                            if (!allGranted) {
                                (preference as? SwitchPreferenceCompat)?.isChecked = false
                                Toast.makeText(activity,"Please allow all permissions", Toast.LENGTH_LONG).show()
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                val uri = Uri.fromParts("package", activity?.packageName, null)
                                intent.data = uri
                                activity?.startActivity(intent)
                            }
                        }
                }

                true
            }

            val pkePreference = findPreference<SwitchPreferenceCompat>(Prefs.USE_PROOFMODE_KEY_ENCRYPTION)
            val activity = activity
            val availability = Hbks.deviceAvailablity(requireContext())

            if (activity != null && availability !is Hbks.Availability.Unavailable) {
                pkePreference?.isSingleLineTitle = false

                pkePreference?.setTitle(when (Hbks.biometryType(activity)) {
                    Hbks.BiometryType.StrongBiometry -> R.string.prefs_proofmode_key_encryption_title_biometrics

                    Hbks.BiometryType.DeviceCredential -> R.string.prefs_proofmode_key_encryption_title_passcode

                    else -> R.string.prefs_proofmode_key_encryption_title_all
                })

                pkePreference?.setOnPreferenceChangeListener { _, newValue ->
                    if (newValue as Boolean) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && availability is Hbks.Availability.Enroll) {
                            enrollBiometrics.launch(Hbks.enrollIntent(availability.type))
                        } else {
                            enableProofModeKeyEncryption(pkePreference)
                        }
                    }
                    else {
                        if (Prefs.proofModeEncryptedPassphrase != null) {
                            Prefs.proofModeEncryptedPassphrase = null

                            Hbks.removeKey()

                            ProofModeHelper.restartApp(activity)
                        }
                    }

                    true
                }
            }
            else {
                pkePreference?.isVisible = false
            }
        }

        private fun enableProofModeKeyEncryption(pkePreference: SwitchPreferenceCompat) {

            val key = Hbks.loadKey() ?: Hbks.createKey()

            if (key != null && Prefs.proofModeEncryptedPassphrase == null) {
                createPassphrase(key, activity) {
                    if (it != null) {
                        ProofModeHelper.removePgpKey(requireContext())

                        // We need to kill the app and restart,
                        // since the ProofMode singleton loads the passphrase
                        // in its singleton constructor. Urgh.
                        ProofModeHelper.restartApp(requireActivity())
                    } else {
                        Hbks.removeKey()

                        pkePreference.isChecked = false
                    }
                }
            } else {
                // What??  shouldn't happen if enrolled with a PIN or Fingerprint
            }
        }
    }

    private lateinit var mBinding: ActivitySettingsContainerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivitySettingsContainerBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setupToolbar(getString(R.string.proofmode))


        supportFragmentManager
            .beginTransaction()
            .replace(mBinding.container.id, Fragment())
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {

        private fun shareKey(activity: Activity) {
            try {
                val mPgpUtils = PgpUtils.getInstance(activity, null)
                val pubKey = mPgpUtils.publicKeyString

                if (pubKey.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_TEXT, pubKey)
                    activity.startActivity(intent)
                }
            }
            catch (ioe: IOException) {
                Timber.d("error publishing key")
            }
        }

        private fun createPassphrase(key: SecretKey, activity: FragmentActivity?, completed: (passphrase: String?) -> Unit) {
            val passphrase = UUID.randomUUID().toString()

            Hbks.encrypt(passphrase, key, activity) { ciphertext, _ ->
                if (ciphertext == null) {
                    return@encrypt completed(null)
                }

                Prefs.proofModeEncryptedPassphrase = ciphertext

                Hbks.decrypt(Prefs.proofModeEncryptedPassphrase, key, activity) { decrpytedPassphrase, _ ->
                    if (decrpytedPassphrase == null || decrpytedPassphrase != passphrase) {
                        Prefs.proofModeEncryptedPassphrase = null

                        return@decrypt completed(null)
                    }

                    completed(passphrase)
                }
            }
        }
    }
}