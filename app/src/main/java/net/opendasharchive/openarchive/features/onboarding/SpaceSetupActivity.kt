package net.opendasharchive.openarchive.features.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivitySpaceSetupBinding
import net.opendasharchive.openarchive.db.SnowbirdError
import net.opendasharchive.openarchive.extensions.androidViewModel
import net.opendasharchive.openarchive.extensions.onBackButtonPressed
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.internetarchive.presentation.InternetArchiveFragment
import net.opendasharchive.openarchive.features.main.MainActivity
import net.opendasharchive.openarchive.features.settings.SpaceSetupFragment
import net.opendasharchive.openarchive.features.settings.SpaceSetupSuccessFragment
import net.opendasharchive.openarchive.services.gdrive.GDriveFragment
import net.opendasharchive.openarchive.services.snowbird.SnowbirdCreateGroupFragment
import net.opendasharchive.openarchive.services.snowbird.SnowbirdFileListFragment
import net.opendasharchive.openarchive.services.snowbird.SnowbirdFragment
import net.opendasharchive.openarchive.services.snowbird.SnowbirdGroupListFragment
import net.opendasharchive.openarchive.services.snowbird.SnowbirdGroupViewModel
import net.opendasharchive.openarchive.services.snowbird.SnowbirdJoinGroupFragment
import net.opendasharchive.openarchive.services.snowbird.SnowbirdRepoListFragment
import net.opendasharchive.openarchive.services.snowbird.SnowbirdRepoViewModel
import net.opendasharchive.openarchive.services.snowbird.SnowbirdShareFragment
import net.opendasharchive.openarchive.services.webdav.WebDavFragment
import net.opendasharchive.openarchive.services.webdav.WebDavSetupLicenseFragment
import net.opendasharchive.openarchive.util.FullScreenOverlayManager
import net.opendasharchive.openarchive.util.Utility
import kotlin.getValue

interface ToolbarConfigurable {
    fun getToolbarTitle(): String
    fun getToolbarSubtitle(): String? = null
    fun shouldShowBackButton(): Boolean = true
}

abstract class BaseFragment : Fragment(), ToolbarConfigurable {

    val snowbirdGroupViewModel: SnowbirdGroupViewModel by androidViewModel()
    val snowbirdRepoViewModel: SnowbirdRepoViewModel by androidViewModel()

    open fun dismissKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    open fun handleError(error: SnowbirdError) {
        Utility.showMaterialWarning(
            requireContext(),
            error.friendlyMessage
        )
    }

    open fun handleLoadingStatus(isLoading: Boolean) {
        if (isLoading) {
            FullScreenOverlayManager.show(this@BaseFragment)
        } else {
            FullScreenOverlayManager.hide()
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? SpaceSetupActivity)?.updateToolbarFromFragment(this)
    }
}

class SpaceSetupActivity : BaseActivity() {

    companion object {
        const val FRAGMENT_TAG = "ssa_fragment"
    }

    private lateinit var mBinding: ActivitySpaceSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivitySpaceSetupBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setupToolbar(
            title = "Servers",
            showBackButton = true
        )

        initSpaceSetupFragmentBindings()
        initWebDavFragmentBindings()
        initWebDavCreativeLicenseBindings()
        initSpaceSetupSuccessFragmentBindings()
        initInternetArchiveFragmentBindings()
        initGDriveFragmentBindings()
        initRavenBindings()


        onBackButtonPressed {
            // Return "true" if you fully handle the back press yourself
            // Return "false" if you want to let the system handle it (i.e., finish the Activity)

            if (supportFragmentManager.backStackEntryCount > 1) {
                // We still have fragments in the back stack to pop
                supportFragmentManager.popBackStack()
                true // fully handled here
            } else {
                // No more fragments left in back stack, let the system finish Activity
                false
            }
        }

        intent.getBooleanExtra("snowbird", false).let {
            if (it) {
                navigateToFragment(SnowbirdFragment.newInstance())
            }
        }
    }

    fun updateToolbarFromFragment(fragment: Fragment) {
        if (fragment is ToolbarConfigurable) {
            val title = fragment.getToolbarTitle()
            val subtitle = fragment.getToolbarSubtitle()
            val showBackButton = fragment.shouldShowBackButton()
            setupToolbar(title = title, showBackButton = showBackButton)
            supportActionBar?.subtitle = subtitle
        } else {
            // Default toolbar configuration if fragment doesn't implement interface
            setupToolbar(title = "Servers", showBackButton = true)
            supportActionBar?.subtitle = null
        }
    }

    private fun initSpaceSetupSuccessFragmentBindings() {
        supportFragmentManager.setFragmentResultListener(
            SpaceSetupSuccessFragment.RESP_DONE,
            this
        ) { key, bundle ->
            finishAffinity()
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun initSpaceSetupFragmentBindings() {
        supportFragmentManager.setFragmentResultListener(
            SpaceSetupFragment.RESULT_REQUEST_KEY,
            this
        ) { _, bundle ->
            when (bundle.getString(SpaceSetupFragment.RESULT_BUNDLE_KEY)) {
                SpaceSetupFragment.RESULT_VAL_INTERNET_ARCHIVE -> {
                    navigateToFragment(InternetArchiveFragment.newInstance())
                }

                SpaceSetupFragment.RESULT_VAL_WEBDAV -> {
                    navigateToFragment(WebDavFragment.newInstance())
                }

                SpaceSetupFragment.RESULT_VAL_GDRIVE -> {
                    navigateToFragment(GDriveFragment())
                }

                SpaceSetupFragment.RESULT_VAL_RAVEN -> {
                    navigateToFragment(SnowbirdFragment.newInstance())
                }
            }
        }
    }

    /**
     * Init NextCloud credentials
     *
     */
    private fun initWebDavFragmentBindings() {
        supportFragmentManager.setFragmentResultListener(
            WebDavFragment.RESP_SAVED,
            this
        ) { key, bundle ->
            val spaceId = bundle.getLong(WebDavFragment.ARG_SPACE_ID)
            val fragment =
                WebDavSetupLicenseFragment.newInstance(spaceId = spaceId, isEditing = false)
            navigateToFragment(fragment)
        }


        supportFragmentManager.setFragmentResultListener(
            WebDavFragment.RESP_CANCEL,
            this
        ) { key, bundle ->
            navigateToFragment(SpaceSetupFragment())
        }
    }

    /**
     * Init select Creative Commons Licensing
     *
     */
    private fun initWebDavCreativeLicenseBindings() {
        supportFragmentManager.setFragmentResultListener(
            WebDavSetupLicenseFragment.RESP_SAVED,
            this
        ) { key, bundle ->
            val message = getString(R.string.you_have_successfully_connected_to_a_private_server)
            val fragment = SpaceSetupSuccessFragment.newInstance(message)
            navigateToFragment(fragment)
        }

        supportFragmentManager.setFragmentResultListener(
            WebDavSetupLicenseFragment.RESP_CANCEL,
            this
        ) { key, bundle ->
            navigateToFragment(SpaceSetupFragment())
        }
    }

    private fun initInternetArchiveFragmentBindings() {
        supportFragmentManager.setFragmentResultListener(
            InternetArchiveFragment.RESP_SAVED,
            this
        ) { key, bundle ->
            val fragment =
                SpaceSetupSuccessFragment.newInstance(getString(R.string.you_have_successfully_connected_to_the_internet_archive))
            navigateToFragment(fragment)
        }

        supportFragmentManager.setFragmentResultListener(
            InternetArchiveFragment.RESP_CANCEL,
            this
        ) { key, bundle ->
            navigateToFragment(SpaceSetupFragment())
        }
    }

    private fun initGDriveFragmentBindings() {
        supportFragmentManager.setFragmentResultListener(
            GDriveFragment.RESP_CANCEL,
            this
        ) { key, bundle ->

            navigateToFragment(SpaceSetupFragment())
        }

        supportFragmentManager.setFragmentResultListener(
            GDriveFragment.RESP_AUTHENTICATED,
            this
        ) { key, bundle ->
            val fragment =
                SpaceSetupSuccessFragment.newInstance(getString(R.string.you_have_successfully_connected_to_gdrive))
            navigateToFragment(fragment)
        }
    }

    private fun initRavenBindings() {

        initSnowbirdFragmentBindings()

        initSnowbirdGroupListFragmentBindings()

        initSnowbirdCreateGroupFragmentBindings()

        initSnowbirdRepoListFragmentBindings()

    }

    private fun initSnowbirdFragmentBindings() {
        supportFragmentManager.setFragmentResultListener(
            SnowbirdFragment.RESULT_REQUEST_KEY,
            this
        ) { key, bundle ->
            when (bundle.getString(SnowbirdFragment.RESULT_BUNDLE_KEY)) {

                SnowbirdFragment.RESULT_VAL_RAVEN_MY_GROUPS -> {
                    navigateToFragment(SnowbirdGroupListFragment.newInstance())
                }

                SnowbirdFragment.RESULT_VAL_RAVEN_CREATE_GROUP -> {
                    val fragment = SnowbirdCreateGroupFragment.newInstance()
                    navigateToFragment(fragment)
                }

                SnowbirdFragment.RESULT_VAL_RAVEN_JOIN_GROUPS -> {
                    val uriString = bundle.getString(SnowbirdFragment.RESULT_VAL_RAVEN_JOIN_GROUPS_ARG) ?: ""
                    navigateToFragment(SnowbirdJoinGroupFragment.newInstance(uriString))
                }
            }
        }
    }

    private fun initSnowbirdGroupListFragmentBindings() {
        supportFragmentManager.setFragmentResultListener(
            SnowbirdGroupListFragment.RESULT_REQUEST_KEY,
            this
        ) { key, bundle ->

            when (bundle.getString(SnowbirdGroupListFragment.RESULT_BUNDLE_NAVIGATION_KEY)) {
                SnowbirdGroupListFragment.RESULT_VAL_RAVEN_CREATE_GROUP_SCREEN -> {
                    val fragment = SnowbirdCreateGroupFragment.newInstance()
                    navigateToFragment(fragment)
                }
                SnowbirdGroupListFragment.RESULT_VAL_RAVEN_REPO_LIST_SCREEN -> {
                    val groupKey = bundle.getString(SnowbirdGroupListFragment.RESULT_BUNDLE_GROUP_KEY) ?: ""
                    val fragment = SnowbirdRepoListFragment.newInstance(groupKey)
                    navigateToFragment(fragment)
                }
                SnowbirdGroupListFragment.RESULT_VAL_RAVEN_SHARE_SCREEN -> {
                    val groupKey = bundle.getString(SnowbirdGroupListFragment.RESULT_BUNDLE_GROUP_KEY) ?: ""
                    val fragment = SnowbirdShareFragment.newInstance(groupKey)
                    navigateToFragment(fragment)
                }
            }
        }
    }

    private fun initSnowbirdCreateGroupFragmentBindings() {
        supportFragmentManager.setFragmentResultListener(
            SnowbirdCreateGroupFragment.RESULT_REQUEST_KEY,
            this
        ) { key, bundle ->
            when(bundle.getString(SnowbirdCreateGroupFragment.RESULT_NAVIGATION_KEY)) {
                SnowbirdCreateGroupFragment.RESULT_NAVIGATION_VAL_SHARE_SCREEN -> {
                    val groupKey =
                        bundle.getString(SnowbirdCreateGroupFragment.RESULT_BUNDLE_GROUP_KEY) ?: ""
                    val fragment = SnowbirdShareFragment.newInstance(groupKey)
                    navigateToFragment(fragment)
                }
            }
        }
    }

    private fun initSnowbirdRepoListFragmentBindings() {
        supportFragmentManager.setFragmentResultListener(
            SnowbirdRepoListFragment.RESULT_REQUEST_KEY,
            this
        ) { key, bundle ->
            val groupKey = bundle.getString(SnowbirdRepoListFragment.RESULT_VAL_RAVEN_GROUP_KEY) ?: ""
            val repoKey = bundle.getString(SnowbirdRepoListFragment.RESULT_VAL_RAVEN_REPO_KEY) ?: ""
            val fragment = SnowbirdFileListFragment.newInstance(
                groupKey = groupKey,
                repoKey = repoKey
            )
            navigateToFragment(fragment)
        }
    }


//    @Deprecated("Deprecated in Java")
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        supportFragmentManager.findFragmentByTag(FRAGMENT_TAG)?.let {
//            onActivityResult(requestCode, resultCode, data)
//        }
//    }

    private fun navigateToFragment(
        fragment: BaseFragment,
        addToBackstack: Boolean = true
    ) {
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(mBinding.spaceSetupFragment.id, fragment, FRAGMENT_TAG)
            .apply {
                if (addToBackstack) addToBackStack(null)
            }.commit()
    }
}
