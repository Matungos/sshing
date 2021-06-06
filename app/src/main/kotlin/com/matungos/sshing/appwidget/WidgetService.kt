package com.matungos.sshing.appwidget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import androidx.annotation.RequiresApi
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import androidx.core.content.res.ResourcesCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.matungos.sshing.R
import com.matungos.sshing.appwidget.AbstractWidgetProvider.Companion.FORCE_EXECUTE_ACTION
import com.matungos.sshing.appwidget.AbstractWidgetProvider.Companion.UPDATE_ACTION_EXECUTE
import com.matungos.sshing.client.ClientFactory
import com.matungos.sshing.client.SshClient
import com.matungos.sshing.data.DataStore
import com.matungos.sshing.extensions.TAG
import com.matungos.sshing.extensions.longToast
import com.matungos.sshing.model.Command
import com.matungos.sshing.presentation.CriticalPopupActivity
import com.matungos.sshing.utils.*
import com.matungos.sshing.utils.LogUtils.logd
import com.matungos.sshing.utils.LogUtils.logw
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class WidgetService : JobIntentService() {

    private val handler = Handler(Looper.getMainLooper())

    private var client: SshClient? = null

    private var isCancelled = false

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BROADCAST_ABORT_COMMAND -> abortCommand()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter()
        filter.addAction(BROADCAST_ABORT_COMMAND)
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter)
        setInterruptIfStopped(true)
    }

    override fun onDestroy() {
        logd(TAG, "onDestroy")
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    override fun onHandleWork(intent: Intent) {
        if (isCancelled) return
        if (intent.hasExtra(COMMAND_ID_PARAMETER)) {
            val commandId = intent.getStringExtra(COMMAND_ID_PARAMETER) ?: return
            val toastMessages = intent.getBooleanExtra(TOAST_MESSAGES_PARAMETER, false)
            val command = DataStore.getCommand(commandId)
            val widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            command?.let {
                notification(this)
                if (command.isCritical && intent.action != ACTION_EXECUTE_CONFIRMED) {
                    applicationContext.startActivity(
                        getPopupIntent(
                            applicationContext,
                            widgetId,
                            command.id,
                            toastMessages
                        )
                    )
                } else {
                    broadcastChange(BROADCAST_COMMAND_EXECUTION_STARTED, command.id)
                    executeCommand(command, widgetId, toastMessages)
                }
            }
        }
    }

    private fun executeCommand(command: Command, widgetId: Int, toastMessages: Boolean) {
        val widgetProvider = WidgetProviderFacade.getProvider(applicationContext, widgetId)

        widgetProvider.setUpdatingView(applicationContext, widgetId)

        client = ClientFactory.getClient()

        val host = DataStore.getHost(command.hostId)
        if (host != null) {
            client?.execute(command,
                host,
                onSuccess = { result ->
                    handleResponse(result, command, toastMessages, widgetProvider, widgetId)
                },
                onError = { e ->
                    handleError(e, command, toastMessages, widgetProvider, widgetId)
                })
        }
    }

    private fun abortCommand() {
        isCancelled = true
        CoroutineScope(IO).launch {
            client?.close()
        }
        stopSelf()
        logd(TAG, "abortCommand")
    }

    private fun showResponse(result: String) {
        handler.post {
            longToast(result)
        }
    }

    private fun handleResponse(
        response: String,
        command: Command,
        toastMessages: Boolean,
        widgetProvider: AbstractWidgetProvider,
        widgetId: Int
    ) {
        if (command.doNotNotifyEmptyResponse && TextUtils.isEmpty(response)) {
            broadcastChange(BROADCAST_COMMAND_EXECUTED)
        } else {
            val finalMessage =
                if (TextUtils.isEmpty(response)) getString(R.string.empty_response_message) else response
            if (toastMessages) {
                showResponse(finalMessage)
            } else {
                broadcastChange(BROADCAST_COMMAND_EXECUTED, finalMessage)
            }
        }
        widgetProvider.drawWidget(applicationContext, widgetId, command)
        logd(TAG, "handleResponse")
    }

    private fun handleError(
        exception: Throwable,
        command: Command,
        toastMessages: Boolean,
        widgetProvider: AbstractWidgetProvider,
        widgetId: Int
    ) {
        if (toastMessages) exception.message?.let { showResponse(it) }
        broadcastChange(BROADCAST_COMMAND_FAILED, exception.message)
        widgetProvider.drawWidget(applicationContext, widgetId, command)
        logw(TAG, "handleError $exception.message")
    }

    private fun broadcastChange(action: String, result: String? = null) {
        val intent = Intent(action)
        result?.let {
            intent.putExtra("message", result)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun notification(context: Context) {
        val channelId = "sshing_service"
        val channelName = "SSHing Service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(channelId, channelName)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.drawable.ic_notification_service)
            .setPriority(PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setColor(ResourcesCompat.getColor(resources, R.color.colorPrimaryDark, theme))
            .setContentTitle(getString(R.string.app_name))
            .setContentText(context.getString(R.string.command_executing_message))
            .build()
        val notificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String) {
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
    }

    companion object {

        const val NOTIFICATION_ID = 101

        fun getPopupIntent(
            context: Context,
            widgetId: Int,
            commandId: String,
            toastMessages: Boolean
        ): Intent {
            return Intent(context, CriticalPopupActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(COMMAND_ID_PARAMETER, commandId)
                putExtra(TOAST_MESSAGES_PARAMETER, toastMessages)
            }
        }

        fun getRuntimeIntent(context: Context, commandId: String, toastMessages: Boolean): Intent {
            return Intent(context, WidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                putExtra(COMMAND_ID_PARAMETER, commandId)
                putExtra(TOAST_MESSAGES_PARAMETER, toastMessages)
                action = FORCE_EXECUTE_ACTION
            }
        }

        fun getByPassIntent(
            context: Context,
            widgetId: Int,
            commandId: String,
            toastMessages: Boolean
        ): Intent {
            return Intent(context, WidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(COMMAND_ID_PARAMETER, commandId)
                putExtra(TOAST_MESSAGES_PARAMETER, toastMessages)
                action = ACTION_EXECUTE_CONFIRMED
            }
        }

        fun getWidgetIntent(
            context: Context,
            widgetId: Int,
            commandId: String,
            toastMessages: Boolean = true
        ): Intent {
            return Intent(context, WidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(COMMAND_ID_PARAMETER, commandId)
                putExtra(TOAST_MESSAGES_PARAMETER, toastMessages)
                action = UPDATE_ACTION_EXECUTE
            }
        }

        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, WidgetService::class.java, 1, intent)
        }

        fun stopService(context: Context) {
            LocalBroadcastManager.getInstance(context)
                .sendBroadcast(Intent(BROADCAST_ABORT_COMMAND))
        }

    }

}
