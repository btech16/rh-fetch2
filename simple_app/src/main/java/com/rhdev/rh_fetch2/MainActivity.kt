package com.rhdev.rh_fetch2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rhdev.rh_fetch2.databinding.ActivityMainBinding
import com.tonyodev.fetch2.CompletedDownload
import com.tonyodev.fetch2.DefaultFetchNotificationManager
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.EnqueueAction
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.Fetch.Impl.getInstance
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2core.FetchObserver
import com.tonyodev.fetch2core.Reason
import com.tonyodev.fetch2okhttp.OkHttpDownloader
import okhttp3.OkHttpClient
import timber.log.Timber
import kotlin.io.path.pathString


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fetch: Fetch

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(
                    this, "Cannot call without that permission", Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this, "Permission Granted", Toast.LENGTH_LONG
                )
            }
        }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun isPermissionGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED

    override fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        val shouldShowRequestPermissionRationale =
            super.shouldShowRequestPermissionRationale(permission)
        Timber.d("permission = $permission , shouldShowRequestPermissionRationale = $shouldShowRequestPermissionRationale")
        return shouldShowRequestPermissionRationale
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initTimber()
        val okHttpClient: OkHttpClient = OkHttpClient.Builder().build()

        val fetchConfiguration = FetchConfiguration.Builder(this)
            .setDownloadConcurrentLimit(10)
            .enableLogging(true)
            .setNotificationManager(object :
                DefaultFetchNotificationManager(this) {
                override fun getFetchInstanceForNamespace(namespace: String): Fetch {
                    return fetch
                }
            })
            .setHttpDownloader(OkHttpDownloader(okHttpClient))
            .build()

        fetch = getInstance(fetchConfiguration)
        fetch.deleteAll()



        binding.run {
            initFetchBtn.setOnClickListener {
                Toast.makeText(this@MainActivity, "Is Closed ${fetch.isClosed}", Toast.LENGTH_LONG)
                    .show()
            }
            downloadBtn.setOnClickListener {
                Timber.d(
                    "areNotificationEnabled = ${
                        NotificationManagerCompat.from(this@MainActivity).areNotificationsEnabled()
                    }"
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    when {
                        isPermissionGranted() -> {
                            downloadFile()
                        }

                        shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                            Toast.makeText(
                                this@MainActivity, "تم رفض الإذن أكثر من مرة", Toast.LENGTH_LONG
                            ).show()
                            startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                ).setData(Uri.fromParts("package", packageName, null))
                            )
                        }

                        else -> {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                } else {
                    downloadFile()
                }
            }
            downloadBtn.setOnLongClickListener {
                fetch.addCompletedDownload(
                    completedDownload = CompletedDownload(),
                    alertListeners = true,
                    func = { download ->
                        Timber.d("download = $download")
                    },
                    func2 = { error ->
                        Timber.e(error.throwable)
                    })
                true
            }
        }

    }

    private fun initTimber() {
        Timber.plant(object : Timber.DebugTree() {
            override fun createStackElementTag(element: StackTraceElement): String {
                return "P_(${element.fileName}:${element.lineNumber})#M#${element.methodName}"
            }


            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                val nTag = tag?.substringBefore("#M#")
                val nMessage = "${tag?.substringAfterLast("#M#")}(): { $message }"
                super.log(priority, nTag, nMessage, t)
            }
        })
    }


    private fun downloadFile() {
        val url = "https://link.testfile.org/15MB"
        val downloadPath = kotlin.io.path.createTempFile("prefix", ".temp")
        Timber.d("downloadPath = $downloadPath")

        val request = Request(url, downloadPath.pathString)
        request.priority = Priority.HIGH
        request.networkType = NetworkType.ALL
        request.enqueueAction = EnqueueAction.REPLACE_EXISTING

        val fetchObserver: FetchObserver<Download> = object : FetchObserver<Download> {
            override fun onChanged(data: Download, reason: Reason) {
                Timber.d("reason = $reason")
                if (reason === Reason.DOWNLOAD_COMPLETED) {
                    fetch.removeFetchObserversForDownload(request.id, this)
                } else if (reason === Reason.DOWNLOAD_CANCELLED || reason === Reason.DOWNLOAD_ERROR) {
                    fetch.removeFetchObserversForDownload(request.id, this)
                }
            }

        }

        fetch.attachFetchObserversForDownload(request.id, fetchObserver)
            .enqueue(request, { result ->
                Timber.d("result = $result")
            }, { error ->
                Timber.d(error.throwable)
            })
    }


}
