# Gemini Nano Tester — GitHub APK Builder

No Android Studio required.

1. Create a GitHub repository.
2. Upload all files/folders from this project.
3. Push to the `main` branch.
4. Open the repository's **Actions** tab.
5. Select **Build Gemini Nano Tester APK** if needed.
6. Wait for the workflow to finish.
7. Open the completed run and download the artifact named **GeminiNanoTester-debug-apk**.
8. Extract it and install `app-debug.apk` on the POCO X8 Pro and Galaxy S24 FE.

The app checks Google's ML Kit GenAI Prompt API at runtime and attempts a real Prompt API inference.

Important: a successful build does not mean the device supports Nano. The runtime status shown by the app is the result that matters.
