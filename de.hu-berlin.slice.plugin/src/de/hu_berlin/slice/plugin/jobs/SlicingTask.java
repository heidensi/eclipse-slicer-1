package de.hu_berlin.slice.plugin.jobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.thin.ThinSlicer;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

/**
 * Task to compute the slice. 
 * @author MartinEberlein
 */
public class SlicingTask implements ITask {
	
	@Override
    public void run(IProgressMonitor monitor, SlicingContext context) throws TaskException {
        monitor.subTask("computing the slice...");
        try {
        	
        			Statement statement = getStatement(context);
                if(context.sliceType == true) {
                		forwardSlice(statement, context);
                }else {
                		backwardSlice(statement, context);
                }
        }
        catch (Exception e) {
            throw new TaskException(null, e);
        }
        		monitor.done();
    		}
	
	
	/**
	 * Finds a Statement (from the SlicingContext) inside the call graph and returns it.
	 * TODO needs to find the selected statement!
	 * @param context
	 * @return the Statement
	 * @throws InvalidClassFileException 
	 */
	public Statement getStatement(SlicingContext context) throws InvalidClassFileException {
		List<CGNode> mainMethods = findMethods(context);
		Statement statement = null;
		
		for (CGNode mainNode : mainMethods) {
    				//statement = findCallTo(mainNode, "println"); //only searches for call instructions
    				statement = test(mainNode, context.editorContext.getTextSelection().getStartLine()+1);
    			if (statement == null) {
    				System.err.println("failed to find call to " + "println" + " in " + mainNode);
    				continue;
    			}
		}
		return statement;
	}
	
	/**
	 * Iterates through the call graph to find the methods the statement belongs to
	 * @param SlicingContext
	 * @return a List of all the main-methods
	 */
	public  List<CGNode> findMethods(SlicingContext context) {
		Atom aclassname = Atom.findOrCreateUnicodeAtom(ssplit(context.editorContext.getTextEditor().getTitle())); //class the statement belongs to
        Atom name = Atom.findOrCreateUnicodeAtom(context.editorContext.getMethodDeclaration().getName().toString()); //method the statement belongs to
        List<CGNode> result = new ArrayList<>();
        for (Iterator<? extends CGNode> it = context.callGraph.getSuccNodes(context.callGraph.getFakeRootNode()); it.hasNext();) {
            CGNode n = it.next();
            if(n.getMethod().getDeclaringClass().getName().getClassName() == aclassname) {
            	if (n.getMethod().getName().equals(name)) {
                    result.add(n);
                }
            }
            
        }
        if (result.isEmpty()) {
            Assertions.UNREACHABLE("failed to find method");
        }
        return result;
    }

    /**
     * Finds a call instruction inside the main method.
     * @param n The call graph node of the main method
     * @param methodName the method name of the call instruction
     * @return a statement inside the call graph
     */
    public static Statement findCallTo(CGNode n, String methodName) {
        IR ir = n.getIR();
        for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();) {
            SSAInstruction s = it.next();
            if (s instanceof com.ibm.wala.ssa.SSAAbstractInvokeInstruction) {
                com.ibm.wala.ssa.SSAAbstractInvokeInstruction call = (com.ibm.wala.ssa.SSAAbstractInvokeInstruction) s;
                //System.out.println(n.toString().substring(0, 20) + " "
                //      + call.getCallSite().getDeclaredTarget().getName().toString());
                if (call.getCallSite().getDeclaredTarget().getName().toString().equals(methodName)) {
      
                    com.ibm.wala.util.intset.IntSet indices = ir.getCallInstructionIndices(call.getCallSite());
                    com.ibm.wala.util.debug.Assertions.productionAssertion(indices.size() == 1,
                            "expected 1 but got " + indices.size());
                    return new com.ibm.wala.ipa.slicer.NormalStatement(n, indices.intIterator().next());
                }
            }
        }
        // Assertions.UNREACHABLE("failed to find call to " + methodName + " in " + n);
        return null;
    }
    
    /**
     *Test     
     * @param n The call graph node of the main method
     * @param methodName the method name of the call instruction
     * @return a statement inside the call graph
     * @throws InvalidClassFileException 
     */
    public static Statement test(CGNode n, int l) throws InvalidClassFileException {
        IR ir = n.getIR();
        IBytecodeMethod method = (IBytecodeMethod)ir.getMethod();
        for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();) {
            SSAInstruction s = it.next();
                		int i = s.iindex;
                		int bc = method.getBytecodeIndex(i);
                		//System.out.println("TEST Index "+ bc + "; line " + method.getLineNumber(bc));
                    
                		if(method.getLineNumber(bc)== l) {
                			/*SSAInstruction a = it.next();
                			if (a instanceof com.ibm.wala.ssa.SSAAbstractInvokeInstruction) {
                				System.out.println("DEMO");
                				com.ibm.wala.ssa.SSAAbstractInvokeInstruction call = (com.ibm.wala.ssa.SSAAbstractInvokeInstruction) a;
                				if (call.getCallSite().getDeclaredTarget().getName().toString().equals("println")) {
                				com.ibm.wala.util.intset.IntSet indices = ir.getCallInstructionIndices(call.getCallSite());
                                com.ibm.wala.util.debug.Assertions.productionAssertion(indices.size() == 1,
                                        "expected 1 but got " + indices.size());
                                return new com.ibm.wala.ipa.slicer.NormalStatement(n, indices.intIterator().next());
                				}
                			}*/
                			return new com.ibm.wala.ipa.slicer.NormalStatement(n, i);
                		}
            
        }
        //Assertions.UNREACHABLE("FEHLER" + n);
        return null;
    }
    
    /**
     * Computes the forward slice and adds the line numbers to the slicing context.
     * @param statement
     * @param SlicingContext
     * @throws IllegalArgumentException
     * @throws CancelException
     */
    public void forwardSlice(Statement statement, SlicingContext context) throws IllegalArgumentException, CancelException {
    		Collection<Statement> slice;
    		slice = Slicer.computeForwardSlice(statement, context.callGraph, context.pointerAnalysis, DataDependenceOptions.NO_BASE_PTRS, ControlDependenceOptions.NONE);
    		Map<String, List<Integer>> k = dumpSlice(slice);
    		//debugLinenumbers(k);
    		context.map = k;
    }
    
    /**
     * Computes the backward slice and adds the line numbers to the slicing context.
     * @param statement
     * @param SlicingContext
     * @throws IllegalArgumentException
     * @throws CancelException
     */
    public void backwardSlice(Statement statement, SlicingContext context) throws IllegalArgumentException, CancelException {
    		Collection<Statement> slice;
    		slice = Slicer.computeBackwardSlice(statement, context.callGraph, context.pointerAnalysis, DataDependenceOptions.NO_BASE_PTRS, ControlDependenceOptions.NONE);
    		Map<String, List<Integer>> k = dumpSlice(slice);
    		//debugLinenumbers(k);
    		context.map = k;
    }
    
    /**
     * Computes the thin backward slice and adds the line numbers to the slicing context.
     * @param statement
     * @param SlicingContext
     * @throws IllegalArgumentException
     * @throws CancelException
     */
    public void thinSlice(Statement statement, SlicingContext context) throws IllegalArgumentException, CancelException {
    		ThinSlicer ts = new ThinSlicer(context.callGraph, context.pointerAnalysis);
        Collection<Statement> slice = ts.computeBackwardThinSlice( statement );
        Map<String, List<Integer>> k = dumpSlice(slice);
        //debugLinenumbers(k);
		context.map = k;
    }
    
    /**
     * Returns the line numbers from the slice
     * @param slice
     * @return a List of all the line numbers from the slice
     */
    public static Map<String, List<Integer>> dumpSlice(Collection<Statement> slice) {
    	Map<String, List<Integer>> src = new HashMap<String, List<Integer>>();
    	List<Integer>src_test = new ArrayList<Integer>();
        for (Statement s : slice) {
            //System.err.println(s);
            if (s.getKind() == Statement.Kind.NORMAL) { // ignore special kinds of statements
                int bcIndex, instructionIndex = ((NormalStatement) s).getInstructionIndex();
                try {
                    bcIndex = ((ShrikeBTMethod) s.getNode().getMethod()).getBytecodeIndex(instructionIndex);
                    String key = s.getNode().getMethod().getDeclaringClass().getName().getClassName().toString();
                    try {
                    	
                        int src_line_number = s.getNode().getMethod().getLineNumber(bcIndex);
                        if(src.containsKey(key)) {
                        		src.get(key).add(src_line_number);
                        }else {
                        		List<Integer>a = new ArrayList<Integer>();
                        		a.add(src_line_number);
                        		src.put(key, a );
                        }
                        			//System.out.print(s.getNode().getMethod().getSignature());
                        			System.err.println("Class: " +s.getNode().getMethod().getDeclaringClass().getName().getClassName() + " ;Line number: " + src_line_number);
                        			src_test.add(src_line_number);
                    } catch (Exception e) {
                        // System.err.println("Bytecode index no good");
                        // System.err.println(e.getMessage());
                    }
                } catch (Exception e) {
                    // System.err.println("it's probably not a BT method (e.g. it's a fakeroot
                    // method)");
                    // System.err.println(e.getMessage());
                }
            }

        }
        for (Map.Entry<String, List<Integer>> entry : src.entrySet())
        {
            System.out.println(entry.getKey() + "/" + entry.getValue());
        }
        
        return src;
    }
    
    
    /**
     * Prints out all the line numbers from the slice
     * @param k
     */
    public void debugLinenumbers(List<Integer> k) {
    		for(int i : k) {
     		System.err.println("Line number: "+ i);
     	}
    }
    
    public String ssplit(String s) {
    		String[] segs = s.split( Pattern.quote( "." ) );
    		return segs[0];
    }
    
	}

