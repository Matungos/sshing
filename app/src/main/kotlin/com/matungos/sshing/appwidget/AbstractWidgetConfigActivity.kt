package com.matungos.sshing.appwidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.matungos.sshing.R
import com.matungos.sshing.databinding.AppwidgetConfigActivityBinding
import com.matungos.sshing.model.Command
import com.matungos.sshing.presentation.commandlist.CommandListAdapter

abstract class AbstractWidgetConfigActivity : AppCompatActivity() {

    abstract fun getType(): WidgetState.WidgetType

    private lateinit var viewModel: WidgetConfigViewModel
    private lateinit var binding: AppwidgetConfigActivityBinding
    private lateinit var adapter: CommandListAdapter

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(WidgetConfigViewModel::class.java)

        binding = AppwidgetConfigActivityBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        val view = binding.root
        setContentView(view)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }
        // If they gave us an intent without the widget id, just bail.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
        }

        binding.toolbar.setNavigationIcon(R.drawable.ic_close)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setTitle(R.string.appwidget_config_title)

        setupRecyclerView()

        viewModel.commandList.observe(this, {
            adapter.submitList(it)
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView() {
        adapter = CommandListAdapter(
            object : CommandListAdapter.CommandListAdapterListener {
                override fun onCommandSelected(command: Command) {
                    val wm = WidgetsManager(applicationContext)
                    val newWs = WidgetState(appWidgetId, command.id, getType())
                    wm.saveConfig(newWs)

                    AbstractWidgetProvider.drawWidget(applicationContext, appWidgetId)

                    val resultValue = Intent()
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    setResult(RESULT_OK, resultValue)
                    finish()
                }

                override fun onEditPressed(position: Int, command: Command, view: View) {}

                override fun onDuplicatePressed(position: Int, command: Command) {}

                override fun onDeletePressed(position: Int, command: Command) {}
            },
            showMenu = false,
            showDragView = false
        )
        binding.commandsRecyclerview.adapter = adapter
        binding.commandsRecyclerview.setHasFixedSize(true)
    }

}
