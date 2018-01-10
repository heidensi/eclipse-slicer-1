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

public class MarkerFactory {

	public static final String MARKER = "com.ibm.mymarkers.mymarker";

	//creating the new marker type
	public static IMarker createMarker(IResource res) throws CoreException {
		IMarker marker = null;
		marker = res.createMarker("com.ibm.mymarkers.mymarker");
		marker.setAttribute("description", "this is one of my markers");
		marker.setAttribute(IMarker.MESSAGE, "My Marker");
		return marker;
	}

	//does not color the line, just adds a marker on the left side
	public static IMarker createMarker(IResource res, int linenumber) throws CoreException {
		IMarker marker = null;
		marker = res.createMarker("com.ibm.mymarkers.mymarker");
		marker.setAttribute("description", "this is one of my markers");
		marker.setAttribute(IMarker.MESSAGE, "My Marker");
		marker.setAttribute(IMarker.LINE_NUMBER, linenumber);
		return marker;
	}

	//Highlights a line and adds a marker
	public static IMarker createMarker(IResource res, int offset, int length) throws CoreException {
		IMarker marker = null;
		marker = res.createMarker("com.ibm.mymarkers.mymarker");
		marker.setAttribute("description", "this is one of my markers");
		marker.setAttribute(IMarker.MESSAGE, "My Marker");
		marker.setAttribute(IMarker.CHAR_START, offset);
		marker.setAttribute(IMarker.CHAR_END, offset + length);
		return marker;
	}

	//finds the markers directly linked with that resource
	public static List<IMarker> findMarkers(IResource resource) {
		try {
			return Arrays.asList(resource.findMarkers(MARKER, true, IResource.DEPTH_ZERO));
		} catch (CoreException e) {
			return new ArrayList<IMarker>();
		}
	}

	//finds all markers related to this or any sub-resources
	public static List<IMarker> findAllMarkers(IResource resource) {
		try {
			return Arrays.asList(resource.findMarkers(MARKER, true, IResource.DEPTH_INFINITE));
		} catch (CoreException e) {
			return new ArrayList<IMarker>();
		}
	}

    //Returns the selection of the package explorer
	public static TreeSelection getTreeSelection() {

		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService()
				.getSelection();
		if (selection instanceof TreeSelection) {
			return (TreeSelection) selection;
		}
		return null;
	}
}
