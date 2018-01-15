package de.hu_berlin.slice.highlighting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.PlatformUI;

/**
 * Class to create a highlighted line and the corresponding marker on the left side 
 */

public class MarkerFactory {

	public static final String MARKER = "de.hu_berlin.slice.marker.slicer";
	
	/**
	 * Creates a new marker type.
	 * @param res 
	 * active editor window.
	 * @return marker
	 * @throws CoreException
	 */

	public static IMarker createMarker(IResource res) throws CoreException {
		IMarker marker = null;
		marker = res.createMarker("de.hu_berlin.slice.marker.slicer"); //ID from the plugin.xml
		marker.setAttribute("description", "this is one of my markers");
		marker.setAttribute(IMarker.MESSAGE, "My Marker");
		return marker;
	}
	
	/**
	 * Adds a single marker to the given line number.
	 * @param res
	 * active editor window.
	 * @param linenumber
	 * a line number from the editor.
	 * @return marker
	 * @throws CoreException
	 */
	public static IMarker createMarker(IResource res, int linenumber) throws CoreException {
		IMarker marker = null;
		marker = res.createMarker("de.hu_berlin.slice.marker.slicer");
		marker.setAttribute("description", "this is one of my markers");
		marker.setAttribute(IMarker.MESSAGE, "My Marker");
		marker.setAttribute(IMarker.LINE_NUMBER, linenumber);
		return marker;
	}
	
	/**
	 * Highlights a line and adds a marker.
	 * @param res
	 * active editor window
	 * @param offset
	 * the indenting of the statement
	 * @param length
	 * of the statement
	 * @return marker
	 * @throws CoreException
	 */
	public static IMarker createMarker(IResource res, int offset, int length) throws CoreException {
		IMarker marker = null;
		marker = res.createMarker("de.hu_berlin.slice.marker.slicer");
		marker.setAttribute("description", "this is one of my markers");
		marker.setAttribute(IMarker.MESSAGE, "My Marker");
		marker.setAttribute(IMarker.CHAR_START, offset);
		marker.setAttribute(IMarker.CHAR_END, offset + length);
		return marker;
	}

	/**
	 * Finds all the markers directly linked to the resource.
	 * @param resource
	 * active editor window
	 * @return a list of all the markers
	 */
	public static List<IMarker> findMarkers(IResource resource) {
		try {
			return Arrays.asList(resource.findMarkers(MARKER, true, IResource.DEPTH_ZERO));
		} catch (CoreException e) {
			return new ArrayList<IMarker>();
		}
	}
	
	/**
	 * Finds all the markers related to this resource or sub-resource.
	 * @param resource
	 * active editor window
	 * @return a list of all the markers
	 */
	public static List<IMarker> findAllMarkers(IResource resource) {
		try {
			return Arrays.asList(resource.findMarkers(MARKER, true, IResource.DEPTH_INFINITE));
		} catch (CoreException e) {
			return new ArrayList<IMarker>();
		}
	}
	
	/**
	 * Returns the selection of the package explorer.
	 * @return selection
	 */
	public static TreeSelection getTreeSelection() {

		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService()
				.getSelection();
		if (selection instanceof TreeSelection) {
			return (TreeSelection) selection;
		}
		return null;
	}
}
