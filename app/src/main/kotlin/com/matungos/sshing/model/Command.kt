package com.matungos.sshing.model

import androidx.annotation.ColorInt
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*

data class Command(

    @SerializedName("id")
    @Expose
    var id: String,

    @SerializedName("label")
    @Expose
    var label: String,

    @SerializedName("command")
    @Expose
    var command: String,

    @SerializedName("critical")
    @Expose
    var isCritical: Boolean,

    @SerializedName("color")
    @Expose
    @ColorInt
    var color: Int,

    @SerializedName("host_id")
    @Expose
    var hostId: String,

    @SerializedName("do_not_notify_empty_response")
    @Expose
    var doNotNotifyEmptyResponse: Boolean
) {

    constructor() : this(generateId(), "", "", false, 0, "", false)

    constructor(
        label: String,
        string: String,
        isCritical: Boolean,
        color: Int,
        hostId: String,
        doNotNotifyEmptyResponse: Boolean
    ) : this(generateId(), label, string, isCritical, color, hostId, doNotNotifyEmptyResponse)

    companion object {

        fun generateId(): String {
            return UUID.randomUUID().toString()
        }

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Command

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Command(id='$id', label='$label', command='$command', isCritical=$isCritical, color=$color, hostId='$hostId', doNotNotifyEmptyResponse=$doNotNotifyEmptyResponse)"
    }

}
