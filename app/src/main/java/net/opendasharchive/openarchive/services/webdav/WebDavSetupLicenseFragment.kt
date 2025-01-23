package net.opendasharchive.openarchive.services.webdav

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentWebdavSetupLicenseBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.onboarding.BaseFragment
import net.opendasharchive.openarchive.features.settings.CcSelector
import kotlin.properties.Delegates

class WebDavSetupLicenseFragment: BaseFragment() {

    private lateinit var binding: FragmentWebdavSetupLicenseBinding

    private var mSpaceId by Delegates.notNull<Long>()

    private lateinit var mSpace: Space

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentWebdavSetupLicenseBinding.inflate(layoutInflater)
        mSpaceId = arguments?.getLong(ARG_SPACE_ID)!!
        mSpace = Space.get(mSpaceId) ?: Space(Space.Type.WEBDAV)

        val isEditing = arguments?.getBoolean(ARG_IS_EDITING) ?: false

        if(isEditing) {
            // Editing means hide subtitle, bottom bar buttons
            binding.buttonBar.visibility = View.GONE
            binding.descriptionText.visibility = View.GONE
        }


        binding.btNext.setOnClickListener {
            setFragmentResult(RESP_SAVED, bundleOf())
        }

        binding.btCancel.setOnClickListener {
            setFragmentResult(RESP_CANCEL, bundleOf())
        }

        binding.cc.tvCc.setText(R.string.set_creative_commons_license_for_all_folders_on_this_server)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isEditing = arguments?.getBoolean(ARG_IS_EDITING) ?: false

        if(isEditing) {
            // Editing means hide subtitle, bottom bar buttons
            binding.name.setText(mSpace.name)
        }

        binding.name.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // Do nothing
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // Do nothing
            }

            override fun afterTextChanged(name: Editable?) {
                if (name == null) return

                mSpace.name = name.toString()
                mSpace.save()
            }
        })

        CcSelector.init(binding.cc, Space.current?.license) {
            val space = Space.current ?: return@init

            space.license = it
            space.save()
        }
    }

    companion object {

        // events emitted by this fragment
        const val RESP_SAVED = "webdav_setup_license_fragment_resp_saved"
        const val RESP_CANCEL = "webdav_setup_license_fragment_resp_cancel"

        const val ARG_SPACE_ID = "space_id"
        const val ARG_IS_EDITING = "isEditing"

        @JvmStatic
        fun newInstance(spaceId: Long, isEditing: Boolean) = WebDavSetupLicenseFragment().apply {
            arguments = Bundle().apply {
                // add any arguments here
                putLong(ARG_SPACE_ID, spaceId)
                putBoolean(ARG_IS_EDITING, isEditing)
            }
        }
    }

    override fun getToolbarTitle() = "Select a License"
    override fun getToolbarSubtitle(): String? = null
    override fun shouldShowBackButton() = false
}