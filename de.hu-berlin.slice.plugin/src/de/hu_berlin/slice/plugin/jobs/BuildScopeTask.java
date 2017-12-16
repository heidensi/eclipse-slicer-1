package de.hu_berlin.slice.plugin.jobs;

import java.io.File;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;

import de.hu_berlin.slice.plugin.AnalysisScopeFactory;
import de.hu_berlin.slice.plugin.BundleService;

/**
 * @author IShowerNaked
 */
class BuildScopeTask implements ITask {

    @Inject
    AnalysisScopeFactory analysisScopeFactory;

    @Inject
    BundleService bundleService;

    @Override
    public void run(IProgressMonitor monitor, SlicingContext context) throws TaskException {
        monitor.subTask("Creating Analysis Scope...");

        File exclusionsFile = null;
        try {
            exclusionsFile = bundleService.getFileByPath("dat/Java60RegressionExclusions.txt");
            context.analysisScope = analysisScopeFactory.create(context.getJavaProject(), exclusionsFile);
        } catch (Exception e) {
            throw new TaskException(null, e);
        }

        monitor.done();
    }
};