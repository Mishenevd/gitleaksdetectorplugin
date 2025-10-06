# 🧩 Gitleaks Detector Plugin for Eclipse

Gitleaks Detector automatically scans your files for secrets (tokens, keys, passwords, etc.) whenever you save a file in Eclipse.  
It integrates the [Gitleaks](https://github.com/gitleaks/gitleaks) CLI directly into your workspace — no external installation required.

---

## 🚀 Features
- Automatic scanning: Runs Gitleaks every time you save a file.  
- Instant feedback: Displays a popup if potential secrets are detected.  
- Self-contained: Bundles the Gitleaks binary for all major platforms — works out of the box.  
- Lightweight: No background jobs or manual setup required.

---

## 🧱 Installation

### Option 1: From Eclipse Marketplace (if published)
1. Open Eclipse → Help → Eclipse Marketplace...  
2. Search for “Gitleaks Detector”.  
3. Click Install and follow the prompts.  
4. Restart Eclipse.

### Option 2: From local update site (if distributing manually)
1. Go to Help → Install New Software...  
2. Click Add..., then:
   - **Name:** Gitleaks Detector  
   - **Location:** (path or URL to your update site ZIP or folder)
3. Select *Gitleaks Detector Plugin*, click Next, and finish installation.  
4. Restart Eclipse.

### Option 3: Drop-in installation (from this repository)
If you have the compiled plugin JAR (for testing or internal use):

1. **Locate your Eclipse installation folder.**  
   - On macOS (installed via Eclipse Installer):  
     ```
     ~/eclipse/java-*/eclipse/
     ```
   - On macOS (installed via .app):  
     ```
     /Applications/Eclipse.app/Contents/Eclipse/
     ```
2. **Create the `dropins` folder** if it doesn’t exist:
   ```bash
   mkdir -p ~/eclipse/java-2025-03/eclipse/dropins
   ```
3. **Copy the plugin JAR** from this repository’s root into the `dropins` folder:
   ```bash
   cp com.aikido.gitleaksdetectorplugin_1.0.0.jar ~/eclipse/java-2025-03/eclipse/dropins/
   ```
4. **Restart Eclipse.**
5. *(If the plugin doesn’t appear)* start Eclipse once with the `-clean` flag:
   ```bash
   /Applications/Eclipse.app/Contents/MacOS/eclipse -clean
   ```

6. **Verify installation:**  
   Go to  
   `Eclipse → About Eclipse IDE → Installation Details → Installed Software`  
   and confirm *Gitleaks Detector Plugin* is listed.

---

## 🧰 Platform support

This plugin includes prepackaged Gitleaks binaries:

| Platform | Architecture | Included Binary |
|-----------|---------------|----------------|
| Windows 10/11 | x64 | ✅ |
| macOS Intel | x64 | ✅ |
| macOS Apple Silicon | arm64 | ✅ |
| Linux (Ubuntu, Fedora, etc.) | x64 | ✅ |

*(Linux ARM64 support can be added by dropping the binary in `bin/linux-arm64/gitleaks`.)*

You don’t need to install Gitleaks manually — the plugin automatically extracts and runs the correct binary.

---

## ⚙️ Usage

1. Open or edit any file in your Eclipse workspace.  
2. When you save the file, the plugin automatically:
   - Runs the bundled Gitleaks scanner on that file.
   - Generates a temporary JSON report.
   - Shows a popup only if secrets are detected.

Example popup:

```
Gitleaks Results
File: /path/to/config.json

Secrets detected (2):

1. aws-access-key — Generic AWS Key
   at /path/to/config.json:14
   match: AKIA********************

2. slack-token — Slack Bot Token
   at /path/to/config.json:27
   match: xoxb-1234...
```

If no findings are detected → no popup.

---

## 🧩 How it works (for maintainers)

- The `SaveListener` class listens for Eclipse resource change events (`POST_CHANGE`).  
- When a file is saved:
  - The listener refreshes the resource to ensure the latest state.
  - The bundled Gitleaks binary (found via `GitleaksLocator`) runs with:
    ```bash
    gitleaks detect --no-git --exit-code=0 --no-banner -v \
      --report-format json \
      --report-path /tmp/report.json \
      --source=<saved-file>
    ```
  - JSON output is parsed and shown in a dialog.

- Binaries are located under `bin/<platform>/gitleaks`, packed into the plugin JAR, and extracted automatically at runtime.

---

## 🛠️ Updating Gitleaks

To update to a new version:

1. Download the latest binaries from [Gitleaks releases](https://github.com/gitleaks/gitleaks/releases).  
2. Extract only the `gitleaks` or `gitleaks.exe` file.
3. Replace the old binaries in:
   ```
   bin/windows-x64/gitleaks.exe
   bin/macos-x64/gitleaks
   bin/macos-arm64/gitleaks
   bin/linux-x64/gitleaks
   ```
4. Rebuild or export the plugin.

---

## ⚖️ License

This plugin and the bundled [Gitleaks](https://github.com/gitleaks/gitleaks) binaries are licensed under the MIT License.  
Include the Gitleaks license file in your distribution if you redistribute the binary.

---

## 🧪 Troubleshooting

- **No popup appears?**  
  Ensure your file was modified and saved. Only files with content changes trigger a scan.

- **Permission denied on macOS/Linux?**  
  The plugin automatically marks the binary executable, but on restrictive systems you may need to run Eclipse once with `sudo` or adjust file permissions.

---
