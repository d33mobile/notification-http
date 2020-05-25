package de.jl.notificationlog.util

import android.app.Application
import android.net.Uri
import android.os.AsyncTask
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import de.jl.notificationlog.BuildConfig
import de.jl.notificationlog.R
import de.jl.notificationlog.data.AppDatabase
import de.jl.notificationlog.ui.AppsUtil
import de.jl.notificationlog.ui.applist.AppListModel
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.DateFormat
import java.util.*

class ExportAsyncTask(private val context: Application, private val packageName: String, private val destination: Uri) : AsyncTask<Void, Void, Void>() {
    companion object {
        private const val PAGE_SIZE = 50
        private const val LOG_TAG = "ExportAsyncTask"

        private val handler = Handler()
    }

    private val calendar = Calendar.getInstance()
    private val dateFormat = DateFormat.getDateTimeInstance()

    override fun doInBackground(vararg params: Void): Void? {
        val exportAllApps = packageName == AppListModel.ALL_APPS
        val sorting = Configuration.with(context).sorting
        val versionHandling = Configuration.with(context).versionHandling
        val hideDuplicates = Configuration.with(context).hideDuplicates
        val db = AppDatabase.with(context).notification()

        try {
            context.contentResolver.openFileDescriptor(destination, "w").use {
                descriptor ->

                BufferedWriter(OutputStreamWriter(FileOutputStream(descriptor!!.fileDescriptor))).use {
                    writer ->

                    if (!exportAllApps) {
                        writer.write(AppsUtil.getAppTitle(packageName, context))
                        writer.write("\n\n")
                    }

                    var offset = 0

                    while (true) {
                        val data = db.getNotificationsPageSync(
                                if (exportAllApps) null else packageName, PAGE_SIZE, offset, sorting,
                                versionHandling, hideDuplicates
                        )

                        offset += data.size

                        if (data.isEmpty()) {
                            break
                        }

                        data.forEach { notification ->
                            writeIfSet(notification.title, writer)
                            writeIfSet(notification.text, writer)
                            writer.newLine()

                            if (exportAllApps) {
                                writer.write(AppsUtil.getAppTitle(notification.packageName, context))
                                writer.write(" - ")
                            }

                            calendar.timeInMillis = notification.time
                            writer.write(dateFormat.format(calendar.time))
                            writer.newLine()

                            writer.newLine()
                            writer.newLine()
                        }
                    }
                }
            }

            handler.post {
                Toast.makeText(context, R.string.export_done, Toast.LENGTH_SHORT).show()
            }
        } catch (ex: Exception) {
            if (BuildConfig.DEBUG) {
                Log.d(LOG_TAG, "export failed", ex)
            }

            handler.post {
                Toast.makeText(context, R.string.export_error, Toast.LENGTH_SHORT).show()
            }
        }

        return null
    }

    @Throws(IOException::class)
    private fun writeIfSet(string: String, writer: BufferedWriter) {
        if (!TextUtils.isEmpty(string)) {
            writer.write(string)
            writer.newLine()
        }
    }
}
