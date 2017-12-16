package de.hu_berlin.slice.plugin.eclipse.classpath;

import java.util.Map;

import org.eclipse.jdt.core.IClasspathEntry;

import com.ibm.wala.classLoader.Module;

/**
 * @author IShowerNaked
 */
interface IClasspathResolver {

    public Map.Entry<ClasspathLoader, Module> resolve(ClasspathScope scope, IClasspathEntry classpathEntry) throws Exception;
}