package com.matungos.sshing.client

import com.matungos.sshing.model.Command
import com.matungos.sshing.model.Host

/**
 * Created by Gabriel on 28/03/2021.
 */
interface SshClient {

    fun initClient()

    fun execute(
        command: Command,
        host: Host,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    )

    fun isValidKey(key: String): Boolean

    fun close()

}
