package de.hu_berlin.slice.plugin.jobs;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;

/**
 * @author IShowerNaked
 */
class ClassHierarchyTask implements ITask {

    @Override
    public void run(IProgressMonitor monitor, SlicingContext context) throws TaskException {
        monitor.subTask("Computing class hierarchy...");

        try {
            context.classHierarchy = ClassHierarchyFactory.make(context.analysisScope);
        } catch (ClassHierarchyException e) {
            throw new TaskException(null, e);
        }
        monitor.done();
    }
}