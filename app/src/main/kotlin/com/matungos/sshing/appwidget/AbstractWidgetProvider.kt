package com.matungos.sshing.appwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.matungos.sshing.R
import com.matungos.sshing.data.DataStore
import com.matungos.sshing.extensions.TAG
import com.matungos.sshing.extensions.getContrastColor
import com.matungos.sshing.model.Command
import com.matungos.sshing.utils.COMMAND_ID_PARAMETER
import com.matungos.sshing.utils.LogUtils.logd

abstract class AbstractWidgetProvider : AppWidgetProvider() {

    abstract fun getLayout(): Int

    abstract fun getProviderClass(): Class<*>

    abstract fun getMainViewId(): Int

    fun setUpdatingView(applicationContext: Context?, widgetId: Int) {
        if (applicationContext == null) return
        val remoteViews = RemoteViews(applicationContext.packageName, getLayout())
        remoteViews.setViewVisibility(R.id.appwidget_working_pb, View.VISIBLE)

        // disable click
        remoteViews.setBoolean(getMainViewId(), "setEnabled", false)

        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        appWidgetManager.updateAppWidget(widgetId, remoteViews)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            // Build the intent to call the service
            val wm = WidgetsManager(context)
            wm.deleteState(appWidgetId)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        logd(TAG, "onUpdate")
        val thisWidget = ComponentName(context, getProviderClass())
        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

        for (appWidgetId in allWidgetIds) {
            drawWidget(context, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val extras = intent.extras
        logd(TAG, "onReceive action: $action")

        val appWidgetId = extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        if (action == UPDATE_ACTION_EXECUTE && appWidgetId != null) {
            val commandId = extras.getString(COMMAND_ID_PARAMETER)
            logd(TAG, "appWidgetId: $appWidgetId")
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && commandId != null) {
                val intentJob = WidgetService.getWidgetIntent(context, appWidgetId, commandId)
                WidgetService.enqueueWork(context, intentJob)
            } else {
                drawWidget(context, appWidgetId)
            }
        } else if (action == AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED && appWidgetId != null) {
            drawWidget(context, appWidgetId)
        } else {
            super.onReceive(context, intent)
        }
    }

    fun drawWidget(applicationContext: Context?, widgetId: Int, command: Command) {
        if (applicationContext == null) return
        val remoteViews = RemoteViews(applicationContext.packageName, getLayout())
        remoteViews.setTextViewText(R.id.command_label_textview, command.label)
        remoteViews.setTextViewText(
            R.id.host_textview,
            DataStore.getHost(command.hostId)?.label ?: ""
        )
        remoteViews.setViewVisibility(R.id.appwidget_working_pb, View.GONE)

        remoteViews.setInt(
            R.id.command_label_textview,
            "setTextColor",
            command.color.getContrastColor()
        )
        remoteViews.setInt(R.id.appwidget_background, "setColorFilter", command.color)

        // enable click
        remoteViews.setBoolean(getMainViewId(), "setEnabled", true)

        initActions(applicationContext, widgetId, command, remoteViews)

        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        appWidgetManager.updateAppWidget(widgetId, remoteViews)
    }

    private fun initActions(
        context: Context,
        appWidgetId: Int,
        command: Command,
        remoteViews: RemoteViews
    ) {
        val clickIntent = Intent(context, WidgetProvider::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(COMMAND_ID_PARAMETER, command.id)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            action = UPDATE_ACTION_EXECUTE
        }
        val pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, clickIntent, 0)
        remoteViews.setOnClickPendingIntent(getMainViewId(), pendingIntent)
        remoteViews.setViewVisibility(R.id.config, View.GONE)
    }

    companion object {

        const val FORCE_UPDATE_ACTION = "com.matungos.sshing.actions.FORCE_UPDATE_ACTION"
        const val FORCE_EXECUTE_ACTION = "com.matungos.sshing.actions.FORCE_EXECUTE_ACTION"
        const val UPDATE_ACTION = "com.matungos.sshing.actions.UPDATE_ACTION"
        const val UPDATE_ACTION_EXECUTE = "com.matungos.sshing.actions.UPDATE_ACTION_EXECUTE"
        const val UPDATE_ACTION_LOADING_IMAGE =
            "com.matungos.sshing.actions.UPDATE_ACTION_LOADING_IMAGE"

        fun drawWidget(context: Context, widgetId: Int) {
            val wm = WidgetsManager(context)
            val ws = wm.getConfig(widgetId)

            val command = if (ws != null) DataStore.getCommand(ws.commandId) else null

            if (command != null) {
                val widgetProvider = WidgetProviderFacade.getProvider(context, widgetId)
                widgetProvider.drawWidget(context, widgetId, command)
                logd(TAG, "drawWidget with state widgetId: $widgetId")
            } else {
                logd(TAG, "drawWidget without state widgetId: $widgetId")
                val remoteViews =
                    RemoteViews(context.packageName, R.layout.appwidget_layout_undefined)

                var activityClass: Class<*> = WidgetMediumConfigActivity::class.java
                if (ws?.type == WidgetState.WidgetType.SMALL) {
                    activityClass = WidgetSmallConfigActivity::class.java
                }

                val configIntent = Intent(context, activityClass)

                configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                configIntent.data = Uri.parse(configIntent.toUri(Intent.URI_INTENT_SCHEME))
                val configPendingIntent = PendingIntent.getActivity(context, 0, configIntent, 0)
                remoteViews.setOnClickPendingIntent(R.id.config, configPendingIntent)

                val appWidgetManager = AppWidgetManager.getInstance(context)
                appWidgetManager.updateAppWidget(widgetId, remoteViews)
            }
        }
    }

}
