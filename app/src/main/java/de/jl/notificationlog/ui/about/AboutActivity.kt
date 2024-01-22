package de.jl.notificationlog.ui.about

import android.os.Bundle
import android.text.method.LinkMovementMethod
import de.jl.notificationlog.R
import de.jl.notificationlog.databinding.ActivityAboutBinding
import de.jl.notificationlog.ui.CheckAuthActivity

class AboutActivity : CheckAuthActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityAboutBinding.inflate(layoutInflater).also { setContentView(it.root) }

        setSupportActionBar(binding.toolbar)

        val versionName = packageManager.getPackageInfo(applicationInfo.packageName, 0).versionName

        binding.content.version.text = getString(R.string.about_version, versionName)
        binding.content.sourceCodeLink.movementMethod = LinkMovementMethod.getInstance()
        binding.content.licenseText.movementMethod = LinkMovementMethod.getInstance()
    }
}
