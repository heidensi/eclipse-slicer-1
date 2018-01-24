package de.hu_berlin.slice.plugin.jobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.thin.ThinSlicer;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAInstruction;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

import de.hu_berlin.slice.plugin.context.EditorContextFactory.EditorContext;
import de.hu_berlin.slice.highlighting.Highlighting;

/**
 * Task to compute the slice. 
 * @author MartinEberlein
 */
public class SlicingTask implements ITask {
	
	@Override
    public void run(IProgressMonitor monitor, SlicingContext context) throws TaskException {
        monitor.subTask("computing the slice...");
        
        try {

            List<CGNode> mainMethods = findMainMethods(context.callGraph);
            
            //LISTEN FÜR DIE ZEILEN
            List<Integer> k = new ArrayList<Integer>();
            
            
            
            for (CGNode mainNode : mainMethods) {
            		System.out.println(mainNode.toString()+ " main method");
                Statement statement = findCallTo(mainNode, "println");
               // testStatement(mainNode);
                
                if (statement == null) {
                    System.err.println("failed to find call to " + "println" + " in " + mainNode);
                    continue;
                }
                
                if(context.sliceType == true) {
                		//compute forward Slice
                		Collection<Statement> slice;
                		slice = Slicer.computeForwardSlice(statement, context.callGraph, context.pointerAnalysis, DataDependenceOptions.NO_BASE_PTRS, ControlDependenceOptions.NONE);
                 	k = dumpSlice(slice);
                 	System.out.println("Zurückgegebene Zeilennummern: forward Slice");
                 	for(int i : k) {
                 		System.out.println("Line number: "+ i);
                 	}	
                 	context.list = k;
                }else {
                		//compute backward Slice
            			Collection<Statement> slice;
            			slice = Slicer.computeBackwardSlice(statement, context.callGraph, context.pointerAnalysis, DataDependenceOptions.NO_BASE_PTRS, ControlDependenceOptions.NONE);
            			k = dumpSlice(slice);
            			System.out.println("Zurückgegebene Zeilennummern: backward Slice");
            				for(int i : k) {
            					System.out.println("Line number: "+ i);
            				}
            			context.list = k;
                }
                 /*
              // context-sensitive thin slice
                 ThinSlicer ts = new ThinSlicer(context.callGraph, context.pointerAnalysis);
                 Collection<Statement> slice1 = ts.c ( statement );
                 l = dumpSlice(slice1);
                */
            }
        	
        }
        catch (Exception e) {
            throw new TaskException(null, e);
        }

        		monitor.done();
    		}
	
	public static List<CGNode> findMainMethods(CallGraph cg) {
        Atom name = Atom.findOrCreateUnicodeAtom("main");
        List<CGNode> result = new ArrayList<>();
        for (Iterator<? extends CGNode> it = cg.getSuccNodes(cg.getFakeRootNode()); it.hasNext();) {
            CGNode n = it.next();
            if (n.getMethod().getName().equals(name)) {
                result.add(n);
            }
        }

        if (result.isEmpty()) {
            Assertions.UNREACHABLE("failed to find main() method");
        }

        return result;
    }
    
	//Test
    public static void testStatement(CGNode n) {
    	IR ir = n.getIR();
        for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();) {
            SSAInstruction s = it.next();
            if (s instanceof com.ibm.wala.ssa.SSAAbstractInvokeInstruction) {
	            com.ibm.wala.ssa.SSAAbstractInvokeInstruction call = (com.ibm.wala.ssa.SSAAbstractInvokeInstruction) s;
	            //System.out.println("Komischer string " + n.toString().substring(0, 20) + " "
	                    + call.getCallSite().getDeclaredTarget().getName().toString());
	            //System.out.println(call.getCallSite().getDeclaredTarget().getName().toString());
            }
            if(s instanceof com.ibm.wala.ssa.SSAAbstractBinaryInstruction) {
        			com.ibm.wala.ssa.SSAAbstractBinaryInstruction bin = (com.ibm.wala.ssa.SSAAbstractBinaryInstruction) s;
        			System.out.println(bin.getDef());
        			System.out.println(bin.toString());
            }
        }
    }
    
    public static Statement findCallTo(CGNode n, String methodName) {
        IR ir = n.getIR();
        for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();) {
            SSAInstruction s = it.next();
            if (s instanceof com.ibm.wala.ssa.SSAAbstractInvokeInstruction) {
                com.ibm.wala.ssa.SSAAbstractInvokeInstruction call = (com.ibm.wala.ssa.SSAAbstractInvokeInstruction) s;
                //System.out.println(n.toString().substring(0, 20) + " "
                        + call.getCallSite().getDeclaredTarget().getName().toString());
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
    
    public static List<Integer> dumpSlice(Collection<Statement> slice) {
    	List<Integer>src_test = new ArrayList<Integer>();
        for (Statement s : slice) {
            //System.err.println(s);
            if (s.getKind() == Statement.Kind.NORMAL) { // ignore special kinds of statements
                int bcIndex, instructionIndex = ((NormalStatement) s).getInstructionIndex();
                try {
                    bcIndex = ((ShrikeBTMethod) s.getNode().getMethod()).getBytecodeIndex(instructionIndex);
                    try {
                        int src_line_number = s.getNode().getMethod().getLineNumber(bcIndex);
                        
                        //ignore Java System Library
                        if(s.getNode().getMethod().getSignature().equals("java.lang.System.<clinit>()V")) {
                        		}else{
                        			System.out.print(s.getNode().getMethod().getSignature());
                        			System.out.println("Source line number = " + src_line_number);
                        			src_test.add(src_line_number);
                        		}
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
        return src_test;
    }
	}

