package com.aikido.gitleaksdetectorplugin;

import org.eclipse.ui.IStartup;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

public class Startup implements IStartup {

    @Override
    public void earlyStartup() {
        // Register the save listener when the Workbench starts
        ResourcesPlugin.getWorkspace().addResourceChangeListener(
            new SaveListener(), IResourceChangeEvent.POST_CHANGE);

        ILog log = Platform.getLog(Platform.getBundle("com.aikido.gitleaksdetectorplugin"));
        log.log(new Status(IStatus.INFO, "com.aikido.gitleaksdetectorplugin",
                "GitleaksDetectorPlugin: listener registered"));
    }
}