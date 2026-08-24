package com.example.gemininanotester

import android.content.ClipboardManager
import android.content.Context
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
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var resultText: TextView

    private val generativeModel by lazy {
        Generation.getClient()
    }

    private var lastReport = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val scroll = ScrollView(this)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val title = TextView(this).apply {
            text = "Gemini Nano AI Benchmark"
            textSize = 27f
            setPadding(0, 0, 0, 24)
        }

        val subtitle = TextView(this).apply {
            text = "On-device GenAI performance test"
            textSize = 16f
            setPadding(0, 0, 0, 20)
        }

        val device = TextView(this).apply {
            text = buildDeviceInfo()
            textSize = 15f
            setPadding(0, 0, 0, 24)
        }

        val availabilityButton = Button(this).apply {
            text = "CHECK AI AVAILABILITY"
        }

        val benchmarkButton = Button(this).apply {
            text = "RUN AI BENCHMARK"
        }

        val copyButton = Button(this).apply {
            text = "COPY BENCHMARK REPORT"
        }

        resultText = TextView(this).apply {
            text = "Run the availability check first."
            textSize = 17f
            setPadding(0, 24, 0, 32)
        }

        content.addView(title)
        content.addView(subtitle)
        content.addView(device)
        content.addView(availabilityButton)
        content.addView(benchmarkButton)
        content.addView(copyButton)
        content.addView(resultText)

        scroll.addView(content)
        root.addView(scroll)

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

    private fun buildDeviceInfo(): String {

        return """
            DEVICE
            ====================

            Manufacturer: ${Build.MANUFACTURER}
            Model: ${Build.MODEL}
            Device: ${Build.DEVICE}
            Product: ${Build.PRODUCT}

            Android: ${Build.VERSION.RELEASE}
            API: ${Build.VERSION.SDK_INT}

            Build:
            ${Build.DISPLAY}

            Security patch:
            ${Build.VERSION.SECURITY_PATCH}

            CPU ABI:
            ${Build.SUPPORTED_ABIS.joinToString()}
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

                val report = when (status) {

                    FeatureStatus.AVAILABLE -> {

                        val modelName = try {
                            generativeModel.getBaseModelName()
                        } catch (_: Exception) {
                            "Unknown"
                        }

                        """
                        AI AVAILABILITY
                        ====================

                        STATUS: AVAILABLE ✅

                        Local GenAI inference should
                        be available for this API.

                        BASE MODEL:
                        $modelName

                        STATUS CHECK:
                        ${elapsed} ms

                        $deviceReport()
                        """.trimIndent()
                    }

                    FeatureStatus.DOWNLOADABLE -> {

                        """
                        AI AVAILABILITY
                        ====================

                        STATUS: DOWNLOADABLE 🟡

                        The feature is supported but
                        required model data is not ready.

                        STATUS CHECK:
                        ${elapsed} ms

                        $deviceReport()
                        """.trimIndent()
                    }

                    FeatureStatus.DOWNLOADING -> {

                        """
                        AI AVAILABILITY
                        ====================

                        STATUS: DOWNLOADING 🟡

                        Model resources are currently
                        being prepared.

                        Try again after the download finishes.

                        $deviceReport()
                        """.trimIndent()
                    }

                    FeatureStatus.UNAVAILABLE -> {

                        """
                        AI AVAILABILITY
                        ====================

                        STATUS: UNAVAILABLE ❌

                        This ML Kit GenAI feature is not
                        currently available on this device
                        configuration.

                        STATUS CHECK:
                        ${elapsed} ms

                        $deviceReport()
                        """.trimIndent()
                    }

                    else -> {

                        """
                        AI AVAILABILITY
                        ====================

                        STATUS: UNKNOWN ⚠️

                        Returned status:
                        $status

                        STATUS CHECK:
                        ${elapsed} ms

                        $deviceReport()
                        """.trimIndent()
                    }
                }

                lastReport = report
                resultText.text = report

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
                    ${elapsed} ms

                    $deviceReport()
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

                        Reason:
                        Gemini Nano is not currently
                        available for the Prompt API.

                        Status:
                        $status

                        This device cannot receive a
                        meaningful Nano performance score
                        until the API becomes available.

                        $deviceReport()
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

                    "Reply with exactly: BENCHMARK TEST ONE",

                    "Explain in three short sentences why the sky is blue.",

                    "Rewrite this sentence to sound more professional: " +
                            "\"I can't come tomorrow because I have some work to do.\"",

                    "Summarize this in one sentence: " +
                            "Artificial intelligence can run directly on a phone, " +
                            "allowing some tasks to work without sending data to a cloud server.",

                    "Give three concise benefits of running an AI model locally on a smartphone."
                )

                val results = mutableListOf<BenchmarkResult>()

                /*
                 * Warm-up run.
                 *
                 * We don't include this in the final average because
                 * first-use initialization can be substantially slower.
                 */
                val warmupStart = SystemClock.elapsedRealtime()

                try {
                    generativeModel.generateContent(
                        "Warm up the local model. Reply with one word: READY"
                    )
                } catch (_: Exception) {
                }

                val warmupTime =
                    SystemClock.elapsedRealtime() - warmupStart

                /*
                 * Measured runs.
                 */
                for ((index, prompt) in prompts.withIndex()) {

                    val beforeCpu =
                        Debug.threadCpuTimeNanos()

                    val start =
                        SystemClock.elapsedRealtime()

                    try {

                        generativeModel.generateContent(prompt)

                        val elapsed =
                            SystemClock.elapsedRealtime() - start

                        val cpuAfter =
                            Debug.threadCpuTimeNanos()

                        results.add(
                            BenchmarkResult(
                                index = index + 1,
                                elapsedMs = elapsed,
                                cpuMs =
                                    (cpuAfter - beforeCpu) / 1_000_000.0,
                                success = true
                            )
                        )

                    } catch (e: Exception) {

                        val elapsed =
                            SystemClock.elapsedRealtime() - start

                        val cpuAfter =
                            Debug.threadCpuTimeNanos()

                        results.add(
                            BenchmarkResult(
                                index = index + 1,
                                elapsedMs = elapsed,
                                cpuMs =
                                    (cpuAfter - beforeCpu) / 1_000_000.0,
                                success = false
                            )
                        )
                    }
                }

                val successful =
                    results.filter { it.success }

                val average =
                    if (successful.isNotEmpty()) {
                        successful.map { it.elapsedMs }.average()
                    } else {
                        0.0
                    }

                val fastest =
                    successful.minOfOrNull { it.elapsedMs }
                        ?: 0L

                val slowest =
                    successful.maxOfOrNull { it.elapsedMs }
                        ?: 0L

                val successRate =
                    if (results.isNotEmpty()) {
                        (successful.size * 100.0) / results.size
                    } else {
                        0.0
                    }

                val battery = getBatterySnapshot()

                val report = buildString {

                    appendLine("GEMINI NANO BENCHMARK")
                    appendLine("====================")
                    appendLine()

                    appendLine("MODEL")
                    appendLine("--------------------")
                    appendLine(modelName)
                    appendLine()

                    appendLine("WARM-UP")
                    appendLine("--------------------")
                    appendLine("$warmupTime ms")
                    appendLine("(excluded from average)")
                    appendLine()

                    appendLine("TEST RESULTS")
                    appendLine("--------------------")

                    for (r in results) {

                        appendLine(
                            "Test ${r.index}: " +
                                    if (r.success) {
                                        "${r.elapsedMs} ms ✅"
                                    } else {
                                        "FAILED ❌"
                                    }
                        )
                    }

                    appendLine()

                    appendLine("SUMMARY")
                    appendLine("--------------------")
                    appendLine(
                        "Successful: ${successful.size}/${results.size}"
                    )
                    appendLine(
                        "Success rate: ${successRate.roundToInt()}%"
                    )

                    if (successful.isNotEmpty()) {
                        appendLine(
                            "Fastest: $fastest ms"
                        )

                        appendLine(
                            "Slowest: $slowest ms"
                        )

                        appendLine(
                            "Average: ${formatDouble(average)} ms"
                        )
                    }

                    appendLine()

                    appendLine("CPU TIME")
                    appendLine("--------------------")

                    successful.forEach { r ->
                        appendLine(
                            "Test ${r.index}: " +
                                    formatDouble(r.cpuMs) +
                                    " ms"
                        )
                    }

                    appendLine()

                    appendLine("BATTERY")
                    appendLine("--------------------")
                    appendLine(battery)

                    appendLine()

                    appendLine("IMPORTANT")
                    appendLine("--------------------")
                    appendLine(
                        "Generation throughput in tokens/sec " +
                                "is NOT reported because the ML Kit " +
                                "Prompt API used here does not expose " +
                                "a reliable generated-token count."
                    )

                    appendLine()

                    appendLine("DEVICE")
                    appendLine("--------------------")
                    appendLine(buildDeviceInfo())
                }

                lastReport = report
                resultText.text = report

            } catch (e: Exception) {

                val report = """
                    GEMINI NANO BENCHMARK
                    ====================

                    BENCHMARK FAILED ❌

                    Exception:
                    ${e.javaClass.simpleName}

                    Message:
                    ${e.message ?: "No message"}

                    $deviceReport()
                """.trimIndent()

                lastReport = report
                resultText.text = report
            }
        }
    }

    private fun getBatterySnapshot(): String {

        val manager =
            getSystemService(BATTERY_SERVICE)
                    as BatteryManager

        val percent =
            manager.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CAPACITY
            )

        return "Battery: $percent%"
    }

    private fun deviceReport(): String {

        return """
            DEVICE
            --------------------
            $deviceInfoShort()

            AICore package:
            ${
                try {
                    packageManager.getPackageInfo(
                        "com.google.android.aicore",
                        0
                    )
                    "INSTALLED ✅"
                } catch (_: Exception) {
                    "NOT DETECTED ❌"
                }
            }
        """.trimIndent()
    }

    private fun deviceInfoShort(): String {

        return """
            ${Build.MANUFACTURER} ${Build.MODEL}
            Android ${Build.VERSION.RELEASE}
            API ${Build.VERSION.SDK_INT}
            ABI ${Build.SUPPORTED_ABIS.joinToString()}
        """.trimIndent()
    }

    private fun formatDouble(value: Double): String {
        return "%.2f".format(value)
    }

    private fun copyReport() {

        val report =
            if (lastReport.isNotBlank()) {
                lastReport
            } else {
                "No benchmark or diagnostic report generated yet."
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

    private data class BenchmarkResult(
        val index: Int,
        val elapsedMs: Long,
        val cpuMs: Double,
        val success: Boolean
    )
}
