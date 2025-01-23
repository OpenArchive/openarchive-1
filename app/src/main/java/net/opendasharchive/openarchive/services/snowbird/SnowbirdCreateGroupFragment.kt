package net.opendasharchive.openarchive.services.snowbird

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.databinding.FragmentSnowbirdCreateGroupBinding
import net.opendasharchive.openarchive.db.SnowbirdError
import net.opendasharchive.openarchive.db.SnowbirdGroup
import net.opendasharchive.openarchive.db.SnowbirdRepo
import net.opendasharchive.openarchive.features.onboarding.BaseFragment
import net.opendasharchive.openarchive.util.FullScreenOverlayCreateGroupManager
import net.opendasharchive.openarchive.util.Utility
import timber.log.Timber

class SnowbirdCreateGroupFragment private constructor() : BaseFragment() {

    private lateinit var viewBinding: FragmentSnowbirdCreateGroupBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentSnowbirdCreateGroupBinding.inflate(inflater)

        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.createGroupButton.setOnClickListener {
            snowbirdGroupViewModel.createGroup(viewBinding.groupNameTextfield.text.toString())
            dismissKeyboard(it)
        }

        initializeViewModelObservers()
    }

    private fun initializeViewModelObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    snowbirdGroupViewModel.groupState.collect { state ->
                        handleGroupStateUpdate(
                            state
                        )
                    }
                }
                launch {
                    snowbirdRepoViewModel.repoState.collect { state ->
                        handleRepoStateUpdate(
                            state
                        )
                    }
                }
            }
        }
    }

    private fun handleGroupStateUpdate(state: SnowbirdGroupViewModel.GroupState) {
        Timber.d("group state = $state")
        when (state) {
            is SnowbirdGroupViewModel.GroupState.Loading -> handleCreateGroupLoadingStatus(true)
            is SnowbirdGroupViewModel.GroupState.SingleGroupSuccess -> handleGroupCreated(state.group)
            is SnowbirdGroupViewModel.GroupState.Error -> handleError(state.error)
            else -> Unit
        }
    }

    private fun handleCreateGroupLoadingStatus(isLoading: Boolean) {
        if (isLoading) {
            FullScreenOverlayCreateGroupManager.show(this@SnowbirdCreateGroupFragment)
        } else {
            FullScreenOverlayCreateGroupManager.hide()
        }
    }


    private fun handleRepoStateUpdate(state: SnowbirdRepoViewModel.RepoState) {
        Timber.d("repo state = $state")
        when (state) {
            is SnowbirdRepoViewModel.RepoState.Loading -> handleCreateGroupLoadingStatus(true)
            is SnowbirdRepoViewModel.RepoState.SingleRepoSuccess -> handleRepoCreated(state.repo)
            is SnowbirdRepoViewModel.RepoState.Error -> handleError(state.error)
            else -> Unit
        }
    }

    override fun handleError(error: SnowbirdError) {
        handleCreateGroupLoadingStatus(false)
        super.handleError(error)
    }

    private fun handleGroupCreated(group: SnowbirdGroup?) {
        group?.let {
            snowbirdGroupViewModel.setCurrentGroup(group)

            lifecycleScope.launch {
                group.save()
                snowbirdRepoViewModel.createRepo(
                    group.key, viewBinding.repoNameTextfield.text.toString()
                )
            }
        }
    }

    private fun handleRepoCreated(repo: SnowbirdRepo?) {
        handleCreateGroupLoadingStatus(false)
        repo?.let {
            repo.groupKey = snowbirdGroupViewModel.currentGroup.value!!.key
            repo.permissions = "READ_WRITE"
            repo.save()
            showConfirmation(repo)
        }
    }

    private fun showConfirmation(repo: SnowbirdRepo?) {
        val group = SnowbirdGroup.get(repo!!.groupKey)!!

        Utility.showMaterialPrompt(
            requireContext(),
            title = "Raven Group Created",
            message = "Would you like to share your new group with a QR code?",
            positiveButtonText = "Yes",
            negativeButtonText = "No",
            completion = { affirm ->
                if (affirm) {
                    setFragmentResult(
                        RESULT_REQUEST_KEY,
                        bundleOf(
                            RESULT_NAVIGATION_KEY to RESULT_NAVIGATION_VAL_SHARE_SCREEN,
                            RESULT_BUNDLE_GROUP_KEY to group.key
                        )
                    )
                    //findNavController().navigate(SnowbirdCreateGroupFragmentDirections.navigateToShareScreen(group.key))
                } else {
                    parentFragmentManager.popBackStack()
                }
            }
        )
    }

    override fun getToolbarTitle(): String {
        return "Create Raven Group"
    }

    companion object {

        const val RESULT_REQUEST_KEY = "create_group_result"

        const val RESULT_NAVIGATION_KEY = "create_group_navigation"

        const val RESULT_NAVIGATION_VAL_SHARE_SCREEN = "share_screen"

        const val RESULT_BUNDLE_GROUP_KEY = "raven_create_group_fragment_bundle_group_id"

        @JvmStatic
        fun newInstance() = SnowbirdCreateGroupFragment()
    }

}