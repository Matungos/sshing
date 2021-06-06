package com.matungos.sshing.utils

import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.snackbar.Snackbar
import com.matungos.sshing.R

object UIUtils {

    fun showError(view: View, message: String, context: Context) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        snackbar.setAction(R.string.ok) { }
        snackbar.setActionTextColor(Color.WHITE)
        snackbar.setTextColor(Color.WHITE)
        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(
            ResourcesCompat.getColor(
                context.resources,
                R.color.colorAlert,
                context.theme
            )
        )
        snackbar.show()
    }

}
