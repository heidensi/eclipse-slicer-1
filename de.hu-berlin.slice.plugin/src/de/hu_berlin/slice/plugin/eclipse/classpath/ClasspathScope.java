package de.hu_berlin.slice.plugin.eclipse.classpath;

import org.eclipse.jdt.core.IJavaProject;

/**
 * @author IShowerNaked
 */
public class ClasspathScope {

    boolean includeSource = true;

    private IJavaProject javaProject;

    public ClasspathScope(IJavaProject javaProject) {
        this.javaProject = javaProject;
    }

    public IJavaProject getJavaProject() {
        return javaProject;
    }

    public boolean isIncludeSource() {
        return includeSource;
    }

    public void setIncludeSource(boolean includeSource) {
        this.includeSource = includeSource;
    }
}