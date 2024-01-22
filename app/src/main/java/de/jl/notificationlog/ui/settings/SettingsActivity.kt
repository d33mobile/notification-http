package de.jl.notificationlog.ui.settings

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import de.jl.notificationlog.R
import de.jl.notificationlog.databinding.ActivitySettingsBinding
import de.jl.notificationlog.ui.App
import de.jl.notificationlog.ui.AppsUtil
import de.jl.notificationlog.ui.CheckAuthActivity

class SettingsActivity : CheckAuthActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivitySettingsBinding.inflate(layoutInflater).also { setContentView(it.root) }

        setSupportActionBar(binding.toolbar)

        binding.content.modeRadioGroup.check(when (configuration.isWhitelistMode) {
            true -> R.id.mode_whitelist
            false -> R.id.mode_blacklist
        })

        binding.content.modeRadioGroup.setOnCheckedChangeListener { _, id ->
            configuration.isWhitelistMode = when (id) {
                R.id.mode_whitelist -> true
                R.id.mode_blacklist -> false
                else -> throw IllegalArgumentException()
            }
        }

        val adapter = CheckableAppListAdapter()
        val apps = AppsUtil.getAllApps(this).sortedBy { it.title.lowercase() }
        val searchTerm = MutableLiveData<String>().apply { value = binding.content.search.text.toString() }

        binding.content.search.addTextChangedListener(object: TextWatcher {
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                searchTerm.value = binding.content.search.text.toString()
            }

            override fun afterTextChanged(p0: Editable?) {
                // ignore
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // ignore
            }
        })

        binding.content.recycler.layoutManager = LinearLayoutManager(this)
        binding.content.recycler.adapter = adapter

        adapter.checkedPackageNames = configuration.invertedApps
        searchTerm.observe(this, Observer { term ->
            adapter.apps = apps.filter {
                it.packageName.contains(term, true) ||
                        it.title.contains(term, true) }
        })

        adapter.listener = object: Listener {
            override fun onAppClicked(app: App) {
                configuration.toggleInvertedApp(app.packageName)
                adapter.checkedPackageNames = configuration.invertedApps
            }
        }
    }
}
