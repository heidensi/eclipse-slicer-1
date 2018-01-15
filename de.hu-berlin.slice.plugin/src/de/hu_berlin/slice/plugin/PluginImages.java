package de.hu_berlin.slice.plugin;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;

import de.hu_berlin.slice.Activator;

/**
 * Utility class to create the images shown in the UI.
 * @author IShowerNaked
 */
public class PluginImages
{
    public static final String PATH_TO_ICONS = "icons/";

    public static final ImageDescriptor DESC_CLEAR        = createImageDescriptor("clear.png");
    public static final ImageDescriptor DESC_RUN_BACKWARD = createImageDescriptor("run_backward.png");
    public static final ImageDescriptor DESC_RUN_FORWARD  = createImageDescriptor("run_forward.png");
    public static final ImageDescriptor DESC_UPDATE       = createImageDescriptor("update.png");

    /**
     * Creates the images shown in the UI.
     * @param fileName
     * @return
     */
    private static ImageDescriptor createImageDescriptor(String fileName) {
        String path = PATH_TO_ICONS + fileName;
        URL fileURL = FileLocator.find(Activator.getDefault().getBundle(), new Path(path), null);
        return ImageDescriptor.createFromURL(fileURL);
    }
}