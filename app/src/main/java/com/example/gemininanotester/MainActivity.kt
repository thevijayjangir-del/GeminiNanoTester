package com.example.gemininanotester

import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
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

        val scrollView = ScrollView(this)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val title = TextView(this).apply {
            text = "Gemini Nano Tester"
            textSize = 28f
            setPadding(0, 0, 0, 24)
        }

        val device = TextView(this).apply {
            text = buildDeviceInfo()
            textSize = 16f
            setPadding(0, 0, 0, 24)
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

        val resultTitle = TextView(this).apply {
            text = "RESULT"
            textSize = 20f
            setPadding(0, 24, 0, 8)
        }

        resultText = TextView(this).apply {
            text = "Press CHECK NANO / AICORE to begin."
            textSize = 17f
            setPadding(0, 8, 0, 32)
        }

        content.addView(title)
        content.addView(device)
        content.addView(checkButton)
        content.addView(testButton)
        content.addView(copyButton)
        content.addView(resultTitle)
        content.addView(resultText)

        scrollView.addView(content)
        root.addView(scrollView)

        setContentView(root)

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

        val aicoreInstalled = try {
            packageManager.getPackageInfo(
                "com.google.android.aicore",
                0
            )
            true
        } catch (_: Exception) {
            false
        }

        return """
            DEVICE

            Manufacturer: ${Build.MANUFACTURER}
            Model: ${Build.MODEL}
            Device: ${Build.DEVICE}
            Product: ${Build.PRODUCT}

            Android: ${Build.VERSION.RELEASE}
            API: ${Build.VERSION.SDK_INT}

            Build: ${Build.DISPLAY}
            Security patch: ${Build.VERSION.SECURITY_PATCH}

            AICore package: ${
                if (aicoreInstalled) "INSTALLED ✅" else "NOT DETECTED ❌"
            }
        """.trimIndent()
    }

    private fun checkNano() {

        resultText.text = "Checking AICore / Gemini Nano..."

        lifecycleScope.launch {

            val startTime = SystemClock.elapsedRealtime()

            try {

                val status = generativeModel.checkStatus()

                val elapsed =
                    SystemClock.elapsedRealtime() - startTime

                val statusDescription = when (status) {

                    FeatureStatus.AVAILABLE ->
                        """
                        AVAILABLE ✅

                        Gemini Nano is available for this Prompt API configuration.

                        You can attempt local inference.
                        """.trimIndent()

                    FeatureStatus.DOWNLOADABLE ->
                        """
                        DOWNLOADABLE 🟡

                        Gemini Nano is supported for this configuration,
                        but the required model is not currently downloaded.

                        A download may be required before inference.
                        """.trimIndent()

                    FeatureStatus.DOWNLOADING ->
                        """
                        DOWNLOADING 🟡

                        Gemini Nano is currently being downloaded.
                        Try again after the download completes.
                        """.trimIndent()

                    FeatureStatus.UNAVAILABLE ->
                        """
                        UNAVAILABLE ❌

                        Gemini Nano is not currently available for
                        this Prompt API configuration on this device.
                        """.trimIndent()

                    else ->
                        """
                        UNKNOWN ⚠️

                        Returned status:
                        $status
                        """.trimIndent()
                }

                var modelName = ""

                if (status == FeatureStatus.AVAILABLE) {

                    modelName = try {
                        "\n\nBase model:\n${generativeModel.getBaseModelName()}"
                    } catch (e: Exception) {
                        "\n\nBase model:\nUnable to retrieve\n${e.message}"
                    }
                }

                val report = """
                    GEMINI NANO DIAGNOSTIC
                    =====================

                    $statusDescription

                    STATUS:
                    $status

                    CHECK TIME:
                    ${elapsed} ms

                    DEVICE
                    ---------------------
                    ${buildDeviceInfo()}
                    $modelName
                """.trimIndent()

                lastReport = report
                resultText.text = report

            } catch (e: Exception) {

                val elapsed =
                    SystemClock.elapsedRealtime() - startTime

                val rawError =
                    e.message ?: "No error message"

                val diagnosis =
                    diagnoseError(rawError)

                val report = """
                    GEMINI NANO DIAGNOSTIC
                    =====================

                    RESULT: CHECK FAILED ❌

                    DIAGNOSIS
                    ---------------------
                    $diagnosis

                    ERROR CLASS
                    ---------------------
                    ${e.javaClass.simpleName}

                    RAW ERROR
                    ---------------------
                    $rawError

                    CHECK TIME
                    ---------------------
                    ${elapsed} ms

                    DEVICE
                    ---------------------
                    ${buildDeviceInfo()}
                """.trimIndent()

                lastReport = report
                resultText.text = report
            }
        }
    }

    private fun runNanoTest() {

        resultText.text =
            "Running local Gemini Nano inference..."

        lifecycleScope.launch {

            val startTime = SystemClock.elapsedRealtime()

            try {

                val status = generativeModel.checkStatus()

                if (status != FeatureStatus.AVAILABLE) {

                    val report = """
                        INFERENCE TEST
                        ===============

                        RESULT: NOT AVAILABLE ❌

                        Gemini Nano is not currently ready
                        for inference.

                        STATUS:
                        $status

                        No inference request was sent.
                    """.trimIndent()

                    lastReport = report
                    resultText.text = report

                    return@launch
                }

                val response = generativeModel.generateContent(
                    "Reply with exactly: GEMINI NANO LOCAL TEST PASS"
                )

                val elapsed =
                    SystemClock.elapsedRealtime() - startTime

                val report = """
                    INFERENCE TEST
                    ===============

                    RESULT: SUCCESS ✅

                    LOCAL INFERENCE COMPLETED

                    Response object:
                    $response

                    Inference time:
                    ${elapsed} ms

                    Device:
                    ${Build.MANUFACTURER} ${Build.MODEL}
                """.trimIndent()

                lastReport = report
                resultText.text = report

            } catch (e: Exception) {

                val elapsed =
                    SystemClock.elapsedRealtime() - startTime

                val rawError =
                    e.message ?: "No error message"

                val diagnosis =
                    diagnoseError(rawError)

                val report = """
                    INFERENCE TEST
                    ===============

                    RESULT: FAILED ❌

                    DIAGNOSIS
                    ---------------------
                    $diagnosis

                    ERROR CLASS
                    ---------------------
                    ${e.javaClass.simpleName}

                    RAW ERROR
                    ---------------------
                    $rawError

                    TIME
                    ---------------------
                    ${elapsed} ms

                    DEVICE
                    ---------------------
                    ${buildDeviceInfo()}
                """.trimIndent()

                lastReport = report
                resultText.text = report
            }
        }
    }

    private fun diagnoseError(error: String): String {

        val lower = error.lowercase()

        return when {

            lower.contains("606-feature_not_found") ||
            lower.contains("feature_not_found") ->

                """
                AICore is present, but the requested GenAI
                feature is not currently exposed.

                Possible causes:

                • Device/model is not supported for this API
                • AICore configuration has not updated yet
                • Required model configuration is unavailable
                • Device firmware/carrier configuration differs
                • Developer-preview feature is not enabled

                Recommended:

                1. Make sure the phone has internet access.
                2. Update AICore if an update is available.
                3. Restart the phone.
                4. Wait and test again.

                The raw AICore error is preserved below.
                """.trimIndent()

            lower.contains("binding_failure") ->

                """
                AICore service could not be reached.

                AICore may not have initialized correctly.

                Recommended:

                • Update AICore
                • Restart the device
                • Make sure Google system components are up to date
                """.trimIndent()

            lower.contains("download_error") ||
            lower.contains("unable to resolve host") ->

                """
                AICore could not download required resources.

                Check the internet connection and try again.
                """.trimIndent()

            lower.contains("background_use_blocked") ->

                """
                The GenAI API was called while the application
                was not considered the foreground application.

                Run the test while this app is visible.
                """.trimIndent()

            lower.contains("busy") ->

                """
                AICore is currently busy.

                Wait a moment and try again.
                """.trimIndent()

            else ->

                """
                An unclassified GenAI/AICore error occurred.

                The raw error below should be used for
                further investigation.
                """.trimIndent()
        }
    }

    private fun copyDiagnosticReport() {

        if (lastReport.isBlank()) {
            resultText.text =
                "Run a diagnostic first, then copy the report."
            return
        }

        val clipboard =
            getSystemService(Context.CLIPBOARD_SERVICE)
                    as ClipboardManager

        val clip =
            android.content.ClipData.newPlainText(
                "Gemini Nano Diagnostic",
                lastReport
            )

        clipboard.setPrimaryClip(clip)

        resultText.text =
            lastReport +
            "\n\n✅ Diagnostic report copied to clipboard."
    }

    override fun onDestroy() {

        try {
            generativeModel.close()
        } catch (_: Exception) {
        }

        super.onDestroy()
    }
}
