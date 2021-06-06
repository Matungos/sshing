package com.matungos.sshing.presentation

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.matungos.sshing.R
import com.matungos.sshing.appwidget.WidgetService
import com.matungos.sshing.data.DataStore
import com.matungos.sshing.utils.COMMAND_ID_PARAMETER
import com.matungos.sshing.utils.TOAST_MESSAGES_PARAMETER

class CriticalPopupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent != null && intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID) && intent.hasExtra(
                COMMAND_ID_PARAMETER
            )
        ) {
            val widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            val toastMessages = intent.getBooleanExtra(TOAST_MESSAGES_PARAMETER, false)
            val commandId = intent.getStringExtra(COMMAND_ID_PARAMETER)
            commandId?.let { DataStore.getCommand(it) }?.let { command ->
                MaterialDialog(this@CriticalPopupActivity)
                    .message(
                        text = String.format(
                            getString(R.string.alert_confirmation_message),
                            command.label
                        )
                    )
                    .positiveButton(android.R.string.ok) {
                        // startService bypassing critical check
                        val intentJob =
                            WidgetService.getByPassIntent(this, widgetId, commandId, toastMessages)
                        WidgetService.enqueueWork(this, intentJob)
                    }
                    .cancelOnTouchOutside(false)
                    .negativeButton(android.R.string.cancel)
                    .onDismiss {
                        finish()
                    }
                    .show()
            }
        }
    }

}
