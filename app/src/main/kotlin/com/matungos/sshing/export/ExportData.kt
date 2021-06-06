package com.matungos.sshing.export

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.matungos.sshing.model.Command
import com.matungos.sshing.model.Host

class ExportData {

    @SerializedName("commands")
    @Expose
    val commands: List<Command>?

    @SerializedName("hosts")
    @Expose
    val hosts: List<Host>?

    constructor(commands: List<Command>, hosts: List<Host>, includeSensitiveData: Boolean) {
        this.commands = commands
        this.hosts = hosts.toList().map { host ->
            val hostCopy = host.copy()
            if (!includeSensitiveData) {
                hostCopy.identity = hostCopy.identity.copy(
                    password = null,
                    key = null,
                    keyPath = null,
                    keyPassphrase = null
                )
            } else {
                hostCopy.identity = hostCopy.identity.copy()
            }
            hostCopy
        }
    }

    constructor(fromString: String) {
        val gson = getGson()
        val arrayType = object : TypeToken<ExportData>() {}.type
        val fromJson = gson.fromJson<ExportData>(fromString, arrayType)
        this.commands = fromJson.commands
        this.hosts = fromJson.hosts
    }

    override fun toString(): String {
        val gson = getGson()
        return gson.toJson(this)
    }

    private fun getGson(): Gson {
        return GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
    }

}
