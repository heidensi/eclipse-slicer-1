package de.hu_berlin.slice.plugin.view;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.google.inject.Guice;
import com.google.inject.Injector;

import de.hu_berlin.slice.ast.ASTService;
import de.hu_berlin.slice.highlighting.Highlighting;
import de.hu_berlin.slice.plugin.AnalysisScopeFactory;
import de.hu_berlin.slice.plugin.BundleService;
import de.hu_berlin.slice.plugin.GuiceModule;
import de.hu_berlin.slice.plugin.PluginImages;
import de.hu_berlin.slice.plugin.ProjectService;
import de.hu_berlin.slice.plugin.WorkspaceService;
import de.hu_berlin.slice.plugin.context.EditorContextFactory;
import de.hu_berlin.slice.plugin.context.EditorContextFactory.EditorContext;
import de.hu_berlin.slice.plugin.jobs.JobFactory;
import de.hu_berlin.slice.plugin.jobs.SlicingContext;
import de.hu_berlin.slice.plugin.jobs.SlicingContext.sliceType;
import de.hu_berlin.slice.plugin.jobs.SlicingContext.optionsCD;
import de.hu_berlin.slice.plugin.jobs.SlicingContext.optionsData;
/**
 * Slice View
 * @author IShowerNaked
 */

public class SliceView extends ViewPart {


    /** The ID of the view as specified by the extension. */
    public static final String ID = "de.hu_berlin.slice.plugin.view.SliceView";

    @Inject
    IWorkbench workbench;

    // -------------------
    // -- DI stuff here --
    // -------------------

    Injector injector = Guice.createInjector(new GuiceModule());

    EditorContextFactory editorContextFactory = injector.getInstance(EditorContextFactory.class);
    ASTService           astService           = injector.getInstance(ASTService.class);
    BundleService        bundleService        = injector.getInstance(BundleService.class);
    ProjectService       projectService       = injector.getInstance(ProjectService.class);
    WorkspaceService     workspaceService     = injector.getInstance(WorkspaceService.class);
    AnalysisScopeFactory analysisScopeFactory = injector.getInstance(AnalysisScopeFactory.class);
    JobFactory           jobFactory           = injector.getInstance(JobFactory.class);

    // -------------------
    // -- UI stuff here --
    // -------------------

    private SourceViewer console;

    // @see configureActions()
    private Action clearViewAction;
    private Action refreshViewAction;
    private Action sliceForwardAction;
    private Action sliceBackwardAction;
    private Action sliceThinBackwardAction;
    private Action automaticClear;
    private Action optionsDDNONE;
    private Action optionsDDNO_BASE_PTRS;
    private Action optionsDDNO_BASE_NO_HEAP;
    private Action optionsDDNo_HEAP;
    private Action optionsDDREFLECTION;
    private Action optionsDDFULL;
    private Action optionsCDNONE;
    private Action optionsCDFULL;
    

    private static optionsCD optionsCD;
    private static optionsData optionsData;
    
    private String color =  "green";

    @Override
    public void createPartControl(Composite parent) {

        // we use a simple source viewer as a console for debug output
        console = new SourceViewer(parent, null, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);

        IDocument document = new Document();
        console.setDocument(document);

        configureActions();
        configureActionBars();
        
        optionsDDFULL.setChecked(true);
        automaticClear.setChecked(true);
        optionsData = optionsData.FULL;
        optionsCDFULL.setChecked(true);
        optionsCD = optionsCD.FULL;
        clearViewAction.run();
    }

    //
    // Set-up for action bar contributions
    //

    private void configureActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalPullDown(IMenuManager manager) {
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        manager.add(optionsDDNONE);
        manager.add(optionsDDNO_BASE_PTRS);
        manager.add(optionsDDNO_BASE_NO_HEAP);
        manager.add(optionsDDNo_HEAP);
        manager.add(optionsDDREFLECTION);
        manager.add(optionsDDFULL);
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        manager.add(optionsCDNONE);
        manager.add(optionsCDFULL);
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        manager.add(automaticClear);
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        manager.add(clearViewAction);
        manager.add(sliceThinBackwardAction);
        manager.add(refreshViewAction);
    }

    /**
     * Not yet in use.
     */
    @SuppressWarnings("unused")
    private void fillContextMenu(IMenuManager manager) {
        manager.add(clearViewAction);
        manager.add(refreshViewAction);
        // allow other plug-ins to add functionality here
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(sliceBackwardAction);
        manager.add(new Separator());
        manager.add(sliceForwardAction);
        manager.add(new Separator());
    }

    //Buttons get specified
    private void configureActions() {
    	
		optionsDDNONE = new Action(
	        "&DD NONE", IAction.AS_CHECK_BOX) {
	      public void run() {
	    	  	color = "purple";
	    	  	System.out.println("NONE");
	    	  	optionsDDNO_BASE_PTRS.setChecked(false);
	    	  	optionsDDFULL.setChecked(false);
	    	  	optionsDDNO_BASE_NO_HEAP.setChecked(false);
	    	  	optionsDDNo_HEAP.setChecked(false);
	    	  	optionsDDREFLECTION.setChecked(false);
	    	  	
	    	  	optionsDDNONE.setChecked(true);
	    	  	
	    	  	optionsData = optionsData.NONE;
	      }
	    };
	    
	    optionsDDNO_BASE_PTRS = new Action(
	        "&DD NO_BASE_PTRS", IAction.AS_CHECK_BOX) {
	      public void run() {
	    	  color = "red";
	    	  	System.out.println("NO_BASE_PTRS");
	    	  		optionsDDNONE.setChecked(false);
	    	  		optionsDDNO_BASE_NO_HEAP.setChecked(false);
  	    	  	optionsDDFULL.setChecked(false);
  	    	  	optionsDDNo_HEAP.setChecked(false);
  	    	  	optionsDDREFLECTION.setChecked(false);
  	    	  	
  	    	  optionsDDNO_BASE_PTRS.setChecked(true);
  	    	  
  	    	optionsData = optionsData.NO_BASE_PTRS;
	      }
	    };
	    
	    	optionsDDFULL = new Action(
      	        "&DD FULL", IAction.AS_CHECK_BOX) {
      	      public void run() {
      	    	  	color = "green";
      	    	  	System.out.println("FULL");
      	    	  	optionsDDNONE.setChecked(false);
          	    	optionsDDNO_BASE_PTRS.setChecked(false);
        	    	  	optionsDDNO_BASE_NO_HEAP.setChecked(false);
        	    	  	optionsDDNo_HEAP.setChecked(false);
        	    	  	optionsDDREFLECTION.setChecked(false);
        	    	  	
        	    	  	optionsDDFULL.setChecked(true);
        	    	  	
        	    	  	optionsData = optionsData.FULL;
      	      }
      	    };
   
      	    
    optionsDDNO_BASE_NO_HEAP = new Action(
      	        "&DD NO_BASE_NO_HEAP", IAction.AS_CHECK_BOX) {
      	      public void run() {
      	    	  	color = "blue";
      	    	  	System.out.println("NO_BASE_NO_HEAP");
      	    	  	optionsDDNONE.setChecked(false);
          	    	optionsDDNO_BASE_PTRS.setChecked(false);
          	    	optionsDDFULL.setChecked(false);
        	    	  	optionsDDNo_HEAP.setChecked(false);
        	    	  	optionsDDREFLECTION.setChecked(false);
        	    	  	
        	    	  	optionsDDNO_BASE_NO_HEAP.setChecked(true);
        	    	  	
        	    	  	optionsData = optionsData.NO_BASE_NO_HEAP;
      	      }
    	};
    
    	optionsDDNo_HEAP = new Action(
  	        "&DD NO_HEAP", IAction.AS_CHECK_BOX) {
  	      public void run() {
  	    	  color = "yellow";
  	    	  System.out.println("NO_HEAP");
  	    	  optionsDDNONE.setChecked(false);
  	    	  optionsDDNO_BASE_PTRS.setChecked(false);
  	    	  optionsDDFULL.setChecked(false);
  	    	  optionsDDNO_BASE_NO_HEAP.setChecked(false);
  	    	  optionsDDREFLECTION.setChecked(false);
  	    	  
  	    	optionsDDNo_HEAP.setChecked(true);
  	    	  
  	    	optionsData = optionsData.NO_HEAP;
  	      }
    	};

		optionsDDREFLECTION = new Action(
	        "&DD REFLECTION", IAction.AS_CHECK_BOX) {
	      public void run() {
	    	  	color = "orange";
	    	  	System.out.println("REFLECTION");
	    	  	optionsDDNONE.setChecked(false);
	  		optionsDDNO_BASE_PTRS.setChecked(false);
	  		optionsDDFULL.setChecked(false);
	  		optionsDDNO_BASE_NO_HEAP.setChecked(false);
	  		optionsDDNo_HEAP.setChecked(false);
	  		
	  		optionsDDREFLECTION.setChecked(true);
	  		
	  		optionsData = optionsData.REFLECTION;
	      }
		};
      	    
      	   
	    optionsCDNONE = new Action(
  	        "&CD NONE", IAction.AS_CHECK_BOX) {
	      public void run() {
	    	  	System.out.println("NONE");
	    	  	optionsCDFULL.setChecked(false);
	    	  	
	    	  	optionsCDNONE.setChecked(true);
	    	  	optionsCD = optionsCD.NONE;
	    	  	
	      }
	    };
	    
	    
	    optionsCDFULL = new Action(
      	        "&CD FULL", IAction.AS_CHECK_BOX) {
    	      public void run() {
    	    	  	System.out.println("FULL");
    	    	  	optionsCDNONE.setChecked(false);
    	    	  	
    	    	  	optionsCDFULL.setChecked(true);
    	    	  	
    	    	  	optionsCD = optionsCD.FULL;
    	      }
    	    };
    	    
    	 automaticClear = new Action(
          	        "&Automatic Clear", IAction.AS_CHECK_BOX) {
        	      public void run() {
        	      }
        	    };

// Action to clear the view
clearViewAction = createAction(
		"Clear", "Clears the slice result view.", PluginImages.DESC_CLEAR,
		new Action() {
			@Override
			public void run() {
				IEditorReference[] editors =PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
				for(IEditorReference ex : editors) {
					Highlighting h;
					try {
						h = new Highlighting(ex);
						h.deleteAllMarkers();
					} catch (PartInitException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (CoreException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
		});

// Action to refresh the view.
refreshViewAction = createAction(
		"Refresh", "Refreshes the slice result view.", PluginImages.DESC_UPDATE,
		createPlaceholderAction("Refresh view button clicked!"));
// enable refresh on pressing F5 etc.
refreshViewAction.setActionDefinitionId(IWorkbenchCommandConstants.FILE_REFRESH);

// Action to perform backward slice
sliceBackwardAction = createAction(
		"Slice backward", "Performs a backward slice.", PluginImages.DESC_RUN_BACKWARD,
		new Action() {
			@Override
			public void run() {
				if(automaticClear.isChecked()) {
				clearViewAction.run();
				}
				slice(sliceType.backward);
			}
		});

// Action to perform thin backward slice
sliceThinBackwardAction = createAction(
		"Slice backward (thin)", "Performs a thin backward slice.", PluginImages.DESC_RUN_BACKWARD,
		new Action() {
			@Override
			public void run() {
				if(automaticClear.isChecked()) {
    				clearViewAction.run();
    				}
				slice(sliceType.thinBackward);
			}
		});

// Action to perform forward slice
sliceForwardAction = createAction(
		"Slice forward", "Performs a forward slice.", PluginImages.DESC_RUN_FORWARD,
		new Action() {
			@Override
			public void run() {
				if(automaticClear.isChecked()) {
    				clearViewAction.run();
    				}
				slice(sliceType.forward);
			}
		});
sliceForwardAction.setText("Slice forward");
sliceForwardAction.setToolTipText("Performs a forward slice.");
sliceForwardAction.setImageDescriptor(PluginImages.DESC_RUN_FORWARD);

}

	private Action createAction(String text, String toolTipText, ImageDescriptor imageDescriptor, Action action) {
		action.setText(text);
		action.setToolTipText(toolTipText);
		action.setImageDescriptor(imageDescriptor);
		return action;
	}

    /**
	 * demo for slicing
     */
    private void slice(sliceType sliceType) {

        List<String> out = new ArrayList<>();

        try {

            EditorContext editorContext = editorContextFactory.create(workbench);


            ITextSelection    textSelection     = editorContext.getTextSelection();
            ICompilationUnit  compilationUnit   = editorContext.getCompilationUnit();
            IJavaProject      javaProject       = editorContext.getJavaProjectContext().getJavaProject();
            ASTNode           ast               = editorContext.getAST();
            Statement         statementNode     = editorContext.getStatementNode();
            MethodDeclaration methodDeclaration = editorContext.getMethodDeclaration();

            out.add("Compilation unit: "                 + compilationUnit.getSource());
            out.add("Text selected: "                    + textSelection.getText());
            out.add("- offset: "                         + textSelection.getOffset());
            out.add("- length: "                         + textSelection.getLength());
            out.add("Project name: "                     + javaProject.getElementName());
            out.add("Statement selected: "               + statementNode.toString());
            out.add("Statement offset: "                 + statementNode.getStartPosition());
            out.add("Statement length: "                 + statementNode.getLength());
            out.add("Method this statement belongs to: " + methodDeclaration.toString());

            Highlighting h = new Highlighting();
            h.HighlightSelected(textSelection);
            IEditorReference[] editors =PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
            SlicingContext slicingContext = new SlicingContext(editorContext, sliceType);
            slicingContext.setOptionsCD(optionsCD);
            slicingContext.setOptionsData(optionsData);

            Job mainJob = jobFactory.create(slicingContext);
            mainJob.addJobChangeListener(new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    for(IEditorReference ex : editors) {
							//System.out.println(ex.getTitle());
							String s = stringSplit(ex.getTitle());
							//System.out.println(s);

							if(slicingContext.getMap().containsKey(s)) {
								for(int i :slicingContext.getMap().get(s)) {
									try {
										Highlighting g = new Highlighting(ex);
										g.HighlightLine(i, color);
									} catch (CoreException | BadLocationException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							}
						}
                }
            });
            mainJob.schedule();


        }
        catch (Exception e) {
            /*
             * for debugging purposes
             *
             *out.add("-- An error occured! --\n");
             *out.add("message: " + e.getMessage());
             *out.add("class: " + e.getClass().getName());
             *out.add("stacktrace: " + Throwables.getStackTraceAsString(e));
             */

            String message = e.getMessage();
            if (message == null)
        	message = "The text selection does not belong to a statement node.";
            Shell activeShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            MessageDialog.openError(activeShell, "An error occured", message);
        }


        console.getDocument().set(String.join("\n", out));
    }

    private void alert(String msg) {
        MessageDialog.openInformation(console.getControl().getShell(), "Slice View", msg);
    }

    private Action createPlaceholderAction(String msg) {
        return new Action() {
            @Override
            public void run() {
                alert(msg);
            }
        };
    }

    @Override
    public void setFocus() {
        console.getControl().setFocus();
    }

    /**
     * cuts off the type extension
     * @param s
     * @return
     */
    public String stringSplit(String s) {
		String[] segs = s.split( Pattern.quote( "." ) );
		return segs[0];
}
}