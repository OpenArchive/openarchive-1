package net.opendasharchive.openarchive.services.snowbird

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import net.opendasharchive.openarchive.db.SnowbirdError
import net.opendasharchive.openarchive.extensions.androidViewModel
import net.opendasharchive.openarchive.util.Utility

open class BaseSnowbirdFragment : Fragment() {
    val snowbirdGroupViewModel: SnowbirdGroupViewModel by androidViewModel()
    val snowbirdRepoViewModel: SnowbirdRepoViewModel by androidViewModel()

    open fun dismissKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    open fun handleError(error: SnowbirdError) {
        Utility.showMaterialWarning(
            requireContext(),
            error.friendlyMessage
        )
    }

    open fun handleLoadingStatus(isLoading: Boolean) {
        if (isLoading) {
            //FullScreenOverlayManager.show(this@BaseSnowbirdFragment)
        } else {
            //FullScreenOverlayManager.hide()
        }
    }
}