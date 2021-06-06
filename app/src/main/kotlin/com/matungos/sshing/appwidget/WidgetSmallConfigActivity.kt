package com.matungos.sshing.appwidget

import com.matungos.sshing.appwidget.WidgetState.WidgetType

class WidgetSmallConfigActivity : AbstractWidgetConfigActivity() {

    override fun getType(): WidgetType {
        return WidgetType.SMALL
    }

}