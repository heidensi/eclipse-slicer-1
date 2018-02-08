package de.hu_berlin.slice.plugin.jobs;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IJavaProject;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;

import de.hu_berlin.slice.plugin.context.EditorContextFactory.EditorContext;

/**
 * Represents all the necessary Information to run the Slice.
 * @author IShowerNaked
 */
public class SlicingContext {
	

    EditorContext editorContext;

    AnalysisScope analysisScope;

    ClassHierarchy classHierarchy;
    
    List<Integer> list;
    
    CallGraph callGraph;
    
    boolean sliceType;
    
    PointerAnalysis<InstanceKey> pointerAnalysis;
    
    Map<String, List<Integer>> map;
    

    public SlicingContext(EditorContext editorContext, boolean b) {
        this.editorContext = editorContext;
        this.sliceType = b;
        
    }

    public IJavaProject getJavaProject() {
        return editorContext.getJavaProjectContext().getJavaProject();
    }

    public AnalysisScope getAnalysisScope() {
        return analysisScope;
    }

    public ClassHierarchy getClassHierarchy() {
        return classHierarchy;
    }
    
    public List<Integer> getList() {
    		return list;
    }
    
    public Map<String,List<Integer>> getMap(){
    		return map;
    }
    
    
    /**
     * @return true for forward slice and false for backward slice
     */
    public boolean getSliceType() {
    		return sliceType;
    }
    

}