package com.matungos.sshing.client

/**
 * Created by Gabriel on 29/03/2021.
 */
object ClientFactory {

    fun getClient(): SshClient {
        //return JSchImplementation()
        val client = SshjImplementation()
        client.initClient()
        return client
    }

}
