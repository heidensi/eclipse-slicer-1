package de.hu_berlin.slice.plugin.context;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * @author IShowerNaked
 * 
 * Used by EditorContextFactory to turn an ICompilationUnit into an IProject and finally return an IJavaProject 
 * 
 */
public class JavaProjectContextFactory {

    public class JavaProjectContext {

        IJavaProject javaProject;

        public IJavaProject getJavaProject() {
            return javaProject;
        }
    }

    public class JavaProjectContextFactoryException extends Exception {

        private static final long serialVersionUID = 1L;

        public JavaProjectContextFactoryException(String msg, Exception e) {
            super(msg, e);
        }
    }

    public JavaProjectContext create(ICompilationUnit compilationUnit) throws JavaProjectContextFactoryException {

        IResource correspondingResource = null;
        try {
            correspondingResource = compilationUnit.getCorrespondingResource();
        }
        catch (JavaModelException e) {
            throw new JavaProjectContextFactoryException("Compilation unit does not belong to any IResource object.", e);
        }

        IProject project = correspondingResource.getProject();

        return create(project);
    }

    public JavaProjectContext create(IProject project) throws JavaProjectContextFactoryException {

        try {
            if (! project.hasNature(JavaCore.NATURE_ID)) {
                throw new JavaProjectContextFactoryException("The given project is not a Java project.", null);
            }
        }
        catch (CoreException e) {
            throw new JavaProjectContextFactoryException(null, e);
        }

        IJavaProject javaProject = JavaCore.create(project);
        return create(javaProject);
    }

    public JavaProjectContext create(IJavaProject javaProject) {

        // ----------------------------------
        // -- Build and return the context --
        // ----------------------------------

        JavaProjectContext context = new JavaProjectContext();
        context.javaProject = javaProject;

        return context;
    }
}