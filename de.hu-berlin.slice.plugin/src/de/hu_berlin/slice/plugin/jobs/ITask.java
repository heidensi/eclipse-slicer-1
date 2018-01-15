package de.hu_berlin.slice.plugin.jobs;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * An interface for running the different tasks.
 * @author IShowerNaked
 */
interface ITask {
	/**
	 * Runs the given task.
	 * @param monitor
	 * @param context which represents the slicing context
	 * @throws TaskException
	 */
    public void run(IProgressMonitor monitor, SlicingContext context) throws TaskException;
}