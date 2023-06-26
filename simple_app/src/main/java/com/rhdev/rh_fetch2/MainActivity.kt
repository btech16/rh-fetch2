package com.rhdev.rh_fetch2

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rhdev.rh_fetch2.databinding.ActivityMainBinding
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
import kotlin.io.path.pathString


class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding
    private lateinit var fetch:Fetch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val okHttpClient: OkHttpClient = OkHttpClient.Builder().build()

        val fetchConfiguration = FetchConfiguration.Builder(this)
            .setDownloadConcurrentLimit(10)
            .setHttpDownloader(OkHttpDownloader(okHttpClient))
            .build()

        fetch = getInstance(fetchConfiguration)

        binding.run {
            initFetchBtn.setOnClickListener {
                    Toast.makeText(this@MainActivity, "Is Closed ${fetch.isClosed}", Toast.LENGTH_LONG).show()
            }


            downloadBtn.setOnClickListener {

                downloadFile()

            }
        }

    }

    private fun downloadFile() {
        val url = "http://ipv4.download.thinkbroadband.com/200MB.zip"
        val downloadPath = kotlin.io.path.createTempFile("prefix", ".temp")
        Log.d("P_TAG", "downloadFile() called , downloadPath : $downloadPath")

        val request = Request(url, downloadPath.pathString)
        request.priority = Priority.HIGH
        request.networkType = NetworkType.ALL
        request.enqueueAction = EnqueueAction.REPLACE_EXISTING

        val fetchObserver: FetchObserver<Download> = object : FetchObserver<Download> {
            override fun onChanged(data: Download, reason: Reason) {
                Log.d(
                    "P_TAG",
                    "onChanged() called with: data = [$data], reason = [$reason]"
                )
                if (reason === Reason.DOWNLOAD_COMPLETED) {
                    fetch.removeFetchObserversForDownload(request.id, this)
                } else if (reason === Reason.DOWNLOAD_CANCELLED || reason === Reason.DOWNLOAD_ERROR) {
                    fetch.removeFetchObserversForDownload(request.id, this)
                }
            }

        }

        fetch.attachFetchObserversForDownload(request.id, fetchObserver)
            .enqueue(request, { result ->
                Log.d(
                    "P_TAG",
                    "enqueue call() called with: result = [$result]"
                )
            }, { result ->
                Log.e(
                    "P_TAG",
                    "call() called with: result = [$result]"
                )
            })
    }


}
