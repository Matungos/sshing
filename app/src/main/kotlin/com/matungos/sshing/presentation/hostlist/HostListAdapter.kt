package com.matungos.sshing.presentation.hostlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.matungos.sshing.databinding.HostListItemBinding
import com.matungos.sshing.model.Host
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by Gabriel on 05/05/2021.
 */
class HostListAdapter(
    private val listener: HostListAdapterListener
) : RecyclerView.Adapter<HostViewHolder>() {

    interface HostListAdapterListener {
        fun onHostSelected(host: Host)
        fun onEditPressed(position: Int, host: Host, view: View)
        fun onDuplicatePressed(position: Int, host: Host)
        fun onDeletePressed(position: Int, host: Host)
    }

    private lateinit var itemTouchHelper: ItemTouchHelper

    var items: ArrayList<Host> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HostViewHolder {
        val binding =
            HostListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HostViewHolder(binding, listener, itemTouchHelper)
    }

    override fun onBindViewHolder(holder: HostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    private fun getItem(position: Int): Host {
        return items[position]
    }

    fun setItemTouchHelper(itemTouchHelper: ItemTouchHelper) {
        this.itemTouchHelper = itemTouchHelper
    }


    fun submitList(list: List<Host>) {
        val oldList = items
        items = ArrayList(list)
        val diffResult = DiffUtil.calculateDiff(HostDiffCallback(oldList, list))
        diffResult.dispatchUpdatesTo(this)
    }

    fun moveItems(fromPosition: Int, toPosition: Int) {
        Collections.swap(items, fromPosition, toPosition)
    }

}
