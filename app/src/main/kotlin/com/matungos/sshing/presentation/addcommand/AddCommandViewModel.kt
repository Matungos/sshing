package com.matungos.sshing.presentation.addcommand

import android.app.Application
import android.text.TextUtils
import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.matungos.sshing.BR
import com.matungos.sshing.R
import com.matungos.sshing.SSHingApplication
import com.matungos.sshing.data.DataStore
import com.matungos.sshing.model.Command
import com.matungos.sshing.model.Host
import com.matungos.sshing.utils.ObservableViewModel
import com.matungos.sshing.utils.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by Gabriel on 14/04/2021.
 */
class AddCommandViewModel(application: Application) : ObservableViewModel(application) {

    private val status = SingleLiveEvent<AddCommandStatus>()

    fun getStatus(): LiveData<AddCommandStatus> {
        return status
    }

    lateinit var command: Command

    var host: Host? = null
        //get() = field
        set(value) {
            field = value
            command.hostId = value?.id ?: ""
            notifyPropertyChanged(BR.hostButtonText)
        }

    private val application by lazy {
        getApplication<SSHingApplication>()
    }

    @Bindable
    fun getHostButtonText(): String {
        host?.let {
            return it.label
        }
        return application.getString(R.string.add_command_select_host_text)
    }

    @Bindable
    fun getColor(): Int {
        return command.color
    }

    fun setColor(color: Int) {
        if (command.color != color) {
            command.color = color
            // Notify observers of a new value.
            notifyPropertyChanged(BR.color)
        }
    }

    fun isCriticalChanged() {
        setIsCritical(!command.isCritical)
    }

    @Bindable
    fun getIsCritical(): Boolean {
        return command.isCritical
    }

    fun setIsCritical(value: Boolean) {
        // Avoids infinite loops.
        if (command.isCritical != value) {
            command.isCritical = value
            // Notify observers of a new value.
            notifyPropertyChanged(BR.isCritical)
        }
    }

    fun doNotNotifyEmptyResponseChanged() {
        setDoNotNotifyEmptyResponse(!command.doNotNotifyEmptyResponse)
    }

    @Bindable
    fun getDoNotNotifyEmptyResponse(): Boolean {
        return command.doNotNotifyEmptyResponse
    }

    fun setDoNotNotifyEmptyResponse(value: Boolean) {
        // Avoids infinite loops.
        if (command.doNotNotifyEmptyResponse != value) {
            command.doNotNotifyEmptyResponse = value
            // Notify observers of a new value.
            notifyPropertyChanged(BR.doNotNotifyEmptyResponse)
        }
    }

    fun saveAction() {
        if (TextUtils.isEmpty(command.label)) {
            status.value = AddCommandStatus.EMPTY_LABEL
            return
        } else {
            status.value = AddCommandStatus.LABEL_OK
        }
        if (TextUtils.isEmpty(command.command)) {
            status.value = AddCommandStatus.EMPTY_COMMAND
            return
        } else {
            status.value = AddCommandStatus.COMMAND_OK
        }
        if (TextUtils.isEmpty(command.hostId)) {
            status.value = AddCommandStatus.HOST_NOT_SELECTED
        } else {
            // save and exit
            viewModelScope.launch(Dispatchers.IO) {
                DataStore.addCommand(command)
            }
            status.value = AddCommandStatus.COMMAND_CREATED
        }
    }

}

enum class AddCommandStatus {
    EMPTY_LABEL,
    LABEL_OK,
    EMPTY_COMMAND,
    COMMAND_OK,
    HOST_NOT_SELECTED,
    COMMAND_CREATED
}
