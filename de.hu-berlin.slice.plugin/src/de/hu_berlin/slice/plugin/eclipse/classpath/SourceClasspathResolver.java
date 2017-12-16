package de.hu_berlin.slice.plugin.eclipse.classpath;

import java.io.File;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaModelException;

import com.google.common.collect.Maps;
import com.ibm.wala.classLoader.BinaryDirectoryTreeModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceDirectoryTreeModule;

import de.hu_berlin.slice.plugin.WorkspaceService;

/**
 * @author IShowerNaked
 */
@Singleton
public class SourceClasspathResolver implements IClasspathResolver {

    @Inject
    WorkspaceService workspaceService;

    @Override
    public Map.Entry<ClasspathLoader, Module> resolve(ClasspathScope scope, IClasspathEntry classpathEntry) throws Exception {

        if (scope.isIncludeSource()) {
            return resolveForSource(scope, classpathEntry);
        }
        return resolveForBinary(scope, classpathEntry);
    }

    private Map.Entry<ClasspathLoader, Module> resolveForSource(ClasspathScope scope, IClasspathEntry classpathEntry) {

        File srcDirectory = workspaceService.getAbsolutePath(classpathEntry.getPath()).toFile();
        Module module = new SourceDirectoryTreeModule(srcDirectory);

        return Maps.immutableEntry(ClasspathLoader.SOURCE, module);
    }

    private Map.Entry<ClasspathLoader, Module> resolveForBinary(ClasspathScope scope, IClasspathEntry classpathEntry) throws JavaModelException {

        IPath outputLocation = classpathEntry.getOutputLocation();
        if (null == outputLocation) {
            outputLocation = scope.getJavaProject().getOutputLocation();
        }

        Module module = new BinaryDirectoryTreeModule(workspaceService.getAbsolutePath(outputLocation).toFile());

        return Maps.immutableEntry(ClasspathLoader.APPLICATION, module);
    }
}