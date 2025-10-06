package com.aikido.gitleaksdetectorplugin;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public final class GitleaksLocator {

    public static String findBinary(Class<?> anyClassInBundle) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        String folder;
        if (os.contains("win")) folder = "windows-x64";
        else if (os.contains("mac") && arch.contains("aarch64")) folder = "macos-arm64";
        else if (os.contains("mac")) folder = "macos-x64";
        else folder = "linux-x64";

        String exe = os.contains("win") ? "gitleaks.exe" : "gitleaks";
        String relative = "gitleaks_binaries/" + folder + "/" + exe;

        Bundle bundle = FrameworkUtil.getBundle(anyClassInBundle);
        URL url = FileLocator.find(bundle, new Path(relative), null);
        if (url == null) throw new IllegalStateException("Bundled gitleaks not found: " + relative);

        URL fileUrl = FileLocator.toFileURL(url);      // extracted if inside JAR
        java.nio.file.Path path = Paths.get(fileUrl.toURI());

        try {  // make sure it’s executable on macOS/Linux
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("r-xr-xr-x");
            Files.setPosixFilePermissions(path, perms);
        } catch (UnsupportedOperationException ignore) {}

        return path.toAbsolutePath().toString();
    }
}