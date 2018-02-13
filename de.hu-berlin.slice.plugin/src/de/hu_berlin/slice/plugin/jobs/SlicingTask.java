package de.hu_berlin.slice.plugin.jobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        	
        			List<NormalStatement> statements = getStatements(context);
                
                switch(context.sliceType) {
                		case backward:
                			//backwardSlice(statement, context);
                			backwardSlice(statements, context);
                			break;
                		case forward:
                			//forwardSlice(statement, context);
                			forwardSlice(statements, context);
                			break;
                		case thinBackward:
                			thinSlice(statements, context);
                			break;
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
    public List<NormalStatement> getStatements(SlicingContext context) throws InvalidClassFileException {
		List<CGNode> mainMethods = findMethods(context);
		List<NormalStatement> statements = null;
		for (CGNode mainNode : mainMethods) {
				statements = findStatements(mainNode, context.editorContext.getTextSelection().getStartLine()+1);
    			if (statements.isEmpty()) {
    				System.err.println("failed to find statement in the call graph. Please select a different statement");
    				continue;
    			}
		}
		return statements;
	}
	
	/**
	 * Iterates through the call graph to find the class and the methods the statement belongs to
	 * @param SlicingContext
	 * @return a List of all the main-methods
	 */
	public  List<CGNode> findMethods(SlicingContext context) {
		Atom aclassname = Atom.findOrCreateUnicodeAtom(stringSplit(context.editorContext.getTextEditor().getTitle())); //class the statement belongs to
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
     * Finds the selected statement inside the call graph     
     * @param n The call graph node of the main method
     * @param l the line number of the selected statement
     * @return a statement inside the call graph
     * @throws InvalidClassFileException 
     */
    public static List<NormalStatement> findStatements(CGNode n, int l) throws InvalidClassFileException {
        IR ir = n.getIR();
        IBytecodeMethod method = (IBytecodeMethod)ir.getMethod();
        List<com.ibm.wala.ipa.slicer.NormalStatement> list = new ArrayList<NormalStatement>();
        for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();) {
            SSAInstruction s = it.next();
                		int i = s.iindex;
                		int bc = method.getBytecodeIndex(i);
                    
                		if(method.getLineNumber(bc)== l) {
                			list.add(new com.ibm.wala.ipa.slicer.NormalStatement(n, i));
                		}
            
        }
        return list;
    }
        
    /**
     * Computes the forward slice and adds the line numbers to the slicing context.
     * @param statement
     * @param SlicingContext
     * @throws IllegalArgumentException
     * @throws CancelException
     */
    public void forwardSlice(List<NormalStatement> statements, SlicingContext context) throws IllegalArgumentException, CancelException {
		Collection<Statement> slice;
		Map<String, List<Integer>> k = new HashMap<String, List<Integer>>();;
		for(Statement statement : statements) {
			slice = Slicer.computeForwardSlice(statement, context.callGraph, context.pointerAnalysis, DataDependenceOptions.NO_BASE_PTRS, ControlDependenceOptions.NONE);
			Map<String, List<Integer>> m = dumpSlice(slice);
			k = merge(k,m);
		}
		k =removeDuplicates(k);
		context.map = k;
		debugLinenumbers(k);
    }
    
    /**
     * Computes the backward slice and adds the line numbers to the slicing context.
     * @param statement
     * @param SlicingContext
     * @throws IllegalArgumentException
     * @throws CancelException
     */    
    public void backwardSlice(List<NormalStatement> statements, SlicingContext context) throws IllegalArgumentException, CancelException {
		Collection<Statement> slice;
		Map<String, List<Integer>> k = new HashMap<String, List<Integer>>();;
		for(Statement statement : statements) {
			slice = Slicer.computeBackwardSlice(statement, context.callGraph, context.pointerAnalysis, DataDependenceOptions.NO_BASE_PTRS, ControlDependenceOptions.NONE);
			Map<String, List<Integer>> m = dumpSlice(slice);
			k = merge(k,m);
		}
		k =removeDuplicates(k);
		context.map = k;
		debugLinenumbers(k);
		
    }
    
    /**
     * Computes the thin backward slice and adds the line numbers to the slicing context.
     * @param statement
     * @param SlicingContext
     * @throws IllegalArgumentException
     * @throws CancelException
     */
    public void thinSlice(List<NormalStatement> statements, SlicingContext context) throws IllegalArgumentException, CancelException {
		Collection<Statement> slice;
		Map<String, List<Integer>> k = new HashMap<String, List<Integer>>();;
		for(Statement statement : statements) {
			ThinSlicer ts = new ThinSlicer(context.callGraph, context.pointerAnalysis);
			slice = ts.computeBackwardThinSlice( statement );
			Map<String, List<Integer>> m = dumpSlice(slice);
			k = merge(k,m);
		}
		k =removeDuplicates(k);
		context.map = k;
		debugLinenumbers(k);
		
    }
    
    /**
     * Returns the line numbers from the slice
     * @param slice
     * @return a Map of all the line numbers from the slice
     */
    public Map<String, List<Integer>> dumpSlice(Collection<Statement> slice) {
    	Map<String, List<Integer>> src = new HashMap<String, List<Integer>>();
        for (Statement s : slice) {
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
                        		 //System.err.println("Class: " +s.getNode().getMethod().getDeclaringClass().getName().getClassName() + " ;Line number: " + src_line_number);
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
        return src;
    }
    
    
    /**
     * Prints out all the line numbers from the slice
     * @param k
     */
    public void debugLinenumbers(Map<String,List<Integer>> m) {
    		System.err.println("Line numbers:");
    		for (Map.Entry<String, List<Integer>> entry : m.entrySet())
        {
            System.err.println(entry.getKey() + " " + entry.getValue());
        }
    }
    
    /**
     * Prints out all the line numbers and the corresponding class names from the slice
     * @param m
     */
    public void printLinenumbers(Map<String,List<Integer>> m){
    		for (Map.Entry<String, List<Integer>> entry : m.entrySet())
        {
            System.err.println("Class: " + entry.getKey() + " ; Line number: " + entry.getValue());
        }
    }
    
    
    /**
     * cuts off the type extension
     * @param String
     * @return
     */
    private String stringSplit(String s) {
    		String[] segs = s.split( Pattern.quote( "." ) );
    		return segs[0];
    }
    
    /**
     * Merges two maps into one, copies all the Integers from one list into the other if the key already exists.
     * @param map1
     * @param map2
     * @return
     */
    public Map<String, List<Integer>> merge(Map<String, List<Integer>> map1, Map<String, List<Integer>> map2){
    		for (Map.Entry<String, List<Integer>> entry : map2.entrySet())
    			{	
    			   if(map1.containsKey(entry.getKey())) {
    				   for(Integer i : map2.get(entry.getKey())) {
    					   map1.get(entry.getKey()).add(i);
    				   }
    			   }else {
    				   map1.put(entry.getKey(), entry.getValue());
    			   }
    			}
    			return map1;
    }
    
    /**
     * removes all duplicates in a Map
     * @param m
     * @return
     */
    public Map<String, List<Integer>> removeDuplicates(Map<String,List<Integer>> m) {
		for (Map.Entry<String, List<Integer>> entry : m.entrySet())
		{
			List<Integer> l = entry.getValue().stream()
			         .distinct()
			         .collect(Collectors.toList());
			m.put(entry.getKey(), l);
		}
		return m;
    	}
}