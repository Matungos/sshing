package com.matungos.sshing.presentation.commandlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.matungos.sshing.databinding.CommandListItemBinding
import com.matungos.sshing.model.Command
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by Gabriel on 05/05/2021.
 */
class CommandListAdapter(
    private val listener: CommandListAdapterListener,
    private val showMenu: Boolean,
    private val showDragView: Boolean
) : RecyclerView.Adapter<CommandViewHolder>() {

    interface CommandListAdapterListener {
        fun onCommandSelected(command: Command)
        fun onEditPressed(position: Int, command: Command, view: View)
        fun onDuplicatePressed(position: Int, command: Command)
        fun onDeletePressed(position: Int, command: Command)
    }

    private var itemTouchHelper: ItemTouchHelper? = null

    var items: ArrayList<Command> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommandViewHolder {
        val binding =
            CommandListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommandViewHolder(binding, listener, itemTouchHelper, showMenu, showDragView)
    }

    override fun onBindViewHolder(holder: CommandViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    private fun getItem(position: Int): Command {
        return items[position]
    }

    fun setItemTouchHelper(itemTouchHelper: ItemTouchHelper) {
        this.itemTouchHelper = itemTouchHelper
    }

    fun submitList(list: List<Command>) {
        val oldList = items
        items = ArrayList(list)
        val diffResult = DiffUtil.calculateDiff(CommandDiffCallback(oldList, list))
        diffResult.dispatchUpdatesTo(this)
    }

    fun moveItems(fromPosition: Int, toPosition: Int) {
        Collections.swap(items, fromPosition, toPosition)
    }

}
