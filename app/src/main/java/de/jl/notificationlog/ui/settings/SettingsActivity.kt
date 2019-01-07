package de.jl.notificationlog.ui.settings

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import de.jl.notificationlog.R
import de.jl.notificationlog.ui.App
import de.jl.notificationlog.ui.AppsUtil
import de.jl.notificationlog.util.Configuration

import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.content_settings.*

class SettingsActivity : AppCompatActivity() {
    val configuration: Configuration by lazy { Configuration.with(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)

        mode_radio_group.check(when (configuration.isWhitelistMode) {
            true -> R.id.mode_whitelist
            false -> R.id.mode_blacklist
        })

        mode_radio_group.setOnCheckedChangeListener { _, id ->
            configuration.isWhitelistMode = when (id) {
                R.id.mode_whitelist -> true
                R.id.mode_blacklist -> false
                else -> throw IllegalArgumentException()
            }
        }

        val adapter = CheckableAppListAdapter()
        val apps = AppsUtil.getAllApps(this).sortedBy { it.title.toLowerCase() }
        val searchTerm = MutableLiveData<String>().apply { value = search.text.toString() }

        search.addTextChangedListener(object: TextWatcher {
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                searchTerm.value = search.text.toString()
            }

            override fun afterTextChanged(p0: Editable?) {
                // ignore
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // ignore
            }
        })

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

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
