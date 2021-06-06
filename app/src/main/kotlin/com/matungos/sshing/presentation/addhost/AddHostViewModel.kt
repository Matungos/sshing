package com.matungos.sshing.presentation.addhost

import android.app.Application
import android.text.TextUtils
import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.matungos.sshing.BR
import com.matungos.sshing.client.ClientFactory
import com.matungos.sshing.data.DataStore
import com.matungos.sshing.model.Host
import com.matungos.sshing.utils.ObservableViewModel
import com.matungos.sshing.utils.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Created by Gabriel on 20/04/2021.
 */
class AddHostViewModel(application: Application) : ObservableViewModel(application) {

    lateinit var host: Host

    private val status = SingleLiveEvent<AddHostStatus>()

    fun getStatus(): LiveData<AddHostStatus> {
        return status
    }

    private val keyStatus = SingleLiveEvent<KeyStatus>()

    fun getKeyStatus(): LiveData<KeyStatus> {
        return keyStatus
    }

    fun setKey(keyStatus: KeyStatus.SuccessKeyRead?) {
        if (keyStatus != null) {
            if (host.identity.keyPath != keyStatus.fileName ||
                host.identity.key != keyStatus.content
            ) {
                host.identity.keyPath = keyStatus.fileName
                host.identity.key = keyStatus.content
                // Notify observers of a new value.
                notifyPropertyChanged(BR.key)
            }
        } else {
            host.identity.keyPath = null
            host.identity.key = null
            // Notify observers of a new value.
            notifyPropertyChanged(BR.key)
        }
    }

    fun setKey(keyPath: String) {
        if (host.identity.keyPath != keyPath) {
            if (TextUtils.isEmpty(keyPath)) {
                host.identity.keyPath = null
                host.identity.key = null
            } else {
                host.identity.keyPath = keyPath
            }
            // Notify observers of a new value.
            notifyPropertyChanged(BR.key)
        }
    }

    @Bindable
    fun getKey(): String {
        return host.identity.keyPath ?: ""
    }

    suspend fun readKey(inputStream: InputStream, documentName: String) {
        return withContext(Dispatchers.IO) {
            try {
                inputStream.use { stream ->
                    val key = stream.readBytes().toString(Charsets.UTF_8)
                    val client = ClientFactory.getClient()
                    val isValidKey = client.isValidKey(key)
                    withContext(Dispatchers.Main) {
                        if (isValidKey) {
                            keyStatus.postValue(KeyStatus.SuccessKeyRead(documentName, key))
                        } else {
                            status.postValue(AddHostStatus.INVALID_KEY)
                        }
                    }
                }
            } catch (ex: Exception) {
                status.postValue(AddHostStatus.INVALID_KEY)
            }
        }
    }

    fun saveAction() {
        val label = host.label.trim { it <= ' ' }
        val address = host.address.trim { it <= ' ' }
        val username = host.identity.username?.trim { it <= ' ' }
        val password = host.identity.password
        val key = host.identity.key?.trim { it <= ' ' }

        if (TextUtils.isEmpty(label)) {
            status.value = AddHostStatus.EMPTY_LABEL
            return
        } else {
            status.value = AddHostStatus.LABEL_OK
        }
        if (TextUtils.isEmpty(address)) {
            status.value = AddHostStatus.EMPTY_ADDRESS
            return
        } else {
            status.value = AddHostStatus.ADDRESS_OK
        }
        if (host.port < 1) {
            status.value = AddHostStatus.INVALID_PORT
            return
        } else {
            status.value = AddHostStatus.PORT_OK
        }
        if (TextUtils.isEmpty(username)) {
            status.value = AddHostStatus.EMPTY_USERNAME
            return
        } else {
            status.value = AddHostStatus.USERNAME_OK
        }
        if (TextUtils.isEmpty(password) && TextUtils.isEmpty(key)) {
            status.value = AddHostStatus.EMPTY_PASSWORD
            return
        } else {
            status.value = AddHostStatus.PASSWORD_OK
        }

        viewModelScope.launch(Dispatchers.IO) {
            DataStore.addHost(host)
        }
        status.value = AddHostStatus.HOST_CREATED
    }

}

sealed class KeyStatus {
    data class SuccessKeyRead(val fileName: String, val content: String) : KeyStatus()
}

enum class AddHostStatus {
    EMPTY_LABEL,
    LABEL_OK,
    EMPTY_ADDRESS,
    ADDRESS_OK,
    INVALID_PORT,
    INVALID_KEY,
    PORT_OK,
    EMPTY_USERNAME,
    USERNAME_OK,
    EMPTY_PASSWORD,
    PASSWORD_OK,
    HOST_CREATED
}
