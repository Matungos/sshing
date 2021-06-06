package com.matungos.sshing.presentation.addcommand

import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.color.ColorPalette
import com.afollestad.materialdialogs.color.colorChooser
import com.afollestad.materialdialogs.list.listItems
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialElevationScale
import com.matungos.sshing.R
import com.matungos.sshing.data.DataStore
import com.matungos.sshing.databinding.AddCommandFragmentBinding
import com.matungos.sshing.extensions.shortToast
import com.matungos.sshing.extensions.themeColor
import com.matungos.sshing.model.Command
import com.matungos.sshing.model.Host
import com.matungos.sshing.utils.HOST_ID_PARAMETER
import com.matungos.sshing.utils.HOST_REQUEST_KEY
import java.util.*

/**
 * Created by Gabriel on 02/05/2021.
 */
class AddCommandFragment : Fragment() {

    private val viewModel: AddCommandViewModel by viewModels()

    private val args: AddCommandFragmentArgs by navArgs()

    private var _binding: AddCommandFragmentBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private var hostList: List<Host> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(requireContext().themeColor(android.R.attr.windowBackground))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        _binding = AddCommandFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.command = Command().apply {
            color = ResourcesCompat.getColor(
                resources,
                R.color.command_background_color,
                requireActivity().theme
            )
        }

        binding.addHostFab.setOnClickListener {
            navigateToAddHost()
        }

        setFragmentResultListener(HOST_REQUEST_KEY) { _: String, bundle: Bundle ->
            val hostId = bundle.getString(HOST_ID_PARAMETER)
            hostList.find { item -> item.id == hostId }?.let {
                onHostSelected(it)
            }
        }

        binding.selectColorButton.setOnClickListener { colorChooserOnClick() }
        binding.colorFab.setOnClickListener { colorChooserOnClick() }

        binding.selectHostButton.setOnClickListener {
            if (hostList.isEmpty()) {
                MaterialDialog(requireActivity())
                    .message(text = "Add your first host")
                    .negativeButton(android.R.string.cancel)
                    .positiveButton(text = "ADD") {
                        navigateToAddHost()
                    }
                    .show()
            } else {
                MaterialDialog(requireActivity()).show {
                    listItems(items = hostList.map { it.label }) { _, index, _ ->
                        // Invoked when the user selects an item
                        onHostSelected(hostList[index])
                    }
                }
            }
        }

        hostList = ArrayList(DataStore.hostList)

        if (!TextUtils.isEmpty(args.commandId)) {
            DataStore.getCommand(args.commandId)?.let { command ->
                viewModel.command = command
                viewModel.host = hostList.find { item -> item.id == viewModel.command.hostId }
                (requireActivity() as AppCompatActivity).supportActionBar?.title =
                    getString(R.string.add_command_edition_title)
            }
        }

        DataStore.hostListLiveData.observe(viewLifecycleOwner, {
            hostList = it
        })

        viewModel.getStatus().observe(viewLifecycleOwner, { handleStatus(it) })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.done_only_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_done) {
            viewModel.saveAction()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun handleStatus(status: AddCommandStatus?) {
        when (status) {
            AddCommandStatus.EMPTY_LABEL -> {
                binding.commandLabelTextInputLayout.error =
                    getString(R.string.empty_form_field_error)
                binding.commandLabelEditText.requestFocus()
            }
            AddCommandStatus.LABEL_OK -> {
                binding.commandLabelTextInputLayout.error = null
            }
            AddCommandStatus.EMPTY_COMMAND -> {
                binding.commandStringTextInputLayout.error =
                    getString(R.string.empty_form_field_error)
                binding.commandStringEditText.requestFocus()
            }
            AddCommandStatus.COMMAND_OK -> {
                binding.commandStringTextInputLayout.error = null
            }
            AddCommandStatus.HOST_NOT_SELECTED -> {
                val snackbar = Snackbar.make(
                    binding.root,
                    R.string.add_command_select_host_text,
                    Snackbar.LENGTH_LONG
                )
                snackbar.setAction(R.string.ok) { }
                snackbar.setTextColor(Color.WHITE)
                snackbar.setActionTextColor(Color.WHITE)
                val snackbarView = snackbar.view
                snackbarView.setBackgroundColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.colorAlert,
                        requireActivity().theme
                    )
                )
                snackbar.show()
            }
            AddCommandStatus.COMMAND_CREATED -> {
                findNavController().navigateUp()
            }
            else -> context?.shortToast("Something went wrong, please try again!")
        }
    }

    private fun onHostSelected(host: Host) {
        viewModel.host = host
    }

    private fun colorChooserOnClick() {
        MaterialDialog(requireActivity()).show {
            title(R.string.colors)
            colorChooser(
                ColorPalette.Primary,
                subColors = ColorPalette.PrimarySub,
                allowCustomArgb = true
            ) { _, color ->
                // Use color integer
                onColorSelected(color)
            }
            positiveButton(R.string.ok)
        }
    }

    private fun onColorSelected(color: Int) {
        viewModel.setColor(color)
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

        val action = AddCommandFragmentDirections.actionAddCommandDestToAddHostDest()
        findNavController().navigate(action, extras)
    }

}
