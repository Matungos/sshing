package com.matungos.sshing.client

import android.text.TextUtils
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Logger
import com.jcraft.jsch.Session
import com.matungos.sshing.BuildConfig
import com.matungos.sshing.extensions.TAG
import com.matungos.sshing.model.Command
import com.matungos.sshing.model.Host
import com.matungos.sshing.utils.LogUtils.logd
import com.matungos.sshing.utils.LogUtils.loge
import com.matungos.sshing.utils.LogUtils.logi
import com.matungos.sshing.utils.LogUtils.logw
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Created by Gabriel on 28/03/2021.
 */
class JSchImplementation : SshClient {

    lateinit var jsch: JSch

    var _session: Session? = null

    override fun initClient() {
        jsch = JSch()
    }

    override fun execute(
        command: Command,
        host: Host,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            JSch.setLogger(object : Logger {
                override fun isEnabled(level: Int): Boolean {
                    return BuildConfig.LOG_ENABLED
                }

                override fun log(level: Int, message: String) {
                    when (level) {
                        Logger.DEBUG -> logd(TAG, message)
                        Logger.ERROR -> loge(TAG, message)
                        Logger.FATAL -> loge(TAG, message)
                        Logger.INFO -> logi(TAG, message)
                        Logger.WARN -> logw(TAG, message)
                        else -> logd(TAG, "level $level $message")
                    }
                }
            })
            val identity = host.identity
            if (!TextUtils.isEmpty(identity.keyPath)) {
                if (TextUtils.isEmpty(identity.keyPassphrase)) {
                    jsch.addIdentity(identity.keyPath)
                } else {
                    jsch.addIdentity(identity.keyPath, identity.keyPassphrase)
                }
            }
            val session = jsch.getSession(identity.username, host.address, host.port)
            _session = session

            if (!TextUtils.isEmpty(identity.password)) {
                session.setPassword(identity.password)
            }
            session.setConfig("StrictHostKeyChecking", "no")
            session.connect()

            val channel = session.openChannel("exec") as ChannelExec
            val inStream = BufferedReader(InputStreamReader(channel.inputStream))
            channel.setCommand(command.command)
            channel.connect()
            val builder = StringBuilder()
            var line: String? = inStream.readLine()
            while (line != null) {
                builder.append(line).append(System.getProperty("line.separator"))
                line = inStream.readLine()
            }
            channel.disconnect()
            val output = builder.toString().trim('\n')
            onSuccess(output)

        } catch (e: Exception) {
            onError(e)

        } finally {
            _session?.disconnect()
        }
    }

    override fun isValidKey(key: String): Boolean {
        return true
    }

    override fun close() {
        _session?.disconnect()
    }

}
