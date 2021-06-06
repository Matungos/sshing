package com.matungos.sshing.appwidget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import com.matungos.sshing.data.DataStore
import com.matungos.sshing.model.Command

/**
 * Created by Gabriel on 29/04/2021.
 */
class WidgetConfigViewModel(application: Application) : AndroidViewModel(application) {

    // MediatorLiveData can observe other LiveData objects and react on their emissions.
    val commandList: MediatorLiveData<List<Command>> = MediatorLiveData()

    init {
        commandList.addSource(DataStore.commandListLiveData) { value ->
            commandList.postValue(value)
        }
    }

}
