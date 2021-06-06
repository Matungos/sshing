package com.matungos.sshing.appwidget

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class WidgetsManager(context: Context) {

    private val gson: Gson = Gson()

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val allWidgetsIds: HashSet<Int>
        get() {
            val widgetString = prefs.getString(ALL_WIDGETS_IDS, "[]")
            val collectionType = object : TypeToken<HashSet<Int>>() {}.type
            return gson.fromJson(widgetString, collectionType)
        }

    fun getConfig(widgetId: Int): WidgetState? {
        val widgetString = prefs.getString(WIDGET_STATE + widgetId, null)
        if (widgetString != null) {
            val collectionType = object : TypeToken<WidgetState>() {}.type
            return gson.fromJson<WidgetState>(widgetString, collectionType)
        }
        return null
    }

    fun saveConfig(ws: WidgetState) {
        val collectionType = object : TypeToken<WidgetState>() {}.type
        val state = gson.toJson(ws, collectionType)
        val editor = prefs.edit()
        editor.putString(WIDGET_STATE + ws.widgetId, state)
        editor.apply()

        val ids = allWidgetsIds
        if (!ids.contains(ws.widgetId)) {
            ids.add(ws.widgetId)
            saveAllIds(ids)
        }
    }

    fun deleteState(widgetId: Int) {
        val preferencesEditor = prefs.edit()
        preferencesEditor.remove(WIDGET_STATE + widgetId)
        preferencesEditor.apply()

        val ids = allWidgetsIds
        if (ids.contains(widgetId)) {
            ids.remove(widgetId)
            saveAllIds(ids)
        }
    }

    private fun saveAllIds(ids: HashSet<Int>) {
        val collectionType = object : TypeToken<HashSet<Int>>() {}.type
        val state = gson.toJson(ids, collectionType)
        val editor = prefs.edit()
        editor.putString(ALL_WIDGETS_IDS, state)
        editor.apply()
    }

    companion object {

        private const val ALL_WIDGETS_IDS = "ALL_WIDGETS_IDS"

        private const val WIDGET_STATE = "WIDGET_STATE_"

    }

}
