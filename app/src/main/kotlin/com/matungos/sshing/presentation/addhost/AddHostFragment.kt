package com.matungos.sshing.presentation.addhost

import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.transition.MaterialContainerTransform
import com.matungos.sshing.R
import com.matungos.sshing.data.DataStore
import com.matungos.sshing.databinding.AddHostFragmentBinding
import com.matungos.sshing.extensions.shortToast
import com.matungos.sshing.extensions.themeColor
import com.matungos.sshing.model.Host
import com.matungos.sshing.model.Identity
import com.matungos.sshing.utils.HOST_ID_PARAMETER
import com.matungos.sshing.utils.HOST_REQUEST_KEY
import com.matungos.sshing.utils.UIUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Gabriel on 02/05/2021.
 */
class AddHostFragment : Fragment() {

    private val viewModel: AddHostViewModel by viewModels()

    private val args: AddHostFragmentArgs by navArgs()

    private var _binding: AddHostFragmentBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

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
        _binding = AddHostFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.chooseKeyButton.setOnClickListener { actionReadKey.launch(arrayOf("*/*")) }

        viewModel.getStatus().observe(viewLifecycleOwner, { handleStatus(it) })

        viewModel.getKeyStatus().observe(viewLifecycleOwner, { onKeySelected(it) })

        if (!TextUtils.isEmpty(args.hostId)) {
            DataStore.getHost(args.hostId)?.let { host ->
                viewModel.host = host
                (requireActivity() as AppCompatActivity).supportActionBar?.title =
                    getString(R.string.add_host_edition_title)
            }
        } else {
            viewModel.host = Host(Identity())
        }
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

    private val actionReadKey =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val documentUri = uri ?: return@registerForActivityResult

            val documentFile = DocumentFile.fromSingleUri(requireContext(), documentUri)
                ?: return@registerForActivityResult

            val documentName = documentFile.name ?: return@registerForActivityResult

            lifecycleScope.launch {
                @Suppress("BlockingMethodInNonBlockingContext")
                val documentStream = withContext(Dispatchers.IO) {
                    requireActivity().contentResolver.openInputStream(documentFile.uri)
                } ?: return@launch

                viewModel.readKey(documentStream, documentName)
            }
        }

    private fun onKeySelected(keyStatus: KeyStatus) {
        when (keyStatus) {
            is KeyStatus.SuccessKeyRead -> {
                viewModel.setKey(keyStatus)
            }
        }
    }

    private fun handleStatus(addHostStatus: AddHostStatus?) {
        when (addHostStatus) {
            AddHostStatus.EMPTY_LABEL -> {
                binding.labelEditText.requestFocus()
                binding.labelTextInputLayout.error = getString(R.string.empty_form_field_error)
            }
            AddHostStatus.LABEL_OK -> binding.labelTextInputLayout.error = null
            AddHostStatus.EMPTY_ADDRESS -> {
                binding.addressEditText.requestFocus()
                binding.addressTextInputLayout.error = getString(R.string.empty_form_field_error)
            }
            AddHostStatus.ADDRESS_OK -> binding.addressTextInputLayout.error = null
            AddHostStatus.INVALID_PORT -> {
                binding.portEditText.requestFocus()
                binding.portTextInputLayout.error = getString(R.string.empty_form_field_error)
            }
            AddHostStatus.PORT_OK -> binding.portTextInputLayout.error = null
            AddHostStatus.EMPTY_USERNAME -> {
                binding.usernameEditText.requestFocus()
                binding.usernameTextInputLayout.error = getString(R.string.empty_form_field_error)
            }
            AddHostStatus.USERNAME_OK -> binding.usernameTextInputLayout.error = null
            AddHostStatus.EMPTY_PASSWORD -> {
                binding.passwordEditText.requestFocus()
                binding.passwordTextInputLayout.error = getString(R.string.empty_form_field_error)
            }
            AddHostStatus.PASSWORD_OK -> {
                binding.passwordTextInputLayout.error = null
            }
            AddHostStatus.INVALID_KEY -> {
                viewModel.setKey(null)
                UIUtils.showError(
                    binding.root,
                    getString(R.string.add_host_invalid_key),
                    requireActivity()
                )
            }
            AddHostStatus.HOST_CREATED -> {
                val result = Bundle(1)
                result.putString(HOST_ID_PARAMETER, viewModel.host.id)
                setFragmentResult(HOST_REQUEST_KEY, result)
                findNavController().navigateUp()
            }
            else -> context?.shortToast("Something went wrong, please try again!")
        }
    }

}
