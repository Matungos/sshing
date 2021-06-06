package com.matungos.sshing.presentation.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.MalformedJsonException
import com.matungos.sshing.R
import com.matungos.sshing.SSHingApplication
import com.matungos.sshing.data.DataStore
import com.matungos.sshing.export.ExportData
import com.matungos.sshing.extensions.TAG
import com.matungos.sshing.utils.LogUtils.logd
import com.matungos.sshing.utils.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Created by Gabriel on 21/04/2021.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    sealed class BackupState {
        data class SuccessCreated(val fileName: String) : BackupState()
        data class SuccessRestored(val details: String) : BackupState()
        data class Error(val message: String) : BackupState()
    }

    private val status = SingleLiveEvent<BackupState>()

    fun getStatus(): LiveData<BackupState> {
        return status
    }

    suspend fun importFile(inputStream: InputStream, documentName: String) {
        return withContext(Dispatchers.IO) {
            try {
                inputStream.use { stream ->
                    val fileContent = stream.readBytes().toString(Charsets.UTF_8)
                    val exportData = ExportData(fileContent)
                    logd(TAG, fileContent)
                    var commandsCreatedCount = 0
                    var commandsUpdatedCount = 0
                    val commandList = ArrayList(DataStore.commandList)
                    exportData.commands?.let {
                        for (command in it) {
                            val index = commandList.indexOf(command)
                            if (index >= 0) {
                                commandsUpdatedCount++
                            } else {
                                commandsCreatedCount++
                            }
                            DataStore.addCommand(command)
                        }
                    }
                    var hostsCreatedCount = 0
                    var hostsUpdatedCount = 0
                    val hostList = ArrayList(DataStore.hostList)
                    exportData.hosts?.let {
                        for (host in it) {
                            val index = hostList.indexOf(host)
                            if (index >= 0) {
                                hostsUpdatedCount++
                            } else {
                                hostsCreatedCount++
                            }
                            DataStore.addHost(host)
                        }
                    }

                    val message = mutableListOf<String>()

                    if (commandsCreatedCount > 0) {
                        message.add(
                            "$commandsCreatedCount ${
                                application.getString(
                                    R.string.settings_commands_created_message
                                )
                            }"
                        )
                    }

                    if (commandsUpdatedCount > 0) {
                        message.add(
                            "$commandsUpdatedCount ${
                                application.getString(
                                    R.string.settings_commands_updated_message
                                )
                            }"
                        )
                    }

                    if (hostsCreatedCount > 0) {
                        message.add(
                            "$hostsCreatedCount ${
                                application.getString(
                                    R.string.settings_hosts_created_message
                                )
                            }"
                        )
                    }

                    if (hostsUpdatedCount > 0) {
                        message.add(
                            "$hostsUpdatedCount ${
                                application.getString(
                                    R.string.settings_hosts_updated_message
                                )
                            }"
                        )
                    }

                    if (message.isNotEmpty()) {
                        status.postValue(BackupState.SuccessRestored(message.joinToString("\n")))
                    } else {
                        status.postValue(BackupState.SuccessRestored("No command or host were imported"))
                    }
                }

            } catch (ex: Exception) {
                when (ex) {
                    is JsonSyntaxException,
                    is MalformedJsonException -> {
                        status.postValue(BackupState.Error("unable to parse $documentName"))
                    }
                    else -> status.postValue(BackupState.Error("unknown error reading $documentName"))
                }
            }
        }
    }

    suspend fun exportData(
        documentName: String,
        documentStream: OutputStream,
        includeSensitiveData: Boolean
    ) {
        return withContext(Dispatchers.IO) {
            try {
                val exportData = createExportData(includeSensitiveData)
                documentStream.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                    writer.write(exportData)
                }
                status.postValue(BackupState.SuccessCreated(documentName))

            } catch (ex: Exception) {
                status.postValue(BackupState.Error("unable to create $documentName"))
            }
        }
    }

    private fun createExportData(includeSensitiveData: Boolean): String {
        val exportData = ExportData(
            DataStore.commandList,
            DataStore.hostList,
            includeSensitiveData
        )
        logd(TAG, exportData.toString())
        val exportData2 = ExportData(exportData.toString())
        logd(TAG, exportData2.toString())
        return exportData.toString()
    }

    private val application by lazy {
        getApplication<SSHingApplication>()
    }

}
