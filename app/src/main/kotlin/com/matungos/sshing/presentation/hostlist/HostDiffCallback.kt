package com.matungos.sshing.presentation.hostlist

import androidx.recyclerview.widget.DiffUtil
import com.matungos.sshing.model.Host

class HostDiffCallback(
    private val oldList: ArrayList<Host>,
    private val newList: List<Host>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return newList[newItemPosition].id == oldList[oldItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem.id == newItem.id &&
                oldItem.port == newItem.port &&
                oldItem.label == newItem.label &&
                oldItem.address == newItem.address
    }

}
