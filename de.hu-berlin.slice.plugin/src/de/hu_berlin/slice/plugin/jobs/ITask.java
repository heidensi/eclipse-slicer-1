package de.hu_berlin.slice.plugin.jobs;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author IShowerNaked
 */
interface ITask {
    public void run(IProgressMonitor monitor, SlicingContext context) throws TaskException;
}