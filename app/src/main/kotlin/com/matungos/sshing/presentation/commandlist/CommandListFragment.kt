package com.matungos.sshing.presentation.commandlist

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialSharedAxis
import com.matungos.sshing.BuildConfig
import com.matungos.sshing.R
import com.matungos.sshing.appwidget.WidgetService
import com.matungos.sshing.databinding.CommandListFragmentBinding
import com.matungos.sshing.extensions.TAG
import com.matungos.sshing.model.Command
import com.matungos.sshing.ui.InsetDecoration
import com.matungos.sshing.ui.ItemTouchHelperViewHolder
import com.matungos.sshing.utils.*
import com.matungos.sshing.utils.LogUtils.logd

class CommandListFragment : Fragment() {

    private lateinit var adapter: CommandListAdapter

    private val viewModel: CommandListViewModel by viewModels()

    private var _binding: CommandListFragmentBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private var handler: Handler? = null

    private var snackbar: Snackbar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        _binding = CommandListFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        handler = Handler(Looper.getMainLooper())

        binding.addCommandFab.setOnClickListener {
            navigateToAddCommand()
        }

        setupRecyclerView()

        viewModel.commandList.observe(viewLifecycleOwner, { list ->
            adapter.submitList(list)
            logd(TAG, "new list received: " + list.joinToString(", ") { it.label })
        })

        viewModel.hostList.observe(viewLifecycleOwner, {
            adapter.notifyDataSetChanged()
        })

        val filter = IntentFilter()
        filter.addAction(BROADCAST_COMMAND_EXECUTION_STARTED)
        filter.addAction(BROADCAST_COMMAND_EXECUTED)
        filter.addAction(BROADCAST_COMMAND_FAILED)

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(broadcastReceiver, filter)

        if (BuildConfig.DEBUG)
            WidgetUtils.updateWidgets(requireContext())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.action_donate_app).isVisible = BuildConfig.IS_FREE_VERSION
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button.
        val id = item.itemId

        if (id == R.id.action_settings) {
            navigateToSettings()
            return true
        }

        if (id == R.id.action_hosts) {
            navigateToHostList()
            return true
        }

        if (id == R.id.action_donate_app) {
            MaterialDialog(requireContext())
                .title(R.string.alert_donate_title)
                .message(R.string.alert_donate_message)
                .icon(R.drawable.ic_shop_white_24)
                .positiveButton(android.R.string.ok) {
                    val appName = "com.matungos.sshing.donate"
                    try {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=$appName")
                            )
                        )

                    } catch (anfe: android.content.ActivityNotFoundException) {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=$appName")
                            )
                        )
                    }
                }
                .cancelOnTouchOutside(false)
                .negativeButton(android.R.string.cancel)
                .show()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadcastReceiver)
        handler = null
        _binding = null
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        adapter = CommandListAdapter(
            object : CommandListAdapter.CommandListAdapterListener {
                override fun onCommandSelected(command: Command) {
                    val intentJob =
                        WidgetService.getRuntimeIntent(requireContext(), command.id, false)
                    WidgetService.enqueueWork(requireContext(), intentJob)
                }

                override fun onEditPressed(position: Int, command: Command, view: View) {
                    onEdit(position, command, view)
                }

                override fun onDuplicatePressed(position: Int, command: Command) {
                    onDuplicate(position, command)
                }

                override fun onDeletePressed(position: Int, command: Command) {
                    onDelete(position, command)
                }
            },
            showMenu = true,
            showDragView = true
        )

        binding.commandsRecyclerview.adapter = adapter
        binding.commandsRecyclerview.setHasFixedSize(true)
        binding.commandsRecyclerview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 5)
                    binding.addCommandFab.hide()
                else if (dy < -5)
                    binding.addCommandFab.show()
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
                viewModel.moveCommand(fromPosition, toPosition)
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
                viewModel.saveCommands()
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.commandsRecyclerview)
        adapter.setItemTouchHelper(itemTouchHelper)
        binding.commandsRecyclerview.addItemDecoration(InsetDecoration(requireContext()))
    }

    private fun onDelete(position: Int, command: Command) {
        logd(TAG, "onDeletePressed $position")
        (binding.commandsRecyclerview.layoutManager as LinearLayoutManager).also {
            val lastVisible = it.findLastCompletelyVisibleItemPosition()
            val itemCount = it.itemCount
            logd(
                TAG,
                "lastVisible $lastVisible itemCount $itemCount"
            )
            if (lastVisible >= itemCount - 1) {
                binding.addCommandFab.show()
            }
        }
        viewModel.deleteCommand(command)
        WidgetUtils.updateWidgets(requireContext())
    }

    private fun onDuplicate(position: Int, command: Command) {
        logd(TAG, "onDuplicatePressed $position")
        viewModel.duplicateCommand(command)
    }

    private fun onEdit(position: Int, command: Command, commandView: View) {
        logd(TAG, "onEditPressed $position")
        navigateToEditCommand(command, commandView)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BROADCAST_COMMAND_EXECUTION_STARTED -> handleCommandExecutionStarted()

                BROADCAST_COMMAND_EXECUTED -> handleCommandExecuted(intent.extras?.getString("message"))

                BROADCAST_COMMAND_FAILED -> handleCommandFailed(intent.extras?.getString("message"))
            }
        }
    }

    private fun handleCommandExecutionStarted() {
        activity?.let { activity ->

            snackbar?.dismiss()
            snackbar = Snackbar.make(
                binding.root,
                R.string.command_executing_message,
                Snackbar.LENGTH_INDEFINITE
            )
            snackbar?.setTextColor(resources.getColor(android.R.color.white))
            snackbar?.let { snackbar ->
                snackbar.setAction(R.string.cancel) { WidgetService.stopService(activity) }
                snackbar.setActionTextColor(Color.WHITE)
                val snackbarView = snackbar.view
                snackbarView.setBackgroundColor(
                    ResourcesCompat.getColor(resources, R.color.colorPrimaryDark, activity.theme)
                )
                snackbar.show()
            }

        }
    }

    private fun handleCommandFailed(stringMessage: String?) {
        activity?.let { activity ->

            snackbar?.dismiss()
            val message = stringMessage ?: "Failed to execute the command"
            UIUtils.showError(binding.root, message, activity)

        }
    }

    private fun handleCommandExecuted(stringMessage: String?) {
        activity?.let { activity ->

            snackbar?.dismiss()
            stringMessage?.let { message ->
                if (message.count { "\n".contains(it) || "\t".contains(it) } > 2) {
                    MaterialDialog(activity).show {
                        message(text = message)
                        positiveButton(R.string.ok)
                    }
                } else {
                    snackbar = Snackbar.make(
                        binding.root,
                        message,
                        Snackbar.LENGTH_LONG
                    )
                    snackbar?.setTextColor(resources.getColor(android.R.color.white))
                    snackbar?.let { snackbar ->
                        val snackbarView = snackbar.view
                        snackbarView.setBackgroundColor(
                            ResourcesCompat.getColor(
                                resources,
                                R.color.colorPrimaryDark,
                                activity.theme
                            )
                        )
                        snackbar.show()
                    }
                }
            }

        }
    }

    private fun navigateToAddCommand() {
        exitTransition = MaterialElevationScale(false).apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }
        reenterTransition = MaterialElevationScale(true).apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }

        val transitionName = getString(R.string.add_command_detail_transition_name)
        val extras = FragmentNavigatorExtras(binding.addCommandFab to transitionName)

        val action = CommandListFragmentDirections.actionHomeDestToAddCommandDest()
        findNavController().navigate(action, extras)
    }

    private fun navigateToEditCommand(command: Command, commandView: View) {
        exitTransition = MaterialElevationScale(false).apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }
        reenterTransition = MaterialElevationScale(true).apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }

        val transitionName = getString(R.string.add_command_detail_transition_name)
        val extras = FragmentNavigatorExtras(commandView to transitionName)

        val action = CommandListFragmentDirections.actionHomeDestToAddCommandDest(command.id)
        findNavController().navigate(action, extras)
    }

    private fun navigateToHostList() {
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }

        val action = CommandListFragmentDirections.actionHomeDestToHostListDest()
        findNavController().navigate(action)
    }

    private fun navigateToSettings() {
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }

        val action = CommandListFragmentDirections.actionHomeDestToSettingsDest()
        findNavController().navigate(action)
    }

}
