package com.aikido.gitleaksdetectorplugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.google.gson.*;

public class SaveListener implements IResourceChangeListener {

    private static final String PLUGIN_ID = "com.aikido.gitleaksdetectorplugin";

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        if (event.getType() != IResourceChangeEvent.POST_CHANGE) return;
        IResourceDelta root = event.getDelta();
        if (root == null) return;

        try {
            root.accept(delta -> {
                IResource res = delta.getResource();
                if (!(res instanceof IFile file)) return true;

                boolean contentChanged = (delta.getFlags() & IResourceDelta.CONTENT) != 0;
                if (!contentChanged) return true;

                Display.getDefault().asyncExec(() -> {
                    try {
                        file.refreshLocal(IResource.DEPTH_ZERO, null);
                        String abs = file.getLocation() != null ? file.getLocation().toOSString() : null;
                        if (abs != null) runGitleaksAndShow(abs);
                    } catch (Exception e) {
                        logError("Error refreshing file before scan", e);
                    }
                });
                return true;
            });
        } catch (CoreException e) {
            logError("resourceChanged failed", e);
        }
    }

    /** Runs gitleaks, parses the JSON, and shows a compact popup if there are findings. */
    private void runGitleaksAndShow(String filePath) {
        Path report = null;
        try {
        	final String gitleaks = GitleaksLocator.findBinary(SaveListener.class);
            report = Files.createTempFile("gitleaks-report-", ".json");
            report.toFile().deleteOnExit();

            List<String> cmd = List.of(
                gitleaks, "detect",
                "--no-git", "--exit-code=0", "--no-banner", "-v",
                "--report-format", "json",
                "--report-path", report.toString(),
                "--source=" + filePath
            );

            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                while (r.readLine() != null) { /* swallow tool output */ }
            }
            p.waitFor();

            if (!Files.exists(report) || Files.size(report) == 0) return;

            String json = Files.readString(report, StandardCharsets.UTF_8);
            String pretty = prettyFindings(filePath, json);
            if (pretty != null && !pretty.isBlank()) showPopup(filePath, pretty);

        } catch (Exception e) {
            logError("Gitleaks failed for " + filePath, e);
            showPopup(filePath, "Failed to run Gitleaks:\n" + e.getMessage());
        } finally {
            if (report != null) try { Files.deleteIfExists(report); } catch (Exception ignore) {}
        }
    }

    /** Returns a formatted findings summary, or null if no findings. */
    private String prettyFindings(String savedFile, String json) {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonArray() || root.getAsJsonArray().size() == 0) return null;

            JsonArray arr = root.getAsJsonArray();
            StringBuilder sb = new StringBuilder();
            sb.append("Secrets detected (").append(arr.size()).append("):\n\n");

            int idx = 1;
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject f = el.getAsJsonObject();

                String file = str(f, "File", savedFile);
                String rule = str(f, "RuleID", str(f, "Rule", "unknown-rule"));
                String desc = str(f, "Description", "");
                int line = intOr(f, "StartLine", -1);
                String match = truncateOneLine(str(f, "Match", str(f, "Secret", "")), 140);

                sb.append(idx++).append(". ").append(rule);
                if (!desc.isBlank()) sb.append(" — ").append(desc);
                sb.append("\n   at ").append(file);
                if (line >= 0) sb.append(":").append(line);
                if (!match.isBlank()) sb.append("\n   match: ").append(match);
                sb.append("\n\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            logError("JSON parse failed", e);
            return null;
        }
    }

    // ---- helpers ----

    private static String str(JsonObject o, String key, String def) {
        return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsString() : def;
    }

    private static int intOr(JsonObject o, String key, int def) {
        return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsInt() : def;
    }

    private static String truncateOneLine(String s, int max) {
        if (s == null) return "";
        String one = s.replace("\r", " ").replace("\n", " ").replaceAll("\\s+", " ").trim();
        return one.length() <= max ? one : one.substring(0, Math.max(0, max - 1)) + "…";
        }

    private void logError(String msg, Throwable t) {
        ILog log = Platform.getLog(Platform.getBundle(PLUGIN_ID));
        log.log(new Status(IStatus.ERROR, PLUGIN_ID, msg, t));
    }

    private void showPopup(String file, String output) {
        Display.getDefault().asyncExec(() -> {
            var window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window != null && !window.getShell().isDisposed()) {
                MessageDialog.openInformation(
                    window.getShell(),
                    "Gitleaks Results",
                    "File: " + file + "\n\n" + output
                );
            }
        });
    }
}