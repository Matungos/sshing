package com.matungos.sshing.client

import android.text.TextUtils
import com.matungos.sshing.extensions.TAG
import com.matungos.sshing.model.Command
import com.matungos.sshing.model.Host
import com.matungos.sshing.utils.LogUtils.loge
import com.matungos.sshing.utils.LogUtils.logi
import com.matungos.sshing.utils.LogUtils.logw
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.Factory
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProviderUtil
import net.schmizz.sshj.userauth.password.PasswordUtils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Created by Gabriel on 28/03/2021.
 */
class SshjImplementation : SshClient {

    lateinit var ssh: SSHClient

    private var _session: Session? = null

    override fun initClient() {
        ssh = SSHClient()
    }

    override fun execute(
        command: Command,
        host: Host,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            ssh.addHostKeyVerifier(PromiscuousVerifier())
            ssh.connect(host.address, host.port)

            val identity = host.identity
            if (!TextUtils.isEmpty(identity.key)) {
                val kp = ssh.loadKeys(
                    identity.key,
                    null,
                    PasswordUtils.createOneOff(identity.keyPassphrase?.toCharArray())
                )
                ssh.authPublickey(identity.username, kp)
            }
            if (!TextUtils.isEmpty(identity.username) && !TextUtils.isEmpty(identity.password)) {
                ssh.authPassword(identity.username, identity.password)
            }
            val session = ssh.startSession()
            _session = session

            val cmd: Session.Command = session.exec(command.command)

            logi(TAG, "Exit status: " + cmd.exitStatus)
            if (!TextUtils.isEmpty(cmd.exitErrorMessage)) {
                loge(
                    TAG,
                    "Command " + cmd.toString() + " failed to execute: " + cmd.exitErrorMessage
                )
                onError(RuntimeException(cmd.exitErrorMessage))
            }
            val inStream = BufferedReader(InputStreamReader(cmd.inputStream))
            val builder = StringBuilder()
            var line: String? = inStream.readLine()
            while (line != null) {
                builder.append(line).append(System.getProperty("line.separator"))
                line = inStream.readLine()
            }

            cmd.join(1, TimeUnit.MINUTES)

            val output = builder.toString().trim('\n')
            onSuccess(output)

        } catch (e: Exception) {
            onError(e)

        } finally {
            try {
                _session?.close()
            } catch (e: IOException) {
                // Do Nothing
            }
            try {
                ssh.disconnect()
            } catch (e: IOException) {
                // Do Nothing
            }
        }

    }

    override fun isValidKey(key: String): Boolean {
        return try {

            val format = KeyProviderUtil.detectKeyFileFormat(key, false)
            val fkp = Factory.Named.Util.create(
                ssh.transport.config.fileKeyProviderFactories,
                format.toString()
            )
            fkp != null

        } catch (ex: Exception) {
            false
        }
    }

    override fun close() {
        try {
            _session?.close()
        } catch (e: IOException) {
            // Do Nothing
            logw(TAG, "session.close" + e.message)
        }
        try {
            ssh.disconnect()
        } catch (e: IOException) {
            // Do Nothing
            logw(TAG, "ssh.disconnect" + e.message)
        }
    }

}
