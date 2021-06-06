package com.matungos.sshing.appwidget

import com.matungos.sshing.R

class WidgetProviderSmall : AbstractWidgetProvider() {

    override fun getLayout(): Int {
        return R.layout.appwidget_layout_small
    }

    override fun getMainViewId(): Int {
        return R.id.appwidget_mainview_small
    }

    override fun getProviderClass() = WidgetProviderSmall::class.java

}