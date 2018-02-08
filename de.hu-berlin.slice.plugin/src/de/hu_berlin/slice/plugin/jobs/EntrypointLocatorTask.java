package de.hu_berlin.slice.plugin.jobs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.SubtypesEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.ClassLoaderReference;

/**
 * Task where the call graph gets build. 
 * @author IShowerNaked
 */
public class EntrypointLocatorTask implements ITask {

    @Override
    public void run(IProgressMonitor monitor, SlicingContext context) throws TaskException {
        monitor.subTask("Locating entrypoint...");

        try {
            ClassHierarchy classHierarchy = context.classHierarchy;
            List<Entrypoint> entrypoints = getEntrypoints(classHierarchy);
            
            AnalysisOptions options = new AnalysisOptions(context.analysisScope, entrypoints);

            // TODO: not sure what exactly this is for, but it seems to place a LambdaMethodTargetSelector to `options`. we must understand this sooner or later...
            Util.addDefaultSelectors(options, classHierarchy);

            CallGraphBuilder<InstanceKey> callGraphBuilder = ZeroXCFABuilder.make(classHierarchy, options, new AnalysisCacheImpl(), null, null, ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.CONSTANT_SPECIFIC);
            CallGraph callGraph = callGraphBuilder.makeCallGraph(options, null);
            context.callGraph = callGraph;
            
            PointerAnalysis<InstanceKey> pointerAnalysis = callGraphBuilder.getPointerAnalysis();
            context.pointerAnalysis = pointerAnalysis;
            
            debugCallgraph(callGraph);

        }
        catch (Exception e) {
            throw new TaskException(null, e);
        }

        monitor.done();
    }
    
    
    /**
     * Iterates through all Application modules to collect the Entrypoints
     * @param classHierarchy
     * @return the Entrypoints of the Class Hierarchy
     */
    public List<Entrypoint> getEntrypoints(ClassHierarchy classHierarchy){
    		IClassLoader classLoader = classHierarchy.getLoader(ClassLoaderReference.Application);
    		List<Entrypoint> entrypoints = new ArrayList<>();

        for (Iterator<IClass> classIterator = classLoader.iterateAllClasses(); classIterator.hasNext(); ) {

            IClass klass = classIterator.next();

            for (IMethod method : klass.getDeclaredMethods()) {
                Entrypoint entrypoint = new SubtypesEntrypoint(method, classHierarchy);
                //System.out.println("Entrypoints " + entrypoint.getMethod().getName());
                entrypoints.add(entrypoint);
            }
        }
        return entrypoints;
    }
    
    /**
     * Prints out all the SSA Instructions from the call graph 
     * @param Callgraph 
     * @throws InvalidClassFileException
     */
    public void debugCallgraph(CallGraph callgraph) throws InvalidClassFileException {

    		for (CGNode cgEntryoint : callgraph.getEntrypointNodes()) {
            IR ir = cgEntryoint.getIR();
            IBytecodeMethod cgBytecodeEntryoint = (IBytecodeMethod)ir.getMethod();

            int ssaInstructionIndex = -1;
            for (Iterator<SSAInstruction> iter = ir.iterateAllInstructions(); iter.hasNext();) {

                SSAInstruction ssaInstruction = iter.next();
                int bcIndex = cgBytecodeEntryoint.getBytecodeIndex(++ssaInstructionIndex);

                if (-1 == bcIndex) {
                    System.err.println("Na, damn it!");
                }
                else {
                    System.err.println(String.format("Instruction Index: %d, Line number: %d, Instruction: %s", bcIndex, cgBytecodeEntryoint.getLineNumber(bcIndex), ssaInstruction.toString()));
                }

                // this demo shows how Wala aggregates multiple source lines into one SSAInstruction
                if (ssaInstruction instanceof SSABinaryOpInstruction) {
                    SSABinaryOpInstruction test = (SSABinaryOpInstruction)ssaInstruction;
                    System.err.println("- Operator: " + test.getOperator() );
                }
                
            }
            
        }
    }
    
    /**
     * Prints out all the Classloaders from the Classhierarchy
     * @param classHierarchy
     */
    public void debugClassloaders(ClassHierarchy classHierarchy) {
    		for (IClassLoader cl : classHierarchy.getLoaders()) {
            System.err.println(cl.getClass().getName());
        }
    }
    

    
}