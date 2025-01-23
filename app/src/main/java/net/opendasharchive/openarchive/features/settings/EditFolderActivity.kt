package net.opendasharchive.openarchive.features.settings

import android.os.Bundle
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityEditFolderBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.extensions.Position
import net.opendasharchive.openarchive.util.extensions.setDrawable

class EditFolderActivity : BaseActivity() {

    companion object {
        const val EXTRA_CURRENT_PROJECT_ID = "archive_extra_current_project_id"
    }

    private lateinit var mProject: Project
    private lateinit var mBinding: ActivityEditFolderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val project = Project.getById(intent.getLongExtra(EXTRA_CURRENT_PROJECT_ID, -1L))
            ?: return finish()

        mProject = project

        mBinding = ActivityEditFolderBinding.inflate(layoutInflater)
        setContentView(mBinding.root)


        setupToolbar("Edit Folder")

        mBinding.folderName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val newName = mBinding.folderName.text.toString()

                if (newName.isNotBlank()) {
                    mProject.description = newName
                    mProject.save()

                    supportActionBar?.title = newName
                    mBinding.folderName.hint = newName


                    setupToolbar(newName)
                }
            }

            false
        }

        mBinding.btRemove.setDrawable(R.drawable.ic_delete, Position.Start, 0.5)
        mBinding.btRemove.setOnClickListener {
            removeProject()
        }

        mBinding.btArchive.setOnClickListener {
            archiveProject()
        }

        CcSelector.init(mBinding.cc, null) {
            mProject.licenseUrl = it
            mProject.save()
        }

        updateUi()
    }

    private fun removeProject() {
        AlertHelper.show(this, R.string.action_remove_project, R.string.remove_from_app, buttons = listOf(
            AlertHelper.positiveButton(R.string.remove) { _, _ ->
                mProject.delete()

                finish()
            },
            AlertHelper.negativeButton()))
    }

    private fun archiveProject() {
        mProject.isArchived = !mProject.isArchived
        mProject.save()

        updateUi()
    }

    private fun updateUi() {
        supportActionBar?.title = mProject.description

        mBinding.folderName.isEnabled = !mProject.isArchived
        mBinding.folderName.hint = mProject.description
        mBinding.folderName.setText(mProject.description)

        mBinding.btArchive.setText(if (mProject.isArchived)
            R.string.action_unarchive_project else
            R.string.action_archive_project)

        val global = mProject.space?.license != null

        if (global) {
            mBinding.cc.tvCc.setText(R.string.set_the_same_creative_commons_license_for_all_folders_on_this_server)
        }

        CcSelector.set(mBinding.cc, mProject.licenseUrl, !mProject.isArchived && !global)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}