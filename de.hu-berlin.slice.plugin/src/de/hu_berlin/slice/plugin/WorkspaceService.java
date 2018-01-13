package de.hu_berlin.slice.plugin;

import javax.inject.Singleton;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * @author IShowerNaked
 * Service Class represents all the needed Information regarding the workspace.
 */
@Singleton
public class WorkspaceService {

	/**
	 * @param path
	 * @return
	 */
    public IPath getAbsolutePath(IPath path) {

        String absolutePathAsString = getAbsolutePathAsString(path);

        return Path.fromOSString(absolutePathAsString);
    }
    
    /**
     * @param path
     * @return
     */
    public String getAbsolutePathAsString(IPath path) {

        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

        IResource resource = workspaceRoot.findMember(path);
        if (null == resource) {
            return path.toOSString();
        }
        else {
            return resource.getLocation().toOSString();
        }
    }
}