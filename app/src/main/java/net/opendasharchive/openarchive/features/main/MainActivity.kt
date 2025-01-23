package net.opendasharchive.openarchive.features.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.logger.AppLogger
import net.opendasharchive.openarchive.databinding.ActivityMainBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.folders.AddFolderActivity
import net.opendasharchive.openarchive.features.media.AddMediaDialogFragment
import net.opendasharchive.openarchive.features.media.AddMediaType
import net.opendasharchive.openarchive.features.media.ContentPickerFragment
import net.opendasharchive.openarchive.features.media.MediaLaunchers
import net.opendasharchive.openarchive.features.media.Picker
import net.opendasharchive.openarchive.features.media.PreviewActivity
import net.opendasharchive.openarchive.features.onboarding.Onboarding23Activity
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity
import net.opendasharchive.openarchive.features.settings.FoldersActivity
import net.opendasharchive.openarchive.features.settings.passcode.AppConfig
import net.opendasharchive.openarchive.features.spaces.SpacesActivity
import net.opendasharchive.openarchive.services.snowbird.SnowbirdBridge
import net.opendasharchive.openarchive.services.snowbird.service.SnowbirdService
import net.opendasharchive.openarchive.upload.UploadService
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.ProofModeHelper
import net.opendasharchive.openarchive.util.Utility
import net.opendasharchive.openarchive.util.extensions.cloak
import net.opendasharchive.openarchive.util.extensions.hide
import net.opendasharchive.openarchive.util.extensions.show
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.text.NumberFormat


class MainActivity : BaseActivity() {

    private val appConfig by inject<AppConfig>()

    private var mMenuDelete: MenuItem? = null

    private var mSnackBar: Snackbar? = null

    private lateinit var binding: ActivityMainBinding
    private lateinit var mPagerAdapter: ProjectAdapter

    private lateinit var mediaLaunchers: MediaLaunchers

    private var mLastItem: Int = 0
    private var mLastMediaItem: Int = 0

    private var mCurrentPagerItem
        get() = binding.pager.currentItem
        set(value) {
            binding.pager.currentItem = value
            updateBottomNavbar(value)
        }

    private val mNewFolderResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                refreshProjects(it.data?.getLongExtra(AddFolderActivity.EXTRA_FOLDER_ID, -1))
            }
        }

    private val folderResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedFolderId = result.data?.getLongExtra("SELECTED_FOLDER_ID", -1)
                if (selectedFolderId != null && selectedFolderId > -1) {
                    navigateToFolder(selectedFolderId)
                }
            }
        }


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Timber.d("Able to post notifications")
        } else {
            Timber.d("Need to explain")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mediaLaunchers = Picker.register(
            activity = this,
            root = binding.root,
            project = { getSelectedProject() },
            completed = { media ->
                refreshCurrentProject()

                if (media.isNotEmpty()) {
                    preview()
                }
            })

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.title = null

        mPagerAdapter = ProjectAdapter(supportFragmentManager, lifecycle)
        binding.pager.adapter = mPagerAdapter

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int, positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                // Do Nothing
            }

            override fun onPageSelected(position: Int) {
                mLastItem = position
                if (position < mPagerAdapter.settingsIndex) {
                    mLastMediaItem = position
                }

                updateBottomNavbar(position)

                refreshCurrentProject()
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

        setupBottomNavBar()


        binding.breadcrumbSpace.setOnClickListener {
            startActivity(Intent(this, SpacesActivity::class.java))
        }

        binding.breadcrumbFolder.setOnClickListener {
            val selectedSpaceId = getSelectedSpace()?.id
            val selectedProjectId = getSelectedProject()?.id
            val intent = Intent(this, FoldersActivity::class.java)
            intent.putExtra(
                FoldersActivity.EXTRA_SELECTED_SPACE_ID,
                selectedSpaceId
            ) // Pass the selected space ID
            intent.putExtra(
                FoldersActivity.EXTRA_SELECTED_PROJECT_ID,
                selectedProjectId
            ) // Pass the selected project ID
            folderResultLauncher.launch(intent)
        }


        if (appConfig.snowbirdEnabled) {
            
            checkNotificationPermissions()

            SnowbirdBridge.getInstance().initialize()
            val intent = Intent(this, SnowbirdService::class.java)
            startForegroundService(intent)

            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri?.scheme == "save-veilid") {
                processUri(uri)
            }
        }
    }

    private fun processUri(uri: Uri) {
        val path = uri.path
        val queryParams = uri.queryParameterNames.associateWith { uri.getQueryParameter(it) }
        AppLogger.d("Path: $path, QueryParams: $queryParams")
    }

    private fun setupBottomNavBar() {
        binding.bottomNavBar.onMyMediaClick = {
            mCurrentPagerItem = mLastMediaItem
        }

        binding.bottomNavBar.onAddClick = {

            if (Prefs.addMediaHint) {
                addClicked(AddMediaType.GALLERY)
            } else {
                AlertHelper.show(
                    context = this,
                    message = R.string.press_and_hold_options_media_screen_message,
                    title = R.string.press_and_hold_options_media_screen_title,
                )
                Prefs.addMediaHint = true
            }
        }

        binding.bottomNavBar.onSettingsClick = {
            mCurrentPagerItem = mPagerAdapter.settingsIndex
        }

        if (Picker.canPickFiles(this)) {
            binding.bottomNavBar.setAddButtonLongClickEnabled()

            binding.bottomNavBar.onAddLongClick = {
                //val addMediaDialogFragment = AddMediaDialogFragment()
                //addMediaDialogFragment.show(supportFragmentManager, addMediaDialogFragment.tag)

                val addMediaBottomSheet =
                    ContentPickerFragment { actionType -> addClicked(actionType) }
                addMediaBottomSheet.show(supportFragmentManager, ContentPickerFragment.TAG)
            }

            supportFragmentManager.setFragmentResultListener(
                AddMediaDialogFragment.RESP_TAKE_PHOTO,
                this
            ) { _, _ ->
                addClicked(AddMediaType.CAMERA)
            }
            supportFragmentManager.setFragmentResultListener(
                AddMediaDialogFragment.RESP_PHOTO_GALLERY,
                this
            ) { _, _ ->
                addClicked(AddMediaType.GALLERY)
            }
            supportFragmentManager.setFragmentResultListener(
                AddMediaDialogFragment.RESP_FILES,
                this
            ) { _, _ ->
                addClicked(AddMediaType.FILES)
            }
        }
    }

    private fun updateBottomNavbar(position: Int) {
        binding.bottomNavBar.updateSelectedItem(isSettings = position == mPagerAdapter.settingsIndex)
        if (position == mPagerAdapter.settingsIndex) {
            binding.breadcrumbContainer.hide()
        } else {
            // Show the breadcrumb container only if there's any server available
            if (Space.current != null) {
                binding.breadcrumbContainer.show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onStart() {
        super.onStart()

        ProofModeHelper.init(this) {
            // Check for any queued uploads and restart, only after ProofMode is correctly initialized.
            UploadService.startUploadService(this)
        }
    }

    override fun onResume() {
        super.onResume()

        refreshSpace()

        mCurrentPagerItem = mLastItem

        if (!Prefs.didCompleteOnboarding) {
            startActivity(Intent(this, Onboarding23Activity::class.java))
        }

        importSharedMedia(intent)
    }


    private fun navigateToFolder(folderId: Long) {
        val folderIndex = mPagerAdapter.getProjectIndexById(folderId)
        if (folderIndex >= 0) {
            binding.pager.setCurrentItem(folderIndex, true)
            mCurrentPagerItem = folderIndex

        } else {
            Toast.makeText(this, "Folder not found", Toast.LENGTH_SHORT).show()
        }
    }


    fun updateAfterDelete(done: Boolean) {
        mMenuDelete?.isVisible = !done

        if (done) refreshCurrentFolderCount()
    }

    private fun addFolder() {
        mNewFolderResultLauncher.launch(Intent(this, AddFolderActivity::class.java))
    }

    private fun refreshSpace() {
        val currentSpace = Space.current
        currentSpace?.let { space ->
            binding.breadcrumbSpace.text = space.friendlyName
            space.setAvatar(binding.spaceIcon)
        } ?: run {
            binding.breadcrumbContainer.visibility = View.INVISIBLE
        }

        refreshProjects()
    }

    private fun refreshProjects(setProjectId: Long? = null) {
        val projects = Space.current?.projects ?: emptyList()

        mPagerAdapter.updateData(projects)

        binding.pager.adapter = mPagerAdapter

        setProjectId?.let {
            mCurrentPagerItem = mPagerAdapter.getProjectIndexById(it, default = 0)
        }
    }

    private fun refreshCurrentProject() {
        val project = getSelectedProject()

        if (project != null) {
            binding.pager.post {
                mPagerAdapter.notifyProjectChanged(project)
            }

            project.space?.setAvatar(binding.spaceIcon)
            binding.breadcrumbFolder.text = project.description
            binding.breadcrumbFolder.show()

        } else {
            this@MainActivity.binding.breadcrumbFolder.cloak()
        }

        refreshCurrentFolderCount()
    }

    private fun refreshCurrentFolderCount() {
        val project = getSelectedProject()

        if (project != null) {
            val count = NumberFormat.getInstance().format(
                project.collections.map { it.size }
                    .reduceOrNull { acc, count -> acc + count } ?: 0)

            binding.folderCount.text = count
            binding.folderCount.show()

        } else {
            binding.folderCount.cloak()
        }
    }

    private fun importSharedMedia(imageIntent: Intent?) {
        if (imageIntent?.action != Intent.ACTION_SEND) return

        val uri = imageIntent.data ?: if ((imageIntent.clipData?.itemCount
                ?: 0) > 0
        ) imageIntent.clipData?.getItemAt(0)?.uri else null
        val path = uri?.path ?: return

        if (path.contains(packageName)) return

        mSnackBar?.show()

        lifecycleScope.launch(Dispatchers.IO) {
            val media = Picker.import(this@MainActivity, getSelectedProject(), uri)

            lifecycleScope.launch(Dispatchers.Main) {
                mSnackBar?.dismiss()
                intent = null

                if (media != null) {
                    preview()
                }
            }
        }
    }

    private fun preview() {
        val projectId = getSelectedProject()?.id ?: return

        PreviewActivity.start(this, projectId)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            2 -> Picker.pickMedia(this, mediaLaunchers.imagePickerLauncher)
        }
    }


    fun getSelectedProject(): Project? {
        return mPagerAdapter.getProject(mCurrentPagerItem)
    }

    fun getSelectedSpace(): Space? {
        return Space.current
    }

    private fun addClicked(mediaType: AddMediaType) {

        // Check if there's any project selected
        if (getSelectedProject() != null) {
            when (mediaType) {
                AddMediaType.CAMERA -> Picker.takePhoto(
                    this@MainActivity,
                    mediaLaunchers.cameraLauncher
                )

                AddMediaType.GALLERY -> Picker.pickMedia(this, mediaLaunchers.imagePickerLauncher)
                AddMediaType.FILES -> Picker.pickFiles(mediaLaunchers.filePickerLauncher)
            }
        } else if (Space.current == null) { // Check if there's any space available
            startActivity(Intent(this, SpaceSetupActivity::class.java))

        } else {

            if (!Prefs.addFolderHintShown) {
                AlertHelper.show(
                    this,
                    R.string.before_adding_media_create_a_new_folder_first,
                    R.string.to_get_started_please_create_a_folder,
                    R.drawable.ic_folder,
                    buttons = listOf(
                        AlertHelper.positiveButton(R.string.add_a_folder) { _, _ ->
                            Prefs.addFolderHintShown = true

                            addFolder()
                        },
                        AlertHelper.negativeButton(R.string.lbl_Cancel)
                    )
                )
            } else {
                addFolder()
            }
        }
    }

    private fun checkNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Timber.d("We have notifications permissions")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showNotificationPermissionRationale()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun showNotificationPermissionRationale() {
        Utility.showMaterialWarning(this, "Accept!") {
            Timber.d("thing")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.menu_main, menu)

        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
//            R.id.snowbird_menu -> {
//                val intent = Intent(this, SpaceSetupActivity::class.java)
//                intent.putExtra("snowbird", true)
//                startActivity(intent)
//                true
//            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
