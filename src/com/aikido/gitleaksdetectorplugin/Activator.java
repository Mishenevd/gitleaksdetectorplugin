package com.aikido.gitleaksdetectorplugin;

import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.eclipse.core.resources.ResourcesPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "com.aikido.gitleaksdetectorplugin";
	private static Activator instance;
	private SaveListener listener;

	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		instance = this;

		// Register listener for file save events
		listener = new SaveListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(listener,
				org.eclipse.core.resources.IResourceChangeEvent.POST_CHANGE);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (listener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
			listener = null;
		}
		instance = null;
		super.stop(context);
	}

	public static Activator getDefault() {
		return instance;
	}
}