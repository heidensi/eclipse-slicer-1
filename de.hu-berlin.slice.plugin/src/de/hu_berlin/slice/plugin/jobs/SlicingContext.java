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
    
    public enum sliceType{
            backward, forward, thinBackward, fullBackward;
    };
    
    public enum optionsData{
            FULL, NO_BASE_PTRS, NO_BASE_NO_HEAP, NO_HEAP, NONE, REFLECTION; 
    };
    
    public enum optionsCD{
            FULL    , NONE;
    };
    
    
    PointerAnalysis<InstanceKey> pointerAnalysis;
    
    Map<String, List<Integer>> map;
    public sliceType sliceType;
    
    public optionsCD optionsCD;
    
    public optionsData optionsData;
    
    public SlicingContext(EditorContext editorContext, sliceType b) {
        this.editorContext = editorContext;
        this.sliceType = b;
        this.optionsCD = optionsCD.FULL;
        this.optionsData = optionsData.FULL;
        
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
    
    public void setOptionsCD(optionsCD c) {
            this.optionsCD = c;
    }
    public void setOptionsData(optionsData c) {
        this.optionsData = c;
    }

    /**
     * @return true for forward slice and false for backward slice
     */
    public sliceType getSliceType() {
    		return sliceType;
    }


    public CallGraph getCallGraph() {
        return callGraph;
    }

    public void setPointerAnalysis(PointerAnalysis<InstanceKey> pointerAnalysis) {
        this.pointerAnalysis = pointerAnalysis;
    }

    public void setAnalysisScope(AnalysisScope analysisScope) {
        this.analysisScope = analysisScope;
    }

    public void setClassHierarchy(ClassHierarchy classHierarchy) {
        this.classHierarchy = classHierarchy;
    }

    public void setSliceType(sliceType sliceType) {
        this.sliceType = sliceType;
    }

    public void setCallGraph(CallGraph callGraph) {
        this.callGraph = callGraph;
    }
}