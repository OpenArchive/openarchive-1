package net.opendasharchive.openarchive.features.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import net.opendasharchive.openarchive.databinding.FragmentSpaceSetupBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.main.MainActivity
import net.opendasharchive.openarchive.features.onboarding.BaseFragment
import net.opendasharchive.openarchive.util.extensions.hide

class SpaceSetupFragment : BaseFragment() {

    private lateinit var mBinding: FragmentSpaceSetupBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentSpaceSetupBinding.inflate(inflater)

        mBinding.webdav.setOnClickListener {
            setFragmentResult(RESULT_REQUEST_KEY, bundleOf(RESULT_BUNDLE_KEY to RESULT_VAL_WEBDAV))
        }

        if (Space.has(Space.Type.INTERNET_ARCHIVE)) {
            mBinding.internetArchive.hide()
        } else {
            mBinding.internetArchive.setOnClickListener {
                setFragmentResult(
                    RESULT_REQUEST_KEY,
                    bundleOf(RESULT_BUNDLE_KEY to RESULT_VAL_INTERNET_ARCHIVE)
                )
            }
        }


        mBinding.snowbird.setOnClickListener {
            setFragmentResult(RESULT_REQUEST_KEY, bundleOf(RESULT_BUNDLE_KEY to RESULT_VAL_RAVEN))
        }

        return mBinding.root
    }

    companion object {
        const val RESULT_REQUEST_KEY = "space_setup_fragment_result"
        const val RESULT_BUNDLE_KEY = "space_setup_result_key"
        const val RESULT_VAL_DROPBOX = "dropbox"
        const val RESULT_VAL_WEBDAV = "webdav"
        const val RESULT_VAL_RAVEN = "raven"
        const val RESULT_VAL_INTERNET_ARCHIVE = "internet_archive"
        const val RESULT_VAL_GDRIVE = "gdrive"
    }

    override fun getToolbarTitle() = "Select a Server"
    override fun getToolbarSubtitle(): String? = null
    override fun shouldShowBackButton() = true
}