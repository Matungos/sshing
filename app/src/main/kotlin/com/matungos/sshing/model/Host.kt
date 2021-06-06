package com.matungos.sshing.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*

data class Host(

    @SerializedName("id")
    @Expose
    var id: String,

    @SerializedName("label")
    @Expose
    var label: String,

    @SerializedName("address")
    @Expose
    var address: String,

    @SerializedName("port")
    @Expose
    var port: Int,

    @SerializedName("identity")
    @Expose
    var identity: Identity
) {

    constructor(identity: Identity) : this(generateId(), "", "", 22, identity)

    override fun toString(): String {
        return this.label            // What to display in the Spinner list.
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Host

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {

        fun generateId(): String {
            return UUID.randomUUID().toString()
        }

    }

}
