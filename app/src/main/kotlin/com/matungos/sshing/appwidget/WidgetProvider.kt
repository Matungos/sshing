package com.matungos.sshing.appwidget

import com.matungos.sshing.R

class WidgetProvider : AbstractWidgetProvider() {

    override fun getLayout(): Int {
        return R.layout.appwidget_layout
    }

    override fun getMainViewId(): Int {
        return R.id.appwidget_mainview_medium
    }

    override fun getProviderClass() = WidgetProvider::class.java

}