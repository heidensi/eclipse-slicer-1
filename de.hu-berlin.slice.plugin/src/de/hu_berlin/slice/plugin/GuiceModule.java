package de.hu_berlin.slice.plugin;

import com.google.inject.AbstractModule;

import de.hu_berlin.slice.ast.ASTService;
import de.hu_berlin.slice.plugin.context.EditorContextFactory;
import de.hu_berlin.slice.plugin.context.JavaProjectContextFactory;
import de.hu_berlin.slice.plugin.eclipse.classpath.LibraryClasspathResolver;
import de.hu_berlin.slice.plugin.eclipse.classpath.SourceClasspathResolver;
import de.hu_berlin.slice.plugin.jobs.JobFactory;

/**
 * @author IShowerNaked
 */
public class GuiceModule extends AbstractModule {

    @Override
    protected void configure() {

        bind(ASTService.class);
        bind(BundleService.class);
        bind(EclipseService.class);
        bind(EditorContextFactory.class);
        bind(JavaProjectContextFactory.class);
        bind(ProjectService.class);
        bind(WorkspaceService.class);

        bind(SourceClasspathResolver.class);
        bind(LibraryClasspathResolver.class);
        bind(AnalysisScopeFactory.class);

        bind(JobFactory.class);
    }
}