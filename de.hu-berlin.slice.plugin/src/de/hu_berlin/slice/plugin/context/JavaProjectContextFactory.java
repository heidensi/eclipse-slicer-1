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
 * Extracts the needed Information about the Java Project either from an ICompliationUnit, IProject
 * or IJavaProject.
 */
public class JavaProjectContextFactory {

	/**
	 * Represents the collected Information about the javaProject.
	 */
    public class JavaProjectContext {

        IJavaProject javaProject;

        public IJavaProject getJavaProject() {
            return javaProject;
        }
    }
    
    /**
     * Exception for the failure of creating the java project context.
     */
    public class JavaProjectContextFactoryException extends Exception {

        private static final long serialVersionUID = 1L;

        public JavaProjectContextFactoryException(String msg, Exception e) {
            super(msg, e);
        }
    }
    /**
     * Creates the JavaProjectContext.
     * @param compilationUnit
     * @return JavaProjectContext
     * @throws JavaProjectContextFactoryException
     */
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
    
    /**
     * Creates the JavaProjectContext.
     * @param project
     * @return	JavaProjectContext
     * @throws JavaProjectContextFactoryException
     */
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
    
    /**
     * Creates the JavaProjectContext.
     * @param javaProject
     * @return JavaProjectContext
     */
    public JavaProjectContext create(IJavaProject javaProject) {

        // ----------------------------------
        // -- Build and return the context --
        // ----------------------------------

        JavaProjectContext context = new JavaProjectContext();
        context.javaProject = javaProject;

        return context;
    }
}