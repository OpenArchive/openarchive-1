package net.opendasharchive.openarchive.features.folders

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityCreateNewFolderBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.settings.CcSelector
import net.opendasharchive.openarchive.util.extensions.hide
import java.util.Date

class CreateNewFolderActivity : BaseActivity() {

    companion object {
        private const val SPECIAL_CHARS = ".*[\\\\/*\\s]"
    }

    private lateinit var mBinding: ActivityCreateNewFolderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityCreateNewFolderBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setupToolbar(
            title = "Create Folder",
            showBackButton = true
        )

        mBinding.newFolder.setText(intent.getStringExtra(AddFolderActivity.EXTRA_FOLDER_NAME))

        mBinding.newFolder.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                store()
            }

            false
        }

        if (Space.current?.license != null) {
            mBinding.cc.root.hide()
        }
        else {
            CcSelector.init(mBinding.cc)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_new_folder, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.action_done -> {
                store()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun store() {
        val name = mBinding.newFolder.text.toString()

        if (name.isBlank()) return

        if (name.matches(SPECIAL_CHARS.toRegex())) {
            Toast.makeText(this,
                getString(R.string.please_do_not_include_special_characters_in_the_name),
                Toast.LENGTH_SHORT).show()

            return
        }

        val space = Space.current ?: return

        if (space.hasProject(name)) {
            Toast.makeText(this, getString(R.string.folder_name_already_exists),
                Toast.LENGTH_LONG).show()

            return
        }

        val license = space.license ?: CcSelector.get(mBinding.cc)

        val project = Project(name, Date(), space.id, licenseUrl = license)
        project.save()

        val i = Intent()
        i.putExtra(AddFolderActivity.EXTRA_FOLDER_ID, project.id)

        setResult(RESULT_OK, i)
        finish()
    }
}
