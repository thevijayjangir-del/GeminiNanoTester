package com.example.gemininanotester

import android.content.ClipboardManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.os.SystemClock
import android.content.pm.PackageInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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

        val benchmarkButton = Button(this).apply {
            text = "RUN AI BENCHMARK"
        }

        val copyButton = Button(this).apply {
            text = "COPY REPORT"
        }

        resultText = TextView(this).apply {
            text = "Run the availability check first."
            textSize = 17f
            setPadding(0, 24, 0, 32)
        }

        content.addView(title)
        content.addView(subtitle)
        content.addView(deviceInfo)
        content.addView(availabilityButton)
        content.addView(benchmarkButton)
        content.addView(copyButton)
        content.addView(resultText)

        scrollView.addView(content)
        root.addView(scrollView)

        setContentView(root)

        availabilityButton.setOnClickListener {
            checkAvailability()
        }

        benchmarkButton.setOnClickListener {
            runBenchmark()
        }

        copyButton.setOnClickListener {
            copyReport()
        }
    }

    private fun getAICoreVersion(): String {
        return try {
            val info: PackageInfo = packageManager.getPackageInfo(
                "com.google.android.aicore",
                0
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionName ?: "Unknown"
            } else {
                @Suppress("DEPRECATION")
                info.versionName ?: "Unknown"
            }

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

    private fun buildDeviceReport(): String {

        return """
            DEVICE
            ====================

            Manufacturer:
            ${Build.MANUFACTURER}

            Model:
            ${Build.MODEL}

            Device:
            ${Build.DEVICE}

            Product:
            ${Build.PRODUCT}

            Android:
            ${Build.VERSION.RELEASE}

            API:
            ${Build.VERSION.SDK_INT}

            Build:
            ${Build.DISPLAY}

            Security patch:
            ${Build.VERSION.SECURITY_PATCH}

            CPU ABI:
            ${Build.SUPPORTED_ABIS.joinToString()}

            AICore:
            ${if (isAICoreInstalled()) "INSTALLED ✅" else "NOT DETECTED ❌"}

            AICore version:
            ${getAICoreVersion()}
        """.trimIndent()
    }

    private fun checkAvailability() {

        resultText.text = "Checking on-device AI..."

        lifecycleScope.launch {

            val start = SystemClock.elapsedRealtime()

            try {

                val status = generativeModel.checkStatus()

                val elapsed =
                    SystemClock.elapsedRealtime() - start

                val report = StringBuilder()

                report.appendLine("GEMINI NANO / AICORE")
                report.appendLine("====================")
                report.appendLine()

                when (status) {

                    FeatureStatus.AVAILABLE -> {

                        report.appendLine("STATUS: AVAILABLE ✅")
                        report.appendLine()
                        report.appendLine(
                            "The requested GenAI Prompt API is available."
                        )
                        report.appendLine()

                        try {
                            report.appendLine("BASE MODEL:")
                            report.appendLine(
                                generativeModel.getBaseModelName()
                            )
                        } catch (_: Exception) {
                            report.appendLine("BASE MODEL: Unknown")
                        }
                    }

                    FeatureStatus.DOWNLOADABLE -> {

                        report.appendLine("STATUS: DOWNLOADABLE 🟡")
                        report.appendLine()
                        report.appendLine(
                            "The feature is supported, but model data "
                        )
                        report.appendLine(
                            "has not been downloaded yet."
                        )
                    }

                    FeatureStatus.DOWNLOADING -> {

                        report.appendLine("STATUS: DOWNLOADING 🟡")
                        report.appendLine()
                        report.appendLine(
                            "AICore is currently downloading model resources."
                        )
                    }

                    FeatureStatus.UNAVAILABLE -> {

                        report.appendLine("STATUS: UNAVAILABLE ❌")
                        report.appendLine()
                        report.appendLine(
                            "The requested GenAI feature is not "
                        )
                        report.appendLine(
                            "currently available on this configuration."
                        )
                    }

                    else -> {

                        report.appendLine("STATUS: UNKNOWN ⚠️")
                        report.appendLine()
                        report.appendLine(
                            "Returned status: $status"
                        )
                    }
                }

                report.appendLine()
                report.appendLine("STATUS CHECK TIME:")
                report.appendLine("$elapsed ms")

                report.appendLine()
                report.appendLine(buildDeviceReport())

                lastReport = report.toString()
                resultText.text = lastReport

            } catch (e: Exception) {

                val elapsed =
                    SystemClock.elapsedRealtime() - start

                val report = """
                    AI AVAILABILITY
                    ====================

                    STATUS: ERROR ❌

                    Exception:
                    ${e.javaClass.simpleName}

                    Message:
                    ${e.message ?: "No message"}

                    Check time:
                    $elapsed ms

                    $buildDeviceReport()
                """.trimIndent()

                lastReport = report
                resultText.text = report
            }
        }
    }

    private fun runBenchmark() {

        resultText.text = "Preparing benchmark..."

        lifecycleScope.launch {

            try {

                val status = generativeModel.checkStatus()

                if (status != FeatureStatus.AVAILABLE) {

                    val report = """
                        GEMINI NANO BENCHMARK
                        ====================

                        BENCHMARK NOT RUN ❌

                        Gemini Nano is not currently
                        available through the Prompt API.

                        Status:
                        $status

                        No performance score was generated.
                        This avoids producing a misleading score.

                        ${buildDeviceReport()}
                    """.trimIndent()

                    lastReport = report
                    resultText.text = report

                    return@launch
                }

                val modelName = try {
                    generativeModel.getBaseModelName()
                } catch (_: Exception) {
                    "Unknown"
                }

                val prompts = listOf(
                    "Reply with exactly: TEST ONE",
                    "Explain in three short sentences why the sky is blue.",
                    "Rewrite this sentence professionally: I cannot attend tomorrow because I have work.",
                    "Summarize this in one sentence: On-device AI can perform certain tasks locally without sending the request to a cloud server.",
                    "Give three concise benefits of running an AI model locally."
                )

                val results = mutableListOf<RunResult>()

                /*
                 * Warm-up.
                 * This is intentionally excluded from the main average.
                 */
                val warmupStart =
                    SystemClock.elapsedRealtime()

                try {
                    generativeModel.generateContent(
                        "Warm up. Reply with READY."
                    )
                } catch (_: Exception) {
                }

                val warmupTime =
                    SystemClock.elapsedRealtime() - warmupStart

                /*
                 * Timed benchmark runs.
                 */
                for (index in prompts.indices) {

                    val prompt = prompts[index]

                    val wallStart =
                        SystemClock.elapsedRealtime()

                    val cpuStart =
                        Debug.threadCpuTimeNanos()

                    try {

                        generativeModel.generateContent(prompt)

                        val wallTime =
                            SystemClock.elapsedRealtime() - wallStart

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
                            SystemClock.elapsedRealtime() - wallStart

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

                val successful =
                    results.filter { it.success }

                val average =
                    if (successful.isNotEmpty()) {
                        successful
                            .map { it.wallTimeMs }
                            .average()
                    } else {
                        0.0
                    }

                val fastest =
                    successful.minOfOrNull {
                        it.wallTimeMs
                    } ?: 0L

                val slowest =
                    successful.maxOfOrNull {
                        it.wallTimeMs
                    } ?: 0L

                val successRate =
                    if (results.isNotEmpty()) {
                        successful.size * 100.0 /
                            results.size
                    } else {
                        0.0
                    }

                val report = StringBuilder()

                report.appendLine("GEMINI NANO BENCHMARK")
                report.appendLine("====================")
                report.appendLine()

                report.appendLine("MODEL:")
                report.appendLine(modelName)
                report.appendLine()

                report.appendLine("WARM-UP:")
                report.appendLine("$warmupTime ms")
                report.appendLine("(excluded from average)")
                report.appendLine()

                report.appendLine("INDIVIDUAL RUNS:")
                report.appendLine("--------------------")

                for (run in results) {

                    if (run.success) {

                        report.appendLine(
                            "Run ${run.number}: " +
                                "${run.wallTimeMs} ms ✅"
                        )

                    } else {

                        report.appendLine(
                            "Run ${run.number}: FAILED ❌"
                        )
                    }
                }

                report.appendLine()

                report.appendLine("SUMMARY:")
                report.appendLine("--------------------")

                report.appendLine(
                    "Success: ${successful.size}/${results.size}"
                )

                report.appendLine(
                    "Success rate: " +
                        String.format(
                            Locale.US,
                            "%.1f",
                            successRate
                        ) +
                        "%"
                )

                if (successful.isNotEmpty()) {

                    report.appendLine(
                        "Fastest: $fastest ms"
                    )

                    report.appendLine(
                        "Slowest: $slowest ms"
                    )

                    report.appendLine(
                        "Average: " +
                            String.format(
                                Locale.US,
                                "%.2f",
                                average
                            ) +
                            " ms"
                    )
                }

                report.appendLine()

                report.appendLine("THREAD CPU TIME:")
                report.appendLine("--------------------")

                for (run in successful) {

                    report.appendLine(
                        "Run ${run.number}: " +
                            String.format(
                                Locale.US,
                                "%.2f",
                                run.cpuTimeMs
                            ) +
                            " ms"
                    )
                }

                report.appendLine()

                report.appendLine("BATTERY:")
                report.appendLine("--------------------")
                report.appendLine(
                    getBatteryPercent()
                )

                report.appendLine()

                report.appendLine("TOKEN THROUGHPUT:")
                report.appendLine("--------------------")
                report.appendLine(
                    "Not measured."
                )
                report.appendLine(
                    "This API version does not provide a reliable"
                )
                report.appendLine(
                    "generated-token count for calculating tokens/sec."
                )

                report.appendLine()

                report.appendLine(buildDeviceReport())

                lastReport = report.toString()
                resultText.text = lastReport

            } catch (e: Exception) {

                val report = """
                    GEMINI NANO BENCHMARK
                    ====================

                    BENCHMARK FAILED ❌

                    Exception:
                    ${e.javaClass.simpleName}

                    Message:
                    ${e.message ?: "No message"}

                    ${buildDeviceReport()}
                """.trimIndent()

                lastReport = report
                resultText.text = report
            }
        }
    }

    private fun getBatteryPercent(): String {

        return try {

            val manager =
                getSystemService(BATTERY_SERVICE)
                    as BatteryManager

            val percent =
                manager.getIntProperty(
                    BatteryManager.BATTERY_PROPERTY_CAPACITY
                )

            "$percent%"

        } catch (_: Exception) {

            "Unknown"
        }
    }

    private fun copyReport() {

        val report =
            if (lastReport.isNotBlank()) {
                lastReport
            } else {
                "No report has been generated yet."
            }

        val clipboard =
            getSystemService(Context.CLIPBOARD_SERVICE)
                as ClipboardManager

        clipboard.setPrimaryClip(
            android.content.ClipData.newPlainText(
                "Gemini Nano Benchmark Report",
                report
            )
        )

        resultText.append(
            "\n\n✅ Report copied to clipboard."
        )
    }

    override fun onDestroy() {

        try {
            generativeModel.close()
        } catch (_: Exception) {
        }

        super.onDestroy()
    }

    private data class RunResult(
        val number: Int,
        val wallTimeMs: Long,
        val cpuTimeMs: Double,
        val success: Boolean
    )
}
