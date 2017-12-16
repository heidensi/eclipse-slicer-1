package de.hu_berlin.slice.plugin.eclipse.classpath;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.jar.JarFile;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.jdt.core.IClasspathEntry;

import com.google.common.collect.Maps;
import com.ibm.wala.classLoader.BinaryDirectoryTreeModule;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;

import de.hu_berlin.slice.plugin.WorkspaceService;

/**
 * @author IShowerNaked
 */
@Singleton
public class LibraryClasspathResolver implements IClasspathResolver {

    @Inject
    WorkspaceService workspaceService;

    @Override
    public Entry<ClasspathLoader, Module> resolve(ClasspathScope scope, IClasspathEntry classpathEntry) throws IOException {

        File correspondingFileOrDirectory = workspaceService.getAbsolutePath(classpathEntry.getPath()).toFile();

        Module module = null;
        if (correspondingFileOrDirectory.isDirectory()) {
            module = new BinaryDirectoryTreeModule(correspondingFileOrDirectory);
        }
        else {
            module = new JarFileModule(new JarFile(correspondingFileOrDirectory));
        }

        return Maps.immutableEntry(ClasspathLoader.PRIMORDIAL, module);
    }
}