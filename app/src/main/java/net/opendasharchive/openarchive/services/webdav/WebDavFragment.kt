package net.opendasharchive.openarchive.services.webdav

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.BuildConfig
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentWebDavBinding
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.onboarding.BaseFragment
import net.opendasharchive.openarchive.services.SaveClient
import net.opendasharchive.openarchive.services.internetarchive.Util
import net.opendasharchive.openarchive.util.AlertHelper
import net.opendasharchive.openarchive.util.Utility
import net.opendasharchive.openarchive.util.extensions.makeSnackBar
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.suspendCoroutine

class WebDavFragment : BaseFragment() {
    private var mSpaceId: Long? = null
    private lateinit var mSpace: Space

    private lateinit var mSnackbar: Snackbar
    private lateinit var binding: FragmentWebDavBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSpaceId = arguments?.getLong(ARG_SPACE_ID) ?: ARG_VAL_NEW_SPACE
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentWebDavBinding.inflate(inflater)

        mSpaceId = arguments?.getLong(ARG_SPACE_ID) ?: ARG_VAL_NEW_SPACE

        if (mSpaceId != ARG_VAL_NEW_SPACE) {
            // setup views for editing and existing space

            mSpace = Space.get(mSpaceId!!) ?: Space(Space.Type.WEBDAV)

            binding.header.visibility = View.GONE
            binding.buttonBar.visibility = View.GONE
            binding.buttonBarEdit.visibility = View.VISIBLE

            binding.server.isEnabled = false
            binding.username.isEnabled = false
            binding.password.isEnabled = false

            // Disable the password visibility toggle
            binding.passwordLayout.isEndIconVisible = false

            binding.server.setText(mSpace.host)
            binding.username.setText(mSpace.username)
            binding.password.setText(mSpace.password)

            binding.name.setText(mSpace.name)

//            mBinding.swChunking.isChecked = mSpace.useChunking
//            mBinding.swChunking.setOnCheckedChangeListener { _, useChunking ->
//                mSpace.useChunking = useChunking
//                mSpace.save()
//            }


            binding.btRemove.setOnClickListener {
                removeProject()
            }

            // swap webDavFragment with Creative Commons License Fragment
            binding.btLicense.setOnClickListener {
                setFragmentResult(RESP_LICENSE, bundleOf())
            }

            binding.name.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    val enteredName = binding.name.text?.toString()?.trim()
                    if (!enteredName.isNullOrEmpty()) {
                        // Update the Space entity and save it using SugarORM
                        mSpace.name = enteredName
                        mSpace.save() // Save the entity using SugarORM

                        // Hide the keyboard
                        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(binding.name.windowToken, 0)
                        binding.name.clearFocus() // Clear focus from the input field

                        // Optional: Provide feedback to the user
                        Snackbar.make(binding.root, "Name saved successfully!", Snackbar.LENGTH_SHORT).show()
                    } else {
                        // Notify the user that the name cannot be empty (optional)
                        Snackbar.make(binding.root, "Name cannot be empty", Snackbar.LENGTH_SHORT).show()
                    }

                    true // Consume the event
                } else {
                    false // Pass the event to the next listener
                }
            }


        } else {
            // setup views for creating a new space
            mSpace = Space(Space.Type.WEBDAV)
            binding.btRemove.visibility = View.GONE
            binding.buttonBar.visibility = View.VISIBLE
            binding.buttonBarEdit.visibility = View.GONE

            binding.name.visibility = View.GONE
        }

        binding.btAuthenticate.setOnClickListener { attemptLogin() }

        binding.btCancel.setOnClickListener {
            setFragmentResult(RESP_CANCEL, bundleOf())
        }

        binding.server.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                binding.server.setText(fixSpaceUrl(binding.server.text)?.toString())
            }
        }

        binding.password.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                attemptLogin()
            }

            false
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mSnackbar = binding.root.makeSnackBar(getString(R.string.login_activity_logging_message))
    }

    private fun fixSpaceUrl(url: CharSequence?): Uri? {
        if (url.isNullOrBlank()) return null

        val uri = Uri.parse(url.toString())
        val builder = uri.buildUpon()

        if (uri.scheme != "https") {
            builder.scheme("https")
        }

        if (uri.authority.isNullOrBlank()) {
            builder.authority(uri.path)
            builder.path(REMOTE_PHP_ADDRESS)
        } else if (uri.path.isNullOrBlank() || uri.path == "/") {
            builder.path(REMOTE_PHP_ADDRESS)
        }

        return builder.build()
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        // Reset errors.
        binding.username.error = null
        binding.password.error = null

        // Store values at the time of the login attempt.
        var errorView: View? = null

        mSpace.host = fixSpaceUrl(binding.server.text)?.toString() ?: ""
        binding.server.setText(mSpace.host)

        mSpace.username = binding.username.text?.toString() ?: ""
        mSpace.password = binding.password.text?.toString() ?: ""

//        mSpace.useChunking = mBinding.swChunking.isChecked

        if (mSpace.host.isEmpty()) {
            binding.server.error = getString(R.string.error_field_required)
            errorView = binding.server
        } else if (mSpace.username.isEmpty()) {
            binding.username.error = getString(R.string.error_field_required)
            errorView = binding.username
        } else if (mSpace.password.isEmpty()) {
            binding.password.error = getString(R.string.error_field_required)
            errorView = binding.password
        }

        if (errorView != null) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            errorView.requestFocus()

            return
        }

        val other = Space.get(Space.Type.WEBDAV, mSpace.host, mSpace.username)

        if (other.isNotEmpty() && other[0].id != mSpace.id) {
            return showError(getString(R.string.you_already_have_a_server_with_these_credentials))
        }

        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        mSnackbar.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                testConnection()
                mSpace.save()
                Space.current = mSpace

//                CleanInsightsManager.getConsent(requireActivity()) {
//                    CleanInsightsManager.measureEvent("backend", "new", Space.Type.WEBDAV.friendlyName)
//                }

                navigate(mSpace.id)
            } catch (exception: IOException) {
                if (exception.message?.startsWith("401") == true) {
                    showError(getString(R.string.error_incorrect_username_or_password), true)
                } else {
                    showError(exception.localizedMessage ?: getString(R.string.error))
                }
            }
        }
    }

    private fun navigate(spaceId: Long) {
        Utility.showMaterialMessage(
            context = requireContext(),
            title = "Success",
            message = "You have successfully authenticated! Now let's continue setting up your media server."
        ) {
            setFragmentResult(RESP_SAVED, bundleOf(ARG_SPACE_ID to spaceId))
        }
    }

    private suspend fun testConnection() {
        val url = mSpace.hostUrl ?: throw IOException("400 Bad Request")

        val client = SaveClient.get(requireContext(), mSpace.username, mSpace.password)

        val request =
            Request.Builder().url(url).method("GET", null).addHeader("OCS-APIRequest", "true")
                .addHeader("Accept", "application/json").build()

        return suspendCoroutine {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    it.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    val code = response.code
                    val message = response.message

                    response.close()

                    if (code != 200 && code != 204) {
                        return it.resumeWith(Result.failure(IOException("$code $message")))
                    }

                    it.resumeWith(Result.success(Unit))
                }
            })
        }
    }

    private fun showError(text: CharSequence, onForm: Boolean = false) {
        requireActivity().runOnUiThread {
            mSnackbar.dismiss()

            if (onForm) {
                binding.password.error = text
                binding.password.requestFocus()
            } else {
                mSnackbar = binding.root.makeSnackBar(text, Snackbar.LENGTH_LONG)
                mSnackbar.show()

                binding.server.requestFocus()
            }
        }
    }

    override fun onStop() {
        super.onStop()

        // make sure the snack-bar is gone when this fragment isn't on display anymore
        mSnackbar.dismiss()
        // also hide keyboard when fragment isn't on display anymore
        Util.hideSoftKeyboard(requireActivity())
    }

    private fun removeProject() {
        AlertHelper.show(
            requireContext(),
            R.string.are_you_sure_you_want_to_remove_this_server_from_the_app,
            R.string.remove_from_app,
            buttons = listOf(
                AlertHelper.positiveButton(R.string.remove) { _, _ ->
                    mSpace.delete()
                    setFragmentResult(RESP_DELETED, bundleOf())
                }, AlertHelper.negativeButton()
            )
        )
    }

    companion object {
        // events emitted by this fragment
        const val RESP_SAVED = "web_dav_fragment_resp_saved"
        const val RESP_DELETED = "web_dav_fragment_resp_deleted"
        const val RESP_CANCEL = "web_dav_fragment_resp_cancel"
        const val RESP_LICENSE = "web_dav_fragment_resp_license"

        // factory method parameters (bundle args)
        const val ARG_SPACE_ID = "space"
        const val ARG_VAL_NEW_SPACE = -1L

        // other internal constants
        const val REMOTE_PHP_ADDRESS = "/remote.php/webdav/"

        @JvmStatic
        fun newInstance(spaceId: Long) = WebDavFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_SPACE_ID, spaceId)
            }
        }

        @JvmStatic
        fun newInstance() = newInstance(ARG_VAL_NEW_SPACE)
    }

    override fun getToolbarTitle(): String = if (mSpaceId == ARG_VAL_NEW_SPACE) {
        "Add Private Server"
    } else {
        "Edit Private Server"
    }
}