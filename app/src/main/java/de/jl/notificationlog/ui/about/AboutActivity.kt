package de.jl.notificationlog.ui.about

import android.os.Bundle
import android.text.method.LinkMovementMethod
import de.jl.notificationlog.BuildConfig
import de.jl.notificationlog.R
import de.jl.notificationlog.ui.CheckAuthActivity
import kotlinx.android.synthetic.main.activity_about.*
import kotlinx.android.synthetic.main.content_about.*

class AboutActivity : CheckAuthActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_about)
        setSupportActionBar(toolbar)

        version.text = getString(R.string.about_version, BuildConfig.VERSION_NAME)
        source_code_link.movementMethod = LinkMovementMethod.getInstance()
        license_text.movementMethod = LinkMovementMethod.getInstance()
    }
}
