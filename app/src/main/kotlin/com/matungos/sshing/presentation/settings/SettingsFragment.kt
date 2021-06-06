package com.matungos.sshing.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.matungos.sshing.R
import com.matungos.sshing.databinding.SettingsFragmentBinding
import com.matungos.sshing.extensions.longToast
import com.matungos.sshing.utils.UIUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Gabriel on 02/05/2021.
 */
class SettingsFragment : Fragment() {

    private var snackbar: Snackbar? = null

    private val viewModel: SettingsViewModel by viewModels()

    private var _binding: SettingsFragmentBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true).apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false).apply {
            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.importSettingsButton.setOnClickListener {
            snackbar?.dismiss()
            snackbar = null
            actionImport.launch(arrayOf("*/*"))
        }

        binding.exportSettings.setOnClickListener {
            snackbar?.dismiss()
            snackbar = null
            actionExport.launch(generateFileName())
        }

        viewModel.getStatus().observe(viewLifecycleOwner, { handleState(it) })
    }

    private fun handleState(state: SettingsViewModel.BackupState) {
        when (state) {
            is SettingsViewModel.BackupState.SuccessRestored -> onBackupRestored(state)

            is SettingsViewModel.BackupState.SuccessCreated -> onBackupCreated(state)

            is SettingsViewModel.BackupState.Error -> onBackupError(state)
        }
    }

    private fun onBackupCreated(state: SettingsViewModel.BackupState.SuccessCreated) {
        context?.longToast("${state.fileName} ${getString(R.string.settings_file_created_message_ending)}")
    }

    private fun onBackupRestored(state: SettingsViewModel.BackupState.SuccessRestored) {
        context?.longToast(state.details)
    }

    private fun onBackupError(state: SettingsViewModel.BackupState.Error) {
        UIUtils.showError(binding.root, state.message, requireContext())
    }

    private val actionImport =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val documentUri = uri ?: return@registerForActivityResult

            val documentFile = DocumentFile.fromSingleUri(requireContext(), documentUri)
                ?: return@registerForActivityResult

            val documentName = documentFile.name ?: return@registerForActivityResult

            lifecycleScope.launch {
                @Suppress("BlockingMethodInNonBlockingContext")
                val documentStream = withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openInputStream(documentUri)
                } ?: return@launch

                viewModel.importFile(documentStream, documentName)
            }
        }

    private val actionExport =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
            val documentUri = uri ?: return@registerForActivityResult

            val documentFile = DocumentFile.fromSingleUri(requireContext(), documentUri)
                ?: return@registerForActivityResult

            val documentName = documentFile.name ?: return@registerForActivityResult

            lifecycleScope.launch {
                @Suppress("BlockingMethodInNonBlockingContext")
                val documentStream = withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openOutputStream(documentUri)
                } ?: return@launch

                viewModel.exportData(
                    documentName,
                    documentStream,
                    binding.includeSensitiveDataCheckBox.isChecked
                )
            }
        }

    companion object {

        @JvmStatic
        fun generateFileName(): String {
            val df = SimpleDateFormat("yyyyMMdd_hhmmss", Locale.US)
            return "sshing_data_" + df.format(Date()) + ".json"
        }

    }
}