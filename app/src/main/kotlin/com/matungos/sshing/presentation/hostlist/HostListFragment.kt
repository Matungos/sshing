package com.matungos.sshing.presentation.hostlist

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialSharedAxis
import com.matungos.sshing.R
import com.matungos.sshing.data.DataStore
import com.matungos.sshing.databinding.HostListFragmentBinding
import com.matungos.sshing.extensions.TAG
import com.matungos.sshing.model.Command
import com.matungos.sshing.model.Host
import com.matungos.sshing.ui.InsetDecoration
import com.matungos.sshing.ui.ItemTouchHelperViewHolder
import com.matungos.sshing.utils.LogUtils.logd
import com.matungos.sshing.utils.UIUtils

/**
 * Created by Gabriel on 02/05/2021.
 */
class HostListFragment : Fragment() {

    private lateinit var adapter: HostListAdapter

    private val viewModel: HostListViewModel by viewModels()

    private var _binding: HostListFragmentBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private var handler: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = HostListFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        handler = Handler(Looper.getMainLooper())

        binding.addHostFab.setOnClickListener {
            navigateToAddHost()
        }

        setupRecyclerView()

        viewModel.hostList.observe(viewLifecycleOwner, {
            adapter.submitList(it)
        })
    }

    override fun onDestroyView() {
        handler = null
        _binding = null
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        adapter = HostListAdapter(object : HostListAdapter.HostListAdapterListener {
            override fun onHostSelected(host: Host) {
                // nop
            }

            override fun onEditPressed(position: Int, host: Host, view: View) {
                onEdit(position, host, view)
            }

            override fun onDuplicatePressed(position: Int, host: Host) {
                onDuplicate(position, host)
            }

            override fun onDeletePressed(position: Int, host: Host) {
                onDelete(position, host)
            }
        })
        binding.hostsRecyclerview.adapter = adapter
        binding.hostsRecyclerview.setHasFixedSize(true)
        binding.hostsRecyclerview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 5)
                    binding.addHostFab.hide()
                else if (dy < -5)
                    binding.addHostFab.show()
            }
        })
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.RIGHT
        ) {
            override fun isLongPressDragEnabled(): Boolean {
                return false
            }

            override fun isItemViewSwipeEnabled(): Boolean {
                return false
            }

            override fun onMove(
                recyclerView: RecyclerView,
                source: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = source.bindingAdapterPosition
                val toPosition = target.bindingAdapterPosition
                viewModel.moveHost(fromPosition, toPosition)
                adapter.moveItems(fromPosition, toPosition)
                adapter.notifyItemMoved(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                // We only want the active item to change
                if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                    if (viewHolder is ItemTouchHelperViewHolder) {
                        // Let the view holder know that this item is being moved or dragged
                        val itemViewHolder = viewHolder as ItemTouchHelperViewHolder
                        itemViewHolder.onItemSelected()
                    }
                }
                super.onSelectedChanged(viewHolder, actionState)
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                if (viewHolder is ItemTouchHelperViewHolder) {
                    // Tell the view holder it's time to restore the idle state
                    val itemViewHolder = viewHolder as ItemTouchHelperViewHolder
                    itemViewHolder.onItemClear()
                }
                viewModel.saveHosts()
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.hostsRecyclerview)
        adapter.setItemTouchHelper(itemTouchHelper)
        binding.hostsRecyclerview.addItemDecoration(InsetDecoration(requireContext()))
    }

    private fun onDelete(position: Int, host: Host) {
        logd(TAG, "onDeletePressed $position")
        val commands = getCommandsWithHost(host)
        if (commands.isNotEmpty()) {
            UIUtils.showError(
                binding.root,
                getString(R.string.delete_host_error),
                requireActivity()
            )
        } else {
            (binding.hostsRecyclerview.layoutManager as LinearLayoutManager).also {
                val lastVisible = it.findLastCompletelyVisibleItemPosition()
                val itemCount = it.itemCount
                logd(
                    TAG,
                    "lastVisible $lastVisible itemCount $itemCount"
                )
                if (lastVisible >= itemCount - 1) {
                    binding.addHostFab.show()
                }
            }
            viewModel.deleteHost(host)
        }
    }

    private fun getCommandsWithHost(host: Host): List<Command> {
        return DataStore.commandList.filter { command -> command.hostId == host.id }
    }

    private fun onDuplicate(position: Int, host: Host) {
        logd(TAG, "onDuplicatePressed $position")
        viewModel.duplicateHost(host)
    }

    private fun onEdit(position: Int, host: Host, hostView: View) {
        logd(TAG, "onEditPressed $position")
        navigateToEditHost(host, hostView)
    }

    private fun navigateToAddHost() {
        exitTransition = MaterialElevationScale(false).apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }
        reenterTransition = MaterialElevationScale(true).apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }

        val transitionName = getString(R.string.add_host_detail_transition_name)
        val extras = FragmentNavigatorExtras(binding.addHostFab to transitionName)

        val action = HostListFragmentDirections.actionHostListDestToAddHostDest()
        findNavController().navigate(action, extras)
    }

    private fun navigateToEditHost(host: Host, hostView: View) {
        exitTransition = MaterialElevationScale(false).apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }
        reenterTransition = MaterialElevationScale(true).apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }

        val transitionName = getString(R.string.add_host_detail_transition_name)
        val extras = FragmentNavigatorExtras(hostView to transitionName)

        val action = HostListFragmentDirections.actionHostListDestToAddHostDest(host.id)
        findNavController().navigate(action, extras)
    }

}
