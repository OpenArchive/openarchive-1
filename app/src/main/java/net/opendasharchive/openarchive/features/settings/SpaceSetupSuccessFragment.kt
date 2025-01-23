package net.opendasharchive.openarchive.features.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import net.opendasharchive.openarchive.databinding.FragmentSpaceSetupSuccessBinding
import net.opendasharchive.openarchive.features.onboarding.BaseFragment

class SpaceSetupSuccessFragment private constructor(): BaseFragment() {
    private lateinit var mBinding: FragmentSpaceSetupSuccessBinding
    private var message = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            message = it.getString(ARG_MESSAGE, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentSpaceSetupSuccessBinding.inflate(inflater)

        if (message.isNotEmpty()) {
            mBinding.successMessage.text = message
        }

        mBinding.btAuthenticate.setOnClickListener { _ ->
            setFragmentResult(RESP_DONE, bundleOf())
        }

        return mBinding.root
    }

    companion object {
        const val RESP_DONE = "space_setup_success_fragment_resp_done"

        const val ARG_MESSAGE = "space_setup_success_fragment_arg_message"

        @JvmStatic
        fun newInstance(message: String) =
            SpaceSetupSuccessFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MESSAGE, message)
                }
            }
    }

    override fun getToolbarTitle() = "Setup complete"
    override fun shouldShowBackButton() = false
}