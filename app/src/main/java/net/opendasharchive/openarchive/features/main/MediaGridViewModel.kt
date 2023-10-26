package net.opendasharchive.openarchive.features.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.opendasharchive.openarchive.db.Collection

class MediaGridViewModel : ViewModel() {

    private val mCollections = MutableLiveData<List<Collection>?>()
    val collections: LiveData<List<Collection>?>
        get() = mCollections

    fun setAllCollection() {
        viewModelScope.launch {
            mCollections.value = Collection.getAll()
        }
    }
}