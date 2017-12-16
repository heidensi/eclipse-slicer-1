package de.hu_berlin.slice.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import com.google.inject.Injector;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceDirectoryTreeModule;
import com.ibm.wala.classLoader.SourceFileModule;

/**
 * @author IShowerNaked
 */
@Singleton
public class ProjectService {

    @Inject
    WorkspaceService workspaceService;

    @Inject
    Injector injector;

    public class ModuleDefinition {

        private List<Module> sourceModuleList = new ArrayList<>();

        private List<Module> systemModuleList = new ArrayList<>();
    }

    public List<String> resolveClassPathList(IJavaProject javaProject) throws JavaModelException {

        List<String> classPathList = new ArrayList<>();

        IClasspathEntry[] classPathEntries = javaProject.getResolvedClasspath(true);
        for (IClasspathEntry classpathEntry : classPathEntries) {
            IPath path = classpathEntry.getPath();

            String absolutePathAsString = workspaceService.getAbsolutePathAsString(path);
            classPathList.add(absolutePathAsString);
        }

        return classPathList;
    }

    public ModuleDefinition getLoaderDefinition(IJavaProject javaProject) throws JavaModelException, IOException {

        ModuleDefinition moduleDefinition = new ModuleDefinition();

        IPackageFragmentRoot[] packageFragmentRootList = javaProject.getAllPackageFragmentRoots();

        for (int i = 0; i < packageFragmentRootList.length; ++i) {

            IPackageFragmentRoot packageFragmentRoot = packageFragmentRootList[i];

            String absolutePathAsString = workspaceService.getAbsolutePathAsString(packageFragmentRoot.getPath());
            File absolutePath = new File(absolutePathAsString);

            Module module = null;

            if (IPackageFragmentRoot.K_SOURCE == packageFragmentRoot.getKind()) {
                if (absolutePath.isDirectory()) {
                    module = new SourceDirectoryTreeModule(absolutePath);
                }
                else {
                    module = new SourceFileModule(absolutePath, absolutePath.getName(), null);
                }
                moduleDefinition.sourceModuleList.add(module);
            }
            else if (IPackageFragmentRoot.K_BINARY == packageFragmentRoot.getKind()) {
                if (absolutePath.isFile()) {
                    module = new JarFileModule(new JarFile(absolutePath));
                    moduleDefinition.systemModuleList.add(module);
                }
            }
        }

        return moduleDefinition;
    }
}