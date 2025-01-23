package net.opendasharchive.openarchive.features.folders

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityBrowseFoldersBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.util.extensions.toggle
import java.util.Date


class BrowseFoldersActivity : BaseActivity() {

    private lateinit var mBinding: ActivityBrowseFoldersBinding
    private val mViewModel: BrowseFoldersViewModel by viewModels()

    private var mSelected: Folder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityBrowseFoldersBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setupToolbar(
            title = getString(R.string.browse_existing),
            showBackButton = true
        )

        mBinding.rvFolderList.layoutManager = LinearLayoutManager(this)

        val space = Space.current
        if (space != null) mViewModel.getFiles(this, space)

        mViewModel.folders.observe(this) {
            mBinding.projectsEmpty.toggle(it.isEmpty())

            mBinding.rvFolderList.adapter = BrowseFoldersAdapter(it) { folder ->
                this.mSelected = folder
                invalidateOptionsMenu()
            }
        }

        mViewModel.progressBarFlag.observe(this) {
            mBinding.progressBar.toggle(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_browse_folder, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val addMenuItem = menu?.findItem(R.id.action_add)
        addMenuItem?.isVisible = mSelected != null
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add -> {
                addFolder(mSelected)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun addFolder(folder: Folder?) {
        if (folder == null) return
        val space = Space.current ?: return

        // This should not happen. These should have been filtered on display.
        if (space.hasProject(folder.name)) return

        val license = space.license

//        if (license.isNullOrBlank()) {
//            val i = Intent()
//            i.putExtra(AddFolderActivity.EXTRA_FOLDER_NAME, folder.name)
//
//            setResult(RESULT_CANCELED, i)
//        }
//        else {
            val project = Project(folder.name, Date(), space.id, licenseUrl = license)
            project.save()

            val i = Intent()
            i.putExtra(AddFolderActivity.EXTRA_FOLDER_ID, project.id)

            setResult(RESULT_OK, i)
//        }

        finish()
    }
}