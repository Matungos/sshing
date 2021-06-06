package com.matungos.sshing.appwidget

import com.google.gson.annotations.SerializedName

class WidgetState {

    enum class WidgetType { SMALL, MEDIUM }

    @SerializedName("widgetId")
    var widgetId: Int = 0

    @SerializedName("command_id")
    lateinit var commandId: String

    @SerializedName("time")
    var time: String? = null

    @SerializedName("type")
    var type: WidgetType = WidgetType.MEDIUM

    private constructor()

    constructor(mAppWidgetId: Int, commandId: String, widgetType: WidgetType) {
        this.widgetId = mAppWidgetId
        this.commandId = commandId
        this.type = widgetType
    }
}