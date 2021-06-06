package com.matungos.sshing.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.facebook.android.crypto.keychain.AndroidConceal
import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain
import com.facebook.crypto.Crypto
import com.facebook.crypto.CryptoConfig
import com.facebook.crypto.Entity
import com.facebook.crypto.exception.CryptoInitializationException
import com.facebook.crypto.exception.KeyChainException
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.matungos.sshing.extensions.TAG
import com.matungos.sshing.model.Command
import com.matungos.sshing.model.Host
import com.matungos.sshing.utils.LogUtils.loge
import com.matungos.sshing.utils.LogUtils.logi
import com.matungos.sshing.utils.LogUtils.logw
import com.matungos.sshing.utils.ObservableList
import java.io.BufferedOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.StreamCorruptedException
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by Gabriel on 11/04/2021.
 */
object DataStore {

    private const val COMMANDS_FILENAME = "COMMANDS"
    private const val HOSTS_FILENAME = "HOSTS"

    private lateinit var gson: Gson

    private lateinit var crypto: Crypto

    private lateinit var applicationContext: Context

    private val commandListImpl: MutableList<Command> = arrayListOf()
    private val commandListMutableLiveData = MutableLiveData<List<Command>>()
    val commandListLiveData: LiveData<List<Command>>
        get() = commandListMutableLiveData
    val commandList: List<Command>
        get() = commandListImpl

    private val hostListImpl: MutableList<Host> = arrayListOf()
    private val hostListMutableLiveData = MutableLiveData<List<Host>>()
    val hostListLiveData: LiveData<List<Host>>
        get() = hostListMutableLiveData
    val hostList: List<Host>
        get() = hostListImpl

    fun init(applicationContext: Context) {
        this.applicationContext = applicationContext

        val keyChain = SharedPrefsBackedKeyChain(applicationContext, CryptoConfig.KEY_256)

        this.crypto = AndroidConceal.get().createDefaultCrypto(keyChain)
        this.gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()

        initCommandList()
        initHostList()
    }

    //region COMMANDS
    private fun initCommandList() {
        val sCommands = readFile(COMMANDS_FILENAME)

        val arrayType = object : TypeToken<ObservableList<Command>>() {}.type

        val obsCommandList = gson.fromJson<ObservableList<Command>>(sCommands, arrayType)

        obsCommandList?.list?.let {
            commandListImpl.addAll(it)
        }

        commandListMutableLiveData.postValue(commandListImpl)
    }

    fun addCommand(command: Command) {
        var position = commandListImpl.indexOf(command)
        if (position == -1) {
            position = 0
        } else {
            commandListImpl.remove(command)
        }
        commandListImpl.add(position, command)
        saveCommands()
        postCommands()
    }

    fun duplicateCommand(command: Command) {
        val position = commandListImpl.indexOf(command)
        val clone = command.copy(id = Command.generateId())
        commandListImpl.add(position + 1, clone)
        saveCommands()
        postCommands()
    }

    fun moveCommand(fromPosition: Int, toPosition: Int) {
        logi(TAG, "moveCommand from $fromPosition to $toPosition")
        Collections.swap(commandListImpl, fromPosition, toPosition)
    }

    fun getCommand(commandId: String): Command? {
        val command = commandList.find { command -> command.id == commandId }
        return command
    }

    fun deleteCommand(command: Command) {
        if (commandListImpl.remove(command)) {
            saveCommands()
            postCommands()
        } else {
            logw(TAG, "item not found")
        }
    }

    fun saveCommands() {
        logi(TAG, "saveCommands")
        val wrapper = ObservableList<Command>()
        wrapper.list = commandListImpl
        writeFile(gson.toJson(wrapper), COMMANDS_FILENAME)
    }

    fun postCommands() {
        val mutableList = ArrayList(commandListImpl)
        commandListMutableLiveData.postValue(mutableList)
    }
    //endregion

    //region HOSTS
    private fun initHostList() {
        val sHosts = readFile(HOSTS_FILENAME)

        val arrayType = object : TypeToken<ObservableList<Host>>() {}.type

        val obsHostList = gson.fromJson<ObservableList<Host>>(sHosts, arrayType)

        obsHostList?.list?.let {
            hostListImpl.addAll(it)
        }

        hostListMutableLiveData.postValue(hostListImpl)
    }

    fun addHost(host: Host) {
        var position = hostListImpl.indexOf(host)
        if (position == -1) {
            position = 0
        } else {
            hostListImpl.remove(host)
        }
        hostListImpl.add(position, host)
        saveHosts()
        postHosts()
    }

    fun duplicateHost(host: Host) {
        val position = hostListImpl.indexOf(host)
        val clone = host.copy(id = Host.generateId())
        hostListImpl.add(position + 1, clone)
        saveHosts()
        postHosts()
    }

    fun moveHost(fromPosition: Int, toPosition: Int) {
        logi(TAG, "moveHost from $fromPosition to $toPosition")
        Collections.swap(hostListImpl, fromPosition, toPosition)
    }

    fun getHost(hostId: String): Host? {
        val host = hostList.find { host -> host.id == hostId }
        return host
    }

    fun deleteHost(host: Host) {
        if (hostListImpl.remove(host)) {
            saveHosts()
            postHosts()
        } else {
            logw(TAG, "item not found")
        }
    }

    fun saveHosts() {
        logi(TAG, "saveHosts")
        val wrapper = ObservableList<Host>()
        wrapper.list = hostListImpl
        writeFile(gson.toJson(wrapper), HOSTS_FILENAME)
    }

    fun postHosts() {
        val mutableList = ArrayList(hostListImpl)
        hostListMutableLiveData.postValue(mutableList)
    }
    //endregion

    private fun writeFile(text: String, fileName: String) {
        try {
            val fileOutputStream = applicationContext.openFileOutput(fileName, Context.MODE_PRIVATE)
            if (!crypto.isAvailable) {
                return
            }
            val fileStream = BufferedOutputStream(fileOutputStream)
            val outputStream = crypto.getCipherOutputStream(
                fileStream, Entity.create(fileName)
            )

            outputStream.write(text.toByteArray(charset("UTF-8")))
            outputStream.close()
            fileStream.close()
            fileOutputStream.close()

        } catch (e: FileNotFoundException) {
            loge(TAG, "FileNotFoundException", e)
        } catch (e: IOException) {
            loge(TAG, "IOException", e)
        } catch (e: Exception) {
            loge(TAG, "Exception", e)
        }
    }

    private fun readFile(fileName: String): String? {
        try {
            val fileStream = applicationContext.openFileInput(fileName)
            val inputStream = crypto.getCipherInputStream(
                fileStream,
                Entity.create(fileName)
            )
            return inputStream.bufferedReader().use { it.readText() }

        } catch (e: FileNotFoundException) {
            // ignore
            // logd(TAG, "FileNotFoundException", e);
        } catch (e: UnsupportedOperationException) {
            loge(TAG, "UnsupportedOperationException", e)
        } catch (e: StreamCorruptedException) {
            loge(TAG, "StreamCorruptedException", e)
        } catch (e: IOException) {
            loge(TAG, "IOException", e)
        } catch (e: KeyChainException) {
            loge(TAG, "KeyChainException", e)
        } catch (e: CryptoInitializationException) {
            loge(TAG, "CryptoInitializationException", e)
        }

        return null
    }

}
