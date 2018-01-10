package de.hu_berlin.slice.highlighting;

import java.util.List;
import java.util.Random;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class Highlighting {

	public void HighlightSelected(ITextSelection textSelection) throws CoreException {

		IFile file = (IFile) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor()
				.getEditorInput().getAdapter(IFile.class);

		int offset = textSelection.getOffset();
		int length = textSelection.getLength();
		MarkerFactory.createMarker(file, offset, length);
	}
	
	public void HighlightLine(int linenumber) throws CoreException, BadLocationException {

		IFile file = (IFile) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor()
				.getEditorInput().getAdapter(IFile.class);
		
		IDocumentProvider provider = new TextFileDocumentProvider();
		provider.connect(file);
		IDocument  document = provider.getDocument(file);
		
		//-1 because the document starts counting lines at 0 and the editor starts at 1
		int offset = document.getLineOffset(linenumber - 1);
		int length = document.getLineLength(linenumber - 1);
		MarkerFactory.createMarker(file, offset, length);
	}
	
	public void HighlightRandomLines() throws CoreException, BadLocationException {

		IFile file = (IFile) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor()
				.getEditorInput().getAdapter(IFile.class);
		
		IDocumentProvider provider = new TextFileDocumentProvider();
		provider.connect(file);
		IDocument  document = provider.getDocument(file);
		
		Random r = new Random();
		int colorLineCount = r.nextInt(document.getNumberOfLines());
        
		// doesnt check if the line is already marked
		for (int i = 0; i < colorLineCount; i++) {
    		Random rr = new Random();
    		int colrThisLine = rr.nextInt(document.getNumberOfLines()) + 1;
    		HighlightLine(colrThisLine);
		}
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
