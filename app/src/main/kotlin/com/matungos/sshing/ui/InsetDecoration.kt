package com.matungos.sshing.ui

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.matungos.sshing.R

/**
 * ItemDecoration implementation that applies an inset margin
 * around each child of the RecyclerView. The inset value is controlled
 * by a dimension resource.
 */
class InsetDecoration(context: Context) : RecyclerView.ItemDecoration() {
    private val mInsets: Int = context.resources.getDimensionPixelSize(R.dimen.card_insets)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.set(0, 0, 0, mInsets)
    }
}
