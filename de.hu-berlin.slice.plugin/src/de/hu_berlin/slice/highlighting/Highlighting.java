package de.hu_berlin.slice.highlighting;

import java.util.List;
import java.util.Random;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;

//import de.hu_berlin.slice.plugin.view.file;


/**
 * Class determines which lines should be highlighted.
 */

public class Highlighting {

	IFile file;
	
	public Highlighting() {
		
		 file = (IFile) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor()
					.getEditorInput().getAdapter(IFile.class);
		 
	}
	
	public Highlighting(IEditorReference ex) throws PartInitException {
		file = (IFile )ex.getEditorInput().getAdapter(IFile.class);
	}
	
	
	/**
	 * Highlights the given Section of the marked text.
	 * @param textSelection 
	 * the selected text in the editor
	 * @throws CoreException
	 */
	public void HighlightSelected(ITextSelection textSelection) throws CoreException {

		int offset = textSelection.getOffset();
		int length = textSelection.getLength();
		MarkerFactory.createMarker(file, offset, length);
	}
	

	/**
	 * Highlights a Line according to a given line number.
	 * @param linenumber
	 * line number from the editor
	 * @throws CoreException
	 * @throws BadLocationException
	 */
	public void HighlightLine(int linenumber) throws CoreException, BadLocationException {

		IDocumentProvider provider = new TextFileDocumentProvider();
		provider.connect(file);
		IDocument  document = provider.getDocument(file);
		
		//-1 because the document starts counting lines at 0 and the editor starts at 1
		int offset = document.getLineOffset(linenumber - 1);
		int length = document.getLineLength(linenumber - 1);
		MarkerFactory.createMarker(file, offset, length);
	}
	
  
	/**
	 * Highlights random lines in the Editor.
	 * @throws CoreException
	 * @throws BadLocationException
	 */
	public void HighlightRandomLines() throws CoreException, BadLocationException {
		
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
	
	
	/**
	 * Deletes all the highlights and markers linked directly to the resource.
	 * @throws CoreException
	 */
	public void deleteMarkers() throws CoreException {
		
		List<IMarker> markers = MarkerFactory.findMarkers(file);
		for (IMarker marker : markers) {
			marker.delete();
		}
	}
	
	public void deleteAllMarkers() throws CoreException {
		
		List<IMarker> markers = MarkerFactory.findAllMarkers(file);
		for (IMarker marker : markers) {
			marker.delete();
		}
	}
}
