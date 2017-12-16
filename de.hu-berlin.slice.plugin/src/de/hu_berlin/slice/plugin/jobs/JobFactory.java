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
 * @author IShowerNaked
 */
@Singleton
public class JobFactory {

    @Inject
    Injector injector;

    public Job create(SlicingContext context) {

        List<ITask> tasks = new ArrayList<>(Arrays.asList(
            injector.getInstance(CompileTask.class),
            injector.getInstance(BuildScopeTask.class),
            injector.getInstance(ClassHierarchyTask.class),
            injector.getInstance(EntrypointLocatorTask.class)
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