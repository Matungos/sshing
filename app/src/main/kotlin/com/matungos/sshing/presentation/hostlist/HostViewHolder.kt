package com.matungos.sshing.presentation.hostlist

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.matungos.sshing.R
import com.matungos.sshing.databinding.HostListItemBinding
import com.matungos.sshing.model.Host
import com.matungos.sshing.ui.ItemTouchHelperViewHolder

/**
 * Created by Gabriel on 03/05/2021.
 */
@SuppressLint("ClickableViewAccessibility")
class HostViewHolder(
    private val binding: HostListItemBinding,
    hostListAdapterListener: HostListAdapter.HostListAdapterListener,
    itemTouchHelper: ItemTouchHelper
) : RecyclerView.ViewHolder(binding.root), ItemTouchHelperViewHolder {

    init {
        val holder = this

        binding.dragView.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                itemTouchHelper.startDrag(holder)
            }
            false
        }

        binding.menu.setOnClickListener { view ->
            binding.host?.let { host ->
                val popup = PopupMenu(view.context, view)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_edit -> {
                            hostListAdapterListener.onEditPressed(
                                holder.bindingAdapterPosition,
                                host,
                                binding.root
                            )
                            true
                        }
                        R.id.action_duplicate -> {
                            hostListAdapterListener.onDuplicatePressed(
                                holder.bindingAdapterPosition,
                                host
                            )
                            true
                        }
                        R.id.action_delete -> {
                            hostListAdapterListener.onDeletePressed(
                                holder.bindingAdapterPosition,
                                host
                            )
                            true
                        }
                        else -> false
                    }
                }
                val inflater = popup.menuInflater
                inflater.inflate(R.menu.menu_host_item, popup.menu)
                popup.show()
            }
        }

//        itemView.setOnClickListener {
//            binding.host?.let { host ->
//                hostListAdapterListener.onHostSelected(host)
//            }
//        }
    }

    override fun onItemSelected() {
        binding.dragView.isSelected = true
        itemView.alpha = ALPHA_DRAGGING
    }

    override fun onItemClear() {
        binding.dragView.isSelected = false
        itemView.alpha = ALPHA_FULL
    }

    fun bind(host: Host) {
        binding.host = host
    }

    companion object {

        internal const val ALPHA_FULL = 1.0f

        internal const val ALPHA_DRAGGING = .8f

    }

}
