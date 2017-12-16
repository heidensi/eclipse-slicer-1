package de.hu_berlin.slice.plugin.jobs;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author IShowerNaked
 */
class CompileTask implements ITask {

    @Override
    public void run(IProgressMonitor monitor, SlicingContext context) throws TaskException {
        monitor.subTask("Compiling...");

        try {
            context.getJavaProject().getProject().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
        } catch (CoreException e) {
            throw new TaskException(null, e);
        }
        monitor.done();
    }
}