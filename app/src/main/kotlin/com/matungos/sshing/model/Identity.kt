package com.matungos.sshing.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*

data class Identity(

    @SerializedName("id")
    @Expose
    var id: String,

    @SerializedName("username")
    @Expose
    var username: String?,

    @SerializedName("password")
    @Expose
    var password: String?,

    @SerializedName(value = "key")
    @Expose
    var key: String?,

    @SerializedName(value = "key_path", alternate = ["keyPath"])
    @Expose
    var keyPath: String?,

    @SerializedName(value = "key_passphrase", alternate = ["keyPassphrase"])
    @Expose
    var keyPassphrase: String?
) {

    constructor() : this(generateId(), null, null, null, null, null)

    companion object {

        fun generateId(): String {
            return UUID.randomUUID().toString()
        }

    }

}
