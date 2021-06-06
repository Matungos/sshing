package com.matungos.sshing.utils

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import com.matungos.sshing.R
import com.matungos.sshing.appwidget.WidgetProvider
import com.matungos.sshing.appwidget.WidgetProviderSmall

object WidgetUtils {

    fun updateWidgets(context: Context) {
        context.apply {
            val intentMedium = Intent(this, WidgetProvider::class.java)
            intentMedium.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val idsMedium = intArrayOf(R.id.appwidget_mainview_medium)
            intentMedium.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idsMedium)
            sendBroadcast(intentMedium)

            val intentSmall = Intent(this, WidgetProviderSmall::class.java)
            intentSmall.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val idsSmall = intArrayOf(R.id.appwidget_mainview_small) // medium?
            intentSmall.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idsSmall)
            sendBroadcast(intentSmall)
        }
    }

}
