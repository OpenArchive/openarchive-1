package net.opendasharchive.openarchive.features.spaces

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.SpaceAdapter
import net.opendasharchive.openarchive.SpaceAdapterListener
import net.opendasharchive.openarchive.SpaceItemDecoration
import net.opendasharchive.openarchive.databinding.ActivitySpacesBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.core.BaseActivity
import net.opendasharchive.openarchive.features.internetarchive.presentation.InternetArchiveActivity
import net.opendasharchive.openarchive.features.onboarding.SpaceSetupActivity
import net.opendasharchive.openarchive.services.gdrive.GDriveActivity
import net.opendasharchive.openarchive.services.webdav.WebDavActivity

class SpacesActivity : BaseActivity(), SpaceAdapterListener {

    private lateinit var mBinding: ActivitySpacesBinding
    private lateinit var mAdapter: SpaceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        mBinding = ActivitySpacesBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setupToolbar(title = "Servers", showBackButton = true)

        mAdapter = SpaceAdapter(context = this, listener = this)

        mBinding.rvProjects.layoutManager = LinearLayoutManager(this)
        val spacing = resources.getDimensionPixelSize(R.dimen.list_item_spacing)
        mBinding.rvProjects.addItemDecoration(SpaceItemDecoration(spacing))
        mBinding.rvProjects.adapter = mAdapter


        mBinding.fabAdd.setOnClickListener {
            startActivity(Intent(this, SpaceSetupActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()

        val projects = Space.Companion.getAll().asSequence().toList()

        mAdapter.update(projects)
    }

    override fun spaceClicked(space: Space) {
        Space.Companion.current = space
        finish()
    }

    override fun editSpaceClicked(spaceId: Long?) {
        startSpaceAuthActivity(spaceId)
    }

    override fun getSelectedSpace(): Space? {
        return Space.Companion.current
    }

    private fun startSpaceAuthActivity(spaceId: Long?) {
        val space = Space.Companion.get(spaceId ?: return) ?: return

        val clazz = when (space.tType) {
            Space.Type.INTERNET_ARCHIVE -> InternetArchiveActivity::class.java
            Space.Type.GDRIVE -> GDriveActivity::class.java
            else -> WebDavActivity::class.java
        }

        val intent = Intent(this@SpacesActivity, clazz)
        intent.putExtra(EXTRA_DATA_SPACE, space.id)

        startActivity(intent)
    }
}