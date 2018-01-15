package de.hu_berlin.slice.plugin.eclipse.classpath;

import java.util.Map;

import org.eclipse.jdt.core.IClasspathEntry;

import com.ibm.wala.classLoader.Module;

/**
 * Interface for resolving a classpath entry.
 * @author IShowerNaked
 */
interface IClasspathResolver {
	
	/**
	 * Resolves the classpath entry according to the classpath scope.
	 * @param scope
	 * determines whether you want the source or binary directory. 
	 * @param classpathEntry
	 * from the classpath file
	 * @return a map entry containing the ClasspathLoaderRefrence and the module tree directory.
	 * @throws Exception
	 */
    public Map.Entry<ClasspathLoader, Module> resolve(ClasspathScope scope, IClasspathEntry classpathEntry) throws Exception;
}