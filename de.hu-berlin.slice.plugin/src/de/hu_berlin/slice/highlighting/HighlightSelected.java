package de.hu_berlin.slice.highlighting;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.PlatformUI;

public class HighlightSelected {

	public void Highlight(ITextSelection textSelection) throws CoreException {

		IFile file = (IFile) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor()
				.getEditorInput().getAdapter(IFile.class);

		int offset = textSelection.getOffset();
		int length = textSelection.getLength();
		MarkerFactory.createMarker(file, offset, length);
	}

	public void deleteMarkers() throws CoreException {
		
		IFile file = (IFile) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor()
				.getEditorInput().getAdapter(IFile.class);

		List<IMarker> markers = MarkerFactory.findMarkers(file);
		for (IMarker marker : markers) {
			marker.delete();
		}
	}
}
