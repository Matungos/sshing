package com.matungos.sshing.appwidget

import android.content.Context

object WidgetProviderFacade {

    fun getProvider(context: Context, widgetId: Int): AbstractWidgetProvider {
        val wm = WidgetsManager(context)
        val ws = wm.getConfig(widgetId)
        if (ws?.type == WidgetState.WidgetType.SMALL)
            return WidgetProviderSmall()
        return WidgetProvider()
    }

}