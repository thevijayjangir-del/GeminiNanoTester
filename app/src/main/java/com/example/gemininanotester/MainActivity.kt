package com.example.gemininanotester

import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageInfo
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.os.SystemClock
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var resultText: TextView

    private val generativeModel by lazy {
        Generation.getClient()
    }

    private var lastReport: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val scrollView = ScrollView(this)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val title = TextView(this).apply {
            text = "Gemini Nano AI Benchmark"
            textSize = 27f
            setPadding(0, 0, 0, 20)
        }

        val subtitle = TextView(this).apply {
            text = "On-device GenAI performance tester"
            textSize = 16f
            setPadding(0, 0, 0, 20)
        }

        val deviceInfo = TextView(this).apply {
            text = buildDeviceReport()
            textSize = 15f
            setPadding(0, 0, 0, 20)
        }

        val availabilityButton = Button(this).apply {
            text = "CHECK AI AVAILABILITY"
        }

        val downloadButton = Button(this).apply {
            text = "DOWNLOAD GEMINI NANO"
        }

        val benchmarkButton = Button(this).apply {
            text = "RUN AI BENCHMARK"
        }

        val copyButton = Button(this).apply {
            text = "COPY REPORT"
        }

        resultText = TextView(this).apply {
            text = "Check AI availability first."
            textSize = 17f
            setPadding(0, 24, 0, 32)
        }

        content.addView(title)
        content.addView(subtitle)
        content.addView(deviceInfo)
        content.addView(availabilityButton)
        content.addView(downloadButton)
        content.addView(benchmarkButton)
        content.addView(copyButton)
        content.addView(resultText)

        scrollView.addView(content)
        root.addView(scrollView)

        setContentView(root)

        availabilityButton.setOnClickListener {
            checkAvailability()
        }

        downloadButton.setOnClickListener {
            downloadNano()
        }

        benchmarkButton.setOnClickListener {
            runBenchmark()
        }

        copyButton.setOnClickListener {
            copyReport()
        }
    }

    // =========================================================
    // AICORE INFORMATION
    // =========================================================

    private fun getAICoreVersion(): String {
        return try {
            val info: PackageInfo =
                packageManager.getPackageInfo(
                    "com.google.android.aicore",
                    0
                )

            @Suppress("DEPRECATION")
            info.versionName ?: "Unknown"

        } catch (_: Exception) {
            "Not installed / not accessible"
        }
    }

    private fun isAICoreInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo(
                "com.google.android.aicore",
                0
            )

            true

        } catch (_: Exception) {
            false
        }
    }

    // =========================================================
    // DEVICE REPORT
    // =========================================================

    private fun buildDeviceReport(): String {

        val aicoreStatus =
            if (isAICoreInstalled()) {
                "INSTALLED"
            } else {
                "NOT DETECTED"
            }

        return buildString {

            appendLine("DEVICE")
            appendLine("====================")
            appendLine()

            appendLine("Manufacturer:")
            appendLine(Build.MANUFACTURER)
            appendLine()

            appendLine("Model:")
            appendLine(Build.MODEL)
            appendLine()

            appendLine("Device:")
            appendLine(Build.DEVICE)
            appendLine()

            appendLine("Product:")
            appendLine(Build.PRODUCT)
            appendLine()

            appendLine("Android:")
            appendLine(Build.VERSION.RELEASE)
            appendLine()

            appendLine("API:")
            appendLine(Build.VERSION.SDK_INT)
            appendLine()

            appendLine("Build:")
            appendLine(Build.DISPLAY)
            appendLine()

            appendLine("Security patch:")
            appendLine(Build.VERSION.SECURITY_PATCH)
            appendLine()

            appendLine("CPU ABI:")
            appendLine(Build.SUPPORTED_ABIS.joinToString())
            appendLine()

            appendLine("AICore:")
            appendLine(aicoreStatus)
            appendLine()

            appendLine("AICore version:")
            appendLine(getAICoreVersion())
        }
    }

    // =========================================================
    // AVAILABILITY CHECK
    // =========================================================

    private fun checkAvailability() {

        resultText.text = "Checking on-device AI..."

        lifecycleScope.launch {

            val start =
                SystemClock.elapsedRealtime()

            try {

                val status =
                    generativeModel.checkStatus()

                val elapsed =
                    SystemClock.elapsedRealtime() - start

                val report =
                    buildString {

                        appendLine("GEMINI NANO / AICORE")
                        appendLine("====================")
                        appendLine()

                        when (status) {

                            FeatureStatus.AVAILABLE -> {

                                appendLine(
                                    "STATUS: AVAILABLE"
                                )

                                appendLine()

                                appendLine(
                                    "Gemini Nano is ready for inference."
                                )

                                appendLine()

                                try {

                                    appendLine("BASE MODEL:")

                                    appendLine(
                                        generativeModel
                                            .getBaseModelName()
                                    )

                                } catch (_: Exception) {

                                    appendLine(
                                        "BASE MODEL: Unknown"
                                    )
                                }
                            }

                            FeatureStatus.DOWNLOADABLE -> {

                                appendLine(
                                    "STATUS: DOWNLOADABLE"
                                )

                                appendLine()

                                appendLine(
                                    "Gemini Nano can be downloaded on this device."
                                )

                                appendLine()

                                appendLine(
                                    "Press DOWNLOAD GEMINI NANO."
                                )
                            }

                            FeatureStatus.DOWNLOADING -> {

                                appendLine(
                                    "STATUS: DOWNLOADING"
                                )

                                appendLine()

                                appendLine(
                                    "Gemini Nano is currently being downloaded."
                                )

                                appendLine()

                                appendLine(
                                    "Wait for the download to finish."
                                )
                            }

                            FeatureStatus.UNAVAILABLE -> {

                                appendLine(
                                    "STATUS: UNAVAILABLE"
                                )

                                appendLine()

                                appendLine(
                                    "The requested GenAI feature is not"
                                )

                                appendLine(
                                    "currently available on this configuration."
                                )
                            }

                            else -> {

                                appendLine(
                                    "STATUS: UNKNOWN"
                                )

                                appendLine()

                                appendLine(
                                    "Returned status: $status"
                                )
                            }
                        }

                        appendLine()

                        appendLine("STATUS CHECK TIME:")
                        appendLine("$elapsed ms")

                        appendLine()

                        appendLine(
                            buildDeviceReport()
                        )
                    }

                lastReport = report
                resultText.text = report

            } catch (e: Exception) {

                val elapsed =
                    SystemClock.elapsedRealtime() - start

                val report =
                    buildString {

                        appendLine("AI AVAILABILITY")
                        appendLine("====================")
                        appendLine()

                        appendLine("STATUS: ERROR")
                        appendLine()

                        appendLine("Exception:")
                        appendLine(
                            e.javaClass.simpleName
                        )

                        appendLine()

                        appendLine("Message:")
                        appendLine(
                            e.message ?: "No message"
                        )

                        appendLine()

                        appendLine("Check time:")
                        appendLine("$elapsed ms")

                        appendLine()

                        appendLine(
                            buildDeviceReport()
                        )
                    }

                lastReport = report
                resultText.text = report
            }
        }
    }

    // =========================================================
    // GEMINI NANO DOWNLOAD
    // =========================================================

    private fun downloadNano() {

        resultText.text =
            "Checking whether Gemini Nano can be downloaded..."

        lifecycleScope.launch {

            try {

                val status =
                    generativeModel.checkStatus()

                when (status) {

                    FeatureStatus.AVAILABLE -> {

                        val report =
                            buildString {

                                appendLine(
                                    "GEMINI NANO DOWNLOAD"
                                )

                                appendLine(
                                    "===================="
                                )

                                appendLine()

                                appendLine(
                                    "STATUS: ALREADY AVAILABLE"
                                )

                                appendLine()

                                appendLine(
                                    "Gemini Nano is already downloaded"
                                )

                                appendLine(
                                    "and ready for inference."
                                )

                                appendLine()

                                try {

                                    appendLine("BASE MODEL:")
                                    appendLine(
                                        generativeModel
                                            .getBaseModelName()
                                    )

                                } catch (_: Exception) {

                                    appendLine(
                                        "BASE MODEL: Unknown"
                                    )
                                }

                                appendLine()

                                appendLine(
                                    buildDeviceReport()
                                )
                            }

                        lastReport = report
                        resultText.text = report
                    }

                    FeatureStatus.DOWNLOADABLE -> {

                        resultText.text =
                            buildString {

                                appendLine(
                                    "GEMINI NANO DOWNLOAD"
                                )

                                appendLine(
                                    "===================="
                                )

                                appendLine()

                                appendLine(
                                    "Starting download..."
                                )

                                appendLine()

                                appendLine(
                                    "Keep the phone connected to the internet."
                                )

                                appendLine(
                                    "Do not close the app while downloading."
                                )
                            }

                        var finalMessage =
                            "Download finished."

                        generativeModel
                            .download()
                            .collect { downloadStatus ->

                                when (downloadStatus) {

                                    is DownloadStatus.DownloadStarted -> {

                                        resultText.text =
                                            buildString {

                                                appendLine(
                                                    "GEMINI NANO DOWNLOAD"
                                                )

                                                appendLine(
                                                    "===================="
                                                )

                                                appendLine()

                                                appendLine(
                                                    "DOWNLOAD STARTED"
                                                )

                                                appendLine()

                                                appendLine(
                                                    "AICore is preparing Gemini Nano."
                                                )
                                            }
                                    }

                                    is DownloadStatus.DownloadProgress -> {

                                        val bytes =
                                            downloadStatus
                                                .totalBytesDownloaded

                                        resultText.text =
                                            buildString {

                                                appendLine(
                                                    "GEMINI NANO DOWNLOAD"
                                                )

                                                appendLine(
                                                    "===================="
                                                )

                                                appendLine()

                                                appendLine(
                                                    "DOWNLOADING..."
                                                )

                                                appendLine()

                                                appendLine(
                                                    "Downloaded:"
                                                )

                                                appendLine(
                                                    formatBytes(bytes)
                                                )

                                                appendLine()

                                                appendLine(
                                                    "AICore is downloading the"
                                                )

                                                appendLine(
                                                    "required Gemini Nano model."
                                                )
                                            }
                                    }

                                    DownloadStatus.DownloadCompleted -> {

                                        finalMessage =
                                            buildString {

                                                appendLine(
                                                    "GEMINI NANO DOWNLOAD"
                                                )

                                                appendLine(
                                                    "===================="
                                                )

                                                appendLine()

                                                appendLine(
                                                    "DOWNLOAD COMPLETE"
                                                )

                                                appendLine()
                                                appendLine(
                                                    "Gemini Nano model assets"
                                                )

                                                appendLine(
                                                    "have been downloaded."
                                                )

                                                appendLine()

                                                appendLine(
                                                    "Checking availability..."
                                                )
                                            }

                                        resultText.text =
                                            finalMessage
                                    }

                                    is DownloadStatus.DownloadFailed -> {

                                        finalMessage =
                                            buildString {

                                                appendLine(
                                                    "GEMINI NANO DOWNLOAD"
                                                )

                                                appendLine(
                                                    "===================="
                                                )

                                                appendLine()

                                                appendLine(
                                                    "DOWNLOAD FAILED"
                                                )

                                                appendLine()

                                                appendLine(
                                                    "The model could not be downloaded."
                                                )

                                                appendLine()

                                                appendLine(
                                                    "Error:"
                                                )

                                                appendLine(
                                                    downloadStatus.toString()
                                                )

                                                appendLine()

                                                appendLine(
                                                    "Check your internet connection,"
                                                )

                                                appendLine(
                                                    "AICore updates and device status."
                                                )
                                            }

                                        resultText.text =
                                            finalMessage
                                    }
                                }
                            }

                        val finalStatus =
                            try {
                                generativeModel.checkStatus()
                            } catch (_: Exception) {
                                null
                            }

                        val report =
                            buildString {

                                appendLine(finalMessage)

                                appendLine()

                                if (finalStatus ==
                                    FeatureStatus.AVAILABLE
                                ) {

                                    appendLine(
                                        "FINAL STATUS: AVAILABLE"
                                    )

                                    appendLine()

                                    try {

                                        appendLine(
                                            "BASE MODEL:"
                                        )

                                        appendLine(
                                            generativeModel
                                                .getBaseModelName()
                                        )

                                    } catch (_: Exception) {

                                        appendLine(
                                            "BASE MODEL: Unknown"
                                        )
                                    }

                                } else {

                                    appendLine(
                                        "FINAL STATUS:"
                                    )

                                    appendLine(
                                        finalStatus?.toString()
                                            ?: "UNKNOWN"
                                    )
                                }

                                appendLine()

                                appendLine(
                                    buildDeviceReport()
                                )
                            }

                        lastReport = report
                        resultText.text = report
                    }

                    FeatureStatus.DOWNLOADING -> {

                        val report =
                            buildString {

                                appendLine(
                                    "GEMINI NANO DOWNLOAD"
                                )

                                appendLine(
                                    "===================="
                                )

                                appendLine()

                                appendLine(
                                    "STATUS: ALREADY DOWNLOADING"
                                )

                                appendLine()

                                appendLine(
                                    "AICore is already downloading"
                                )

                                appendLine(
                                    "Gemini Nano."
                                )

                                appendLine()

                                appendLine(
                                    "Wait for it to finish and"
                                )

                                appendLine(
                                    "check availability again."
                                )

                                appendLine()

                                appendLine(
                                    buildDeviceReport()
                                )
                            }

                        lastReport = report
                        resultText.text = report
                    }

                    FeatureStatus.UNAVAILABLE -> {

                        val report =
                            buildString {

                                appendLine(
                                    "GEMINI NANO DOWNLOAD"
                                )

                                appendLine(
                                    "===================="
                                )

                                appendLine()

                                appendLine(
                                    "DOWNLOAD NOT AVAILABLE"
                                )

                                appendLine()

                                appendLine(
                                    "AICore currently reports that"
                                )

                                appendLine(
                                    "this GenAI feature is unavailable."
                                )

                                appendLine()

                                appendLine(
                                    "This is not a download failure."
                                )

                                appendLine(
                                    "The device configuration has not"
                                )

                                appendLine(
                                    "made the feature downloadable."
                                )

                                appendLine()

                                appendLine(
                                    buildDeviceReport()
                                )
                            }

                        lastReport = report
                        resultText.text = report
                    }

                    else -> {

                        val report =
                            buildString {

                                appendLine(
                                    "GEMINI NANO DOWNLOAD"
                                )

                                appendLine(
                                    "===================="
                                )

                                appendLine()

                                appendLine(
                                    "UNKNOWN STATUS"
                                )

                                appendLine()

                                appendLine(
                                    status.toString()
                                )

                                appendLine()

                                appendLine(
                                    buildDeviceReport()
                                )
                            }

                        lastReport = report
                        resultText.text = report
                    }
                }

            } catch (e: Exception) {

                val report =
                    buildString {

                        appendLine(
                            "GEMINI NANO DOWNLOAD"
                        )

                        appendLine(
                            "===================="
                        )

                        appendLine()

                        appendLine(
                            "DOWNLOAD ERROR"
                        )

                        appendLine()

                        appendLine(
                            "Exception:"
                        )

                        appendLine(
                            e.javaClass.simpleName
                        )

                        appendLine()

                        appendLine(
                            "Message:"
                        )

                        appendLine(
                            e.message ?: "No message"
                        )

                        appendLine()

                        appendLine(
                            buildDeviceReport()
                        )
                    }

                lastReport = report
                resultText.text = report
            }
        }
    }

    // =========================================================
    // FORMAT BYTES
    // =========================================================

    private fun formatBytes(bytes: Long): String {

        if (bytes < 1024) {
            return "$bytes B"
        }

        val kb =
            bytes / 1024.0

        if (kb < 1024) {
            return String.format(
                Locale.US,
                "%.2f KB",
                kb
            )
        }

        val mb =
            kb / 1024.0

        if (mb < 1024) {
            return String.format(
                Locale.US,
                "%.2f MB",
                mb
            )
        }

        val gb =
            mb / 1024.0

        return String.format(
            Locale.US,
            "%.2f GB",
            gb
        )
    }

    // =========================================================
    // AI BENCHMARK
    // =========================================================

    private fun runBenchmark() {

        resultText.text =
            "Preparing benchmark..."

        lifecycleScope.launch {

            try {

                val status =
                    generativeModel.checkStatus()

                if (status != FeatureStatus.AVAILABLE) {

                    val report =
                        buildString {

                            appendLine(
                                "GEMINI NANO BENCHMARK"
                            )

                            appendLine(
                                "===================="
                            )

                            appendLine()

                            appendLine(
                                "BENCHMARK NOT RUN"
                            )

                            appendLine()

                            appendLine(
                                "Gemini Nano is not currently"
                            )

                            appendLine(
                                "available through the Prompt API."
                            )

                            appendLine()

                            appendLine(
                                "Current status:"
                            )

                            appendLine(
                                status.toString()
                            )

                            appendLine()

                            if (
                                status ==
                                FeatureStatus.DOWNLOADABLE
                            ) {

                                appendLine(
                                    "Press DOWNLOAD GEMINI NANO"
                                )

                                appendLine(
                                    "before running the benchmark."
                                )

                            } else {

                                appendLine(
                                    "No performance score was generated."
                                )
                            }

                            appendLine()

                            appendLine(
                                buildDeviceReport()
                            )
                        }

                    lastReport = report
                    resultText.text = report

                    return@launch
                }

                val modelName =
                    try {

                        generativeModel
                            .getBaseModelName()

                    } catch (_: Exception) {

                        "Unknown"
                    }

                val prompts =
                    listOf(

                        "Reply with exactly: TEST ONE",

                        "Explain in three short sentences why the sky is blue.",

                        "Rewrite this sentence professionally: I cannot attend tomorrow because I have work.",

                        "Summarize this in one sentence: On-device AI can perform certain tasks locally without sending the request to a cloud server.",

                        "Give three concise benefits of running an AI model locally."
                    )

                val results =
                    mutableListOf<RunResult>()

                // -------------------------------------------------
                // WARMUP
                // -------------------------------------------------

                val warmupStart =
                    SystemClock.elapsedRealtime()

                try {

                    generativeModel.generateContent(
                        "Warm up. Reply with READY."
                    )

                } catch (_: Exception) {
                }

                val warmupTime =
                    SystemClock.elapsedRealtime() -
                        warmupStart

                // -------------------------------------------------
                // BENCHMARK RUNS
                // -------------------------------------------------

                for (index in prompts.indices) {

                    val prompt =
                        prompts[index]

                    val wallStart =
                        SystemClock.elapsedRealtime()

                    val cpuStart =
                        Debug.threadCpuTimeNanos()

                    try {

                        generativeModel
                            .generateContent(prompt)

                        val wallTime =
                            SystemClock.elapsedRealtime() -
                                wallStart

                        val cpuTime =
                            (
                                Debug.threadCpuTimeNanos() -
                                    cpuStart
                                ) / 1_000_000.0

                        results.add(
                            RunResult(
                                number = index + 1,
                                wallTimeMs = wallTime,
                                cpuTimeMs = cpuTime,
                                success = true
                            )
                        )

                    } catch (_: Exception) {

                        val wallTime =
                            SystemClock.elapsedRealtime() -
                                wallStart

                        val cpuTime =
                            (
                                Debug.threadCpuTimeNanos() -
                                    cpuStart
                                ) / 1_000_000.0

                        results.add(
                            RunResult(
                                number = index + 1,
                                wallTimeMs = wallTime,
                                cpuTimeMs = cpuTime,
                                success = false
                            )
                        )
                    }
                }

                // -------------------------------------------------
                // STATISTICS
                // -------------------------------------------------

                val successful =
                    results.filter {
                        it.success
                    }

                val average =
                    if (successful.isNotEmpty()) {

                        successful
                            .map {
                                it.wallTimeMs
                            }
                            .average()

                    } else {

                        0.0
                    }

                val fastest =
                    successful
                        .minOfOrNull {
                            it.wallTimeMs
                        }
                        ?: 0L

                val slowest =
                    successful
                        .maxOfOrNull {
                            it.wallTimeMs
                        }
                        ?: 0L

                val successRate =
                    if (results.isNotEmpty()) {

                        successful.size *
                            100.0 /
                            results.size

                    } else {

                        0.0
                    }

                // -------------------------------------------------
                // REPORT
                // -------------------------------------------------

                val report =
                    buildString {

                        appendLine(
                            "GEMINI NANO BENCHMARK"
                        )

                        appendLine(
                            "===================="
                        )

                        appendLine()

                        appendLine("MODEL:")
                        appendLine(modelName)

                        appendLine()

                        appendLine("WARM-UP:")
                        appendLine("$warmupTime ms")
                        appendLine(
                            "(excluded from average)"
                        )

                        appendLine()

                        appendLine("INDIVIDUAL RUNS:")
                        appendLine("--------------------")

                        for (run in results) {

                            if (run.success) {

                                appendLine(
                                    "Run ${run.number}: " +
                                        "${run.wallTimeMs} ms"
                                )

                            } else {

                                appendLine(
                                    "Run ${run.number}: FAILED"
                                )
                            }
                        }

                        appendLine()

                        appendLine("SUMMARY:")
                        appendLine("--------------------")

                        appendLine(
                            "Success: " +
                                "${successful.size}/${results.size}"
                        )

                        appendLine(
                            "Success rate: " +
                                String.format(
                                    Locale.US,
                                    "%.1f",
                                    successRate
                                ) +
                                "%"
                        )

                        if (successful.isNotEmpty()) {

                            appendLine(
                                "Fastest: $fastest ms"
                            )

                            appendLine(
                                "Slowest: $slowest ms"
                            )

                            appendLine(
                                "Average: " +
                                    String.format(
                                        Locale.US,
                                        "%.2f",
                                        average
                                    ) +
                                    " ms"
                            )
                        }

                        appendLine()

                        appendLine("THREAD CPU TIME:")
                        appendLine("--------------------")

                        for (run in successful) {

                            appendLine(
                                "Run ${run.number}: " +
                                    String.format(
                                        Locale.US,
                                        "%.2f",
                                        run.cpuTimeMs
                                    ) +
                                    " ms"
                            )
                        }

                        appendLine()

                        appendLine("BATTERY:")
                        appendLine("--------------------")

                        appendLine(
                            getBatteryPercent()
                        )

                        appendLine()

                        appendLine("TOKEN THROUGHPUT:")
                        appendLine("--------------------")

                        appendLine(
                            "Not measured."
                        )

                        appendLine(
                            "This benchmark does not estimate"
                        )

                        appendLine(
                            "tokens/sec from generated text."
                        )

                        appendLine()

                        appendLine(
                            buildDeviceReport()
                        )
                    }

                lastReport = report
                resultText.text = report

            } catch (e: Exception) {

                val report =
                    buildString {

                        appendLine(
                            "GEMINI NANO BENCHMARK"
                        )

                        appendLine(
                            "===================="
                        )

                        appendLine()

                        appendLine(
                            "BENCHMARK FAILED"
                        )

                        appendLine()

                        appendLine("Exception:")
                        appendLine(
                            e.javaClass.simpleName
                        )

                        appendLine()

                        appendLine("Message:")
                        appendLine(
                            e.message ?: "No message"
                        )

                        appendLine()

                        appendLine(
                            buildDeviceReport()
                        )
                    }

                lastReport = report
                resultText.text = report
            }
        }
    }

    // =========================================================
    // BATTERY
    // =========================================================

    private fun getBatteryPercent(): String {

        return try {

            val manager =
                getSystemService(
                    BATTERY_SERVICE
                ) as BatteryManager

            val percent =
                manager.getIntProperty(
                    BatteryManager.BATTERY_PROPERTY_CAPACITY
                )

            "$percent%"

        } catch (_: Exception) {

            "Unknown"
        }
    }

    // =========================================================
    // COPY REPORT
    // =========================================================

    private fun copyReport() {

        val report =
            if (lastReport.isNotBlank()) {

                lastReport

            } else {

                "No report has been generated yet."
            }

        val clipboard =
            getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

        clipboard.setPrimaryClip(
            android.content.ClipData.newPlainText(
                "Gemini Nano Benchmark Report",
                report
            )
        )

        resultText.append(
            "\n\nReport copied to clipboard."
        )
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    override fun onDestroy() {

        try {

            generativeModel.close()

        } catch (_: Exception) {
        }

        super.onDestroy()
    }

    // =========================================================
    // BENCHMARK RESULT
    // =========================================================

    private data class RunResult(

        val number: Int,

        val wallTimeMs: Long,

        val cpuTimeMs: Double,

        val success: Boolean
    )
}
