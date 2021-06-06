package com.matungos.sshing.appwidget

import com.matungos.sshing.appwidget.WidgetState.WidgetType

class WidgetMediumConfigActivity : AbstractWidgetConfigActivity() {

    override fun getType(): WidgetType {
        return WidgetType.MEDIUM
    }

}