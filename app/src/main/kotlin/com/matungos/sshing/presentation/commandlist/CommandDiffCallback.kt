package com.matungos.sshing.presentation.commandlist

import androidx.recyclerview.widget.DiffUtil
import com.matungos.sshing.model.Command

class CommandDiffCallback(
    private val oldList: ArrayList<Command>,
    private val newList: List<Command>
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
                oldItem.hostId == newItem.hostId &&
                oldItem.label == newItem.label &&
                oldItem.color == newItem.color
    }

}
