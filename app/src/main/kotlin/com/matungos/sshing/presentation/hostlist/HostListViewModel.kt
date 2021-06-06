package com.matungos.sshing.presentation.hostlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.matungos.sshing.data.DataStore
import com.matungos.sshing.model.Host
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by Gabriel on 20/04/2021.
 */
class HostListViewModel(application: Application) : AndroidViewModel(application) {

    // MediatorLiveData can observe other LiveData objects and react on their emissions.
    val hostList: MediatorLiveData<List<Host>> = MediatorLiveData()

    init {
        hostList.addSource(DataStore.hostListLiveData) { value ->
            hostList.postValue(value)
        }
    }

    fun saveHosts() {
        viewModelScope.launch(Dispatchers.IO) {
            DataStore.saveHosts()
            DataStore.postHosts()
        }
    }

    fun deleteHost(Host: Host) {
        viewModelScope.launch(Dispatchers.IO) {
            DataStore.deleteHost(Host)
        }
    }

    fun duplicateHost(Host: Host) {
        viewModelScope.launch(Dispatchers.IO) {
            DataStore.duplicateHost(Host)
        }
    }

    fun moveHost(fromPosition: Int, toPosition: Int) {
        DataStore.moveHost(fromPosition, toPosition)
    }

}
