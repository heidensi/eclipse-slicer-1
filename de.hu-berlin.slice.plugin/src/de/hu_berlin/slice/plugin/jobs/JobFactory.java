package de.hu_berlin.slice.plugin.jobs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

import com.google.inject.Injector;

/**
 * Represents a class controlling and monitoring the different steps to compute the slice.
 * @author IShowerNaked
 * 
 * Separates the slice into several tasks.
 */
@Singleton
public class JobFactory {

    @Inject
    Injector injector;
    
    /**
     * Creates and runs the different steps to compute the slice.
     * @param slicing context
     * @return
     */
    public Job create(SlicingContext context) {

        List<ITask> tasks = new ArrayList<>(Arrays.asList(
            injector.getInstance(CompileTask.class),
            injector.getInstance(BuildScopeTask.class),
            injector.getInstance(ClassHierarchyTask.class),
            injector.getInstance(EntrypointLocatorTask.class),
            injector.getInstance(SlicingTask.class)
        ));

        return Job.create("Trying hard", monitor -> {

            SubMonitor subMonitor = SubMonitor.convert(monitor, tasks.size());

            try {
                for (ITask task : tasks) {
                    task.run(subMonitor.split(1), context);
                }
            }
            catch (OperationCanceledException | TaskException e) {
                e.printStackTrace();
                monitor.setCanceled(true);
            }
            monitor.done();
        });
    }
}