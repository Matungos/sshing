package com.matungos.sshing.presentation.commandlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.matungos.sshing.data.DataStore
import com.matungos.sshing.model.Command
import com.matungos.sshing.model.Host
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by Gabriel on 11/04/2021.
 */
class CommandListViewModel(application: Application) : AndroidViewModel(application) {

    // MediatorLiveData can observe other LiveData objects and react on their emissions.
    val commandList: MediatorLiveData<List<Command>> = MediatorLiveData()
    val hostList: MediatorLiveData<List<Host>> = MediatorLiveData()

    init {
        hostList.addSource(DataStore.hostListLiveData) { value ->
            hostList.postValue(value)
        }
        commandList.addSource(DataStore.commandListLiveData) { value ->
            commandList.postValue(value)
        }
    }

    fun saveCommands() {
        viewModelScope.launch(Dispatchers.IO) {
            DataStore.saveCommands()
            DataStore.postCommands()
        }
    }

    fun deleteCommand(command: Command) {
        viewModelScope.launch(Dispatchers.IO) {
            DataStore.deleteCommand(command)
        }
    }

    fun duplicateCommand(command: Command) {
        viewModelScope.launch(Dispatchers.IO) {
            DataStore.duplicateCommand(command)
        }
    }

    fun moveCommand(fromPosition: Int, toPosition: Int) {
        DataStore.moveCommand(fromPosition, toPosition)
    }

}
