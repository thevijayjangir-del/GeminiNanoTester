package com.example.gemininanotester

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var resultText: TextView

    private val generativeModel by lazy {
        Generation.getClient()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val title = TextView(this).apply {
            text = "Gemini Nano Tester"
            textSize = 28f
            setPadding(0, 0, 0, 30)
        }

        val deviceInfo = TextView(this).apply {
            text = buildDeviceInfo()
            textSize = 16f
        }

        val checkButton = Button(this).apply {
            text = "CHECK NANO / AICORE"
        }

        val testButton = Button(this).apply {
            text = "RUN REAL NANO TEST"
        }

        val copyButton = Button(this).apply {
            text = "COPY DIAGNOSTIC REPORT"
        }

        resultText = TextView(this).apply {
            text = "Press a button to begin."
            textSize = 17f
            setPadding(0, 30, 0, 0)
        }

        layout.addView(title)
        layout.addView(deviceInfo)
        layout.addView(checkButton)
        layout.addView(testButton)
        layout.addView(copyButton)
        layout.addView(resultText)

        setContentView(layout)

        checkButton.setOnClickListener {
            checkNano()
        }

        testButton.setOnClickListener {
            runNanoTest()
        }

        copyButton.setOnClickListener {
            copyDiagnosticReport()
        }
    }

    private fun buildDeviceInfo(): String {

        val aicoreVersion = getAICoreVersion()

        return """
            DEVICE

            Manufacturer: ${Build.MANUFACTURER}
            Model: ${Build.MODEL}
            Device: ${Build.DEVICE}
            Product: ${Build.PRODUCT}

            Android: ${Build.VERSION.RELEASE}
            API: ${Build.VERSION.SDK_INT}

            Build: ${Build.DISPLAY}
            Security patch: ${
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    Build.VERSION.SECURITY_PATCH
                else
                    "Unknown"
            }

            AICore package: ${
                if (aicoreVersion != null) "INSTALLED ✅" else "NOT FOUND ❌"
            }

            AICore version: ${aicoreVersion ?: "Unknown"}
        """.trimIndent()
    }

    private fun getAICoreVersion(): String? {

        return try {
            val packageInfo = packageManager.getPackageInfo(
                "com.google.android.aicore",
                0
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionName
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionName
            }

        } catch (e: PackageManager.NameNotFoundException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun checkNano() {

        resultText.text = "Checking Gemini Nano / AICore..."

        lifecycleScope.launch {

            val startTime = System.currentTimeMillis()

            try {

                val status = generativeModel.checkStatus()

                val elapsed = System.currentTimeMillis() - startTime

                val report = StringBuilder()

                report.appendLine("GEMINI NANO DIAGNOSTIC")
                report.appendLine("======================")
                report.appendLine()

                when (status) {

                    FeatureStatus.AVAILABLE -> {

                        report.appendLine("RESULT: AVAILABLE ✅")
                        report.appendLine()
                        report.appendLine("Gemini Nano is currently available")
                        report.appendLine("through the requested ML Kit")
                        report.appendLine("GenAI Prompt API.")
                        report.appendLine()

                        try {
                            report.appendLine("BASE MODEL")
                            report.appendLine("----------------------")
                            report.appendLine(
                                generativeModel.getBaseModelName()
                            )
                        } catch (e: Exception) {
                            report.appendLine("BASE MODEL")
                            report.appendLine("----------------------")
                            report.appendLine("Could not retrieve model name.")
                        }
                    }

                    FeatureStatus.DOWNLOADABLE -> {

                        report.appendLine("RESULT: DOWNLOADABLE 🟡")
                        report.appendLine()
                        report.appendLine(
                            "The requested Gemini Nano feature is supported"
                        )
                        report.appendLine(
                            "but the required model is not downloaded yet."
                        )
                    }

                    FeatureStatus.DOWNLOADING -> {

                        report.appendLine("RESULT: DOWNLOADING 🟡")
                        report.appendLine()
                        report.appendLine(
                            "Gemini Nano is currently being downloaded."
                        )
                    }

                    FeatureStatus.UNAVAILABLE -> {

                        report.appendLine("RESULT: UNAVAILABLE ❌")
                        report.appendLine()
                        report.appendLine(
                            "The requested Gemini Nano feature is not"
                        )
                        report.appendLine(
                            "currently available on this device."
                        )
                        report.appendLine()
                        report.appendLine("IMPORTANT")
                        report.appendLine("----------------------")
                        report.appendLine(
                            "This does NOT necessarily mean AICore is missing."
                        )
                        report.appendLine(
                            "It means this specific GenAI feature is unavailable."
                        )
                    }

                    else -> {

                        report.appendLine("RESULT: UNKNOWN ⚠️")
                        report.appendLine()
                        report.appendLine("Status: $status")
                    }
                }

                report.appendLine()
                report.appendLine("CHECK TIME")
                report.appendLine("----------------------")
                report.appendLine("$elapsed ms")

                report.appendLine()
                report.appendLine("DEVICE")
                report.appendLine("----------------------")
                report.appendLine(buildDeviceInfo())

                resultText.text = report.toString()

            } catch (e: Exception) {

                val elapsed = System.currentTimeMillis() - startTime

                val rawError = e.message ?: e.toString()

                resultText.text = buildErrorReport(
                    title = "CHECK FAILED ❌",
                    error = e,
                    rawError = rawError,
                    elapsed = elapsed
                )
            }
        }
    }

    private fun runNanoTest() {

        resultText.text = "Running real Gemini Nano inference..."

        lifecycleScope.launch {

            val startTime = System.currentTimeMillis()

            try {

                val response = generativeModel.generateContent(
                    "Reply with exactly: GEMINI NANO LOCAL TEST PASS"
                )

                val elapsed = System.currentTimeMillis() - startTime

                val generatedText =
                    response.candidates
                        .firstOrNull()
                        ?.text
                        ?: "No text returned."

                resultText.text = """
                    INFERENCE TEST
                    ===============

                    RESULT: SUCCESS ✅

                    Gemini Nano successfully processed
                    the prompt locally through the ML Kit
                    GenAI Prompt API.

                    RESPONSE
                    ---------------------
                    $generatedText

                    TIME
                    ---------------------
                    $elapsed ms

                    BASE MODEL
                    ---------------------
                    ${
                        try {
                            generativeModel.getBaseModelName()
                        } catch (e: Exception) {
                            "Unknown"
                        }
                    }

                    DEVICE
                    ---------------------
                    ${buildDeviceInfo()}
                """.trimIndent()

            } catch (e: Exception) {

                val elapsed = System.currentTimeMillis() - startTime

                resultText.text = buildErrorReport(
                    title = "INFERENCE TEST FAILED ❌",
                    error = e,
                    rawError = e.message ?: e.toString(),
                    elapsed = elapsed
                )
            }
        }
    }

    private fun buildErrorReport(
        title: String,
        error: Exception,
        rawError: String,
        elapsed: Long
    ): String {

        val diagnosis = diagnoseError(rawError)

        return """
            GEMINI NANO DIAGNOSTIC
            ======================

            RESULT: $title

            DIAGNOSIS
            ---------------------
            $diagnosis

            ERROR CLASS
            ---------------------
            ${error.javaClass.simpleName}

            RAW ERROR
            ---------------------
            $rawError

            TIME
            ---------------------
            $elapsed ms

            DEVICE
            ---------------------
            ${buildDeviceInfo()}
        """.trimIndent()
    }

    private fun diagnoseError(error: String): String {

        val lower = error.lowercase()

        return when {

            lower.contains("606-feature_not_found") ||
            lower.contains("feature_not_found") -> {

                """
                AICore is present and responding, but the
                requested GenAI feature is currently unavailable.

                Possible causes:

                • Device/model is not currently supported
                • AICore configuration has not finished updating
                • Required model configuration is unavailable
                • Firmware/carrier configuration differs
                • Feature rollout is not enabled
                • Device has not completed AICore initialization

                Recommended:

                1. Keep the device connected to the internet.
                2. Update AICore if an update is available.
                3. Restart the device.
                4. Wait and test again.

                The raw AICore error is preserved below.
                """.trimIndent()
            }

            lower.contains("601-binding_failure") ||
            lower.contains("binding_failure") -> {

                """
                AICore is installed but the application could
                not connect to the AICore service.

                Recommended:

                • Update AICore
                • Restart the device
                • Make sure AICore is enabled
                • Reinstall this tester if necessary
                """.trimIndent()
            }

            lower.contains("download_error") -> {

                """
                AICore appears to have encountered a model
                download problem.

                Recommended:

                • Check internet connectivity
                • Keep the device connected to Wi-Fi/mobile data
                • Update AICore
                • Retry after a few minutes
                """.trimIndent()
            }

            lower.contains("background_use_blocked") -> {

                """
                AICore rejected the request because the app
                was not considered the active foreground app.

                Bring this app to the foreground and retry.
                """.trimIndent()
            }

            lower.contains("busy") -> {

                """
                AICore is currently busy.

                Wait a few seconds and retry the inference.
                """.trimIndent()
            }

            else -> {

                """
                The GenAI request failed.

                The exact AICore error is preserved in this
                report so the failure can be investigated.

                No stronger diagnosis is being claimed because
                the returned error does not uniquely identify
                the cause.
                """.trimIndent()
            }
        }
    }

    private fun copyDiagnosticReport() {

        val clipboard =
            getSystemService(CLIPBOARD_SERVICE)
                as android.content.ClipboardManager

        val clip = android.content.ClipData.newPlainText(
            "Gemini Nano Diagnostic Report",
            resultText.text.toString()
        )

        clipboard.setPrimaryClip(clip)

        resultText.append(
            "\n\nDiagnostic report copied to clipboard ✅"
        )
    }

    override fun onDestroy() {

        try {
            generativeModel.close()
        } catch (_: Exception) {
        }

        super.onDestroy()
    }
}
