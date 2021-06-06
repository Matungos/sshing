package com.matungos.sshing.presentation.commandlist

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.matungos.sshing.R
import com.matungos.sshing.data.DataStore
import com.matungos.sshing.databinding.CommandListItemBinding
import com.matungos.sshing.extensions.getContrastColor
import com.matungos.sshing.model.Command
import com.matungos.sshing.ui.ItemTouchHelperViewHolder

/**
 * Created by Gabriel on 03/05/2021.
 */
@SuppressLint("ClickableViewAccessibility")
class CommandViewHolder(
    private val binding: CommandListItemBinding,
    private val listener: CommandListAdapter.CommandListAdapterListener,
    private val itemTouchHelper: ItemTouchHelper?,
    private val showMenu: Boolean,
    private val showDragView: Boolean
) : RecyclerView.ViewHolder(binding.root), ItemTouchHelperViewHolder {

    init {
        val holder = this

        binding.dragView.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                itemTouchHelper?.startDrag(holder)
            }
            false
        }

        binding.menu.setOnClickListener { view ->
            binding.command?.let { command ->
                val popup = PopupMenu(view.context, view)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_edit -> {
                            listener.onEditPressed(
                                holder.bindingAdapterPosition,
                                command,
                                binding.root
                            )
                            true
                        }
                        R.id.action_duplicate -> {
                            listener.onDuplicatePressed(
                                holder.bindingAdapterPosition,
                                command
                            )
                            true
                        }
                        R.id.action_delete -> {
                            listener.onDeletePressed(
                                holder.bindingAdapterPosition,
                                command
                            )
                            true
                        }
                        else -> false
                    }
                }
                val inflater = popup.menuInflater
                inflater.inflate(R.menu.menu_command_item, popup.menu)
                popup.show()
            }
        }

    }

    fun bind(command: Command) {
        binding.command = command
        binding.listener = listener
        binding.dragView.visibility = if (showDragView) View.VISIBLE else View.INVISIBLE
        binding.menu.visibility = if (showMenu) View.VISIBLE else View.INVISIBLE

        binding.hostTextview.text = DataStore.getHost(command.hostId)?.label ?: ""

        binding.commandLabelTextview.setTextColor(command.color.getContrastColor())
        (itemView as MaterialCardView).setCardBackgroundColor(command.color)
    }

    override fun onItemSelected() {
        binding.dragView.isSelected = true
        itemView.alpha = ALPHA_DRAGGING
    }

    override fun onItemClear() {
        binding.dragView.isSelected = false
        itemView.alpha = ALPHA_FULL
    }

    companion object {

        private const val ALPHA_FULL = 1.0f
        private const val ALPHA_DRAGGING = .8f

    }

}
