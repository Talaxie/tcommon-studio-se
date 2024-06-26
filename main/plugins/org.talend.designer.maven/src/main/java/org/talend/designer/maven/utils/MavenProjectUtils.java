// ============================================================================
//
// Copyright (C) 2006-2021 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.maven.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.ClasspathAttribute;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.osgi.service.prefs.BackingStoreException;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.utils.generation.JavaUtils;
import org.talend.designer.maven.model.MavenSystemFolders;
import org.talend.designer.maven.model.ProjectSystemFolder;
import org.talend.designer.maven.model.TalendMavenConstants;

/**
 * DOC zwxue class global comment. Detailled comment
 */
@SuppressWarnings("restriction")
public class MavenProjectUtils {

	public static void enableMavenNature(IProgressMonitor monitor, IProject project) {
		try {
			IProjectDescription projectDescription = project.getDescription();
			if (!project.hasNature(IMavenConstants.NATURE_ID)) {
				projectDescription
						.setNatureIds(ArrayUtils.add(projectDescription.getNatureIds(), IMavenConstants.NATURE_ID));
				project.setDescription(projectDescription, monitor);
				changeClasspath(monitor, project);
				IJavaProject javaProject = JavaCore.create(project);
				clearProjectIndenpendComplianceSettings(javaProject);
			}
		} catch (CoreException e) {
			ExceptionHandler.process(e);
		}
	}

    public static void disableMavenNature(IProgressMonitor monitor, IProject project) {
        try {
        	IProjectDescription projectDescription = project.getDescription();
			if (project.hasNature(IMavenConstants.NATURE_ID)) {
				projectDescription
				.setNatureIds(ArrayUtils.removeElement(projectDescription.getNatureIds(), IMavenConstants.NATURE_ID));
				project.setDescription(projectDescription, monitor);				
			}
        } catch (CoreException e) {
            ExceptionHandler.process(e);
        }
    }

    public static boolean hasMavenNature(IProject project) {
        try {
            return project.hasNature(IMavenConstants.NATURE_ID);
        } catch (CoreException e) {
            ExceptionHandler.process(e);
        }
        return false;
    }

    public static void updateMavenProject(IProgressMonitor monitor, IProject project) throws CoreException {
        // async way
        // MavenUpdateRequest mavenUpdateRequest = new MavenUpdateRequest(project, true, false);
        // MavenPlugin.getMavenProjectRegistry().refresh(mavenUpdateRequest);
        if (project.getName().equals(TalendMavenConstants.PROJECT_NAME)) {
            // do not update .Java project.
            return;
        }
        MavenPlugin.getProjectConfigurationManager().updateProjectConfiguration(project, monitor);

        changeClasspath(monitor, project, MavenSystemFolders.ALL_DIRS_EXT);

        // only need this when pom has no parent.
        // IJavaProject javaProject = JavaCore.create(project);
        // clearProjectIndenpendComplianceSettings(javaProject);
    }

    public static void changeClasspath(IProgressMonitor monitor, IProject p) {
        changeClasspath(monitor, p, MavenSystemFolders.ALL_DIRS);
    }

    public static void changeClasspath(IProgressMonitor monitor, IProject p, ProjectSystemFolder[] folders) {
        try {
            if (!p.hasNature(JavaCore.NATURE_ID)) {
                JavaUtils.addJavaNature(p, monitor);
            }
            IJavaProject javaProject = JavaCore.create(p);
            IClasspathEntry[] rawClasspathEntries = javaProject.getRawClasspath();

            List<IClasspathEntry> list = new LinkedList<>();
            ClasspathAttribute attribute = new ClasspathAttribute("maven.pomderived", Boolean.TRUE.toString());
            for (ProjectSystemFolder psf : folders) {
                IFolder resources = p.getFolder(psf.getPath());
                if (resources.exists()) { // add the condition mostly for routines, since the resources folder might not exist
                    IFolder output = p.getFolder(psf.getOutputPath());
                    IClasspathEntry newEntry = JavaCore.newSourceEntry(resources.getFullPath(), new IPath[0], new IPath[0],
                            output.getFullPath(), new IClasspathAttribute[] { attribute });
                    list.add(newEntry);
                }
            }
            IPath defaultJREContainerPath = JavaRuntime.newDefaultJREContainerPath();

            IClasspathEntry newEntry = JavaCore.newContainerEntry(defaultJREContainerPath, new IAccessRule[] {},
                    new IClasspathAttribute[] { attribute }, false);
            list.add(newEntry);

            newEntry = JavaCore.newContainerEntry(new Path("org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER"),
                    newEntry.getAccessRules(), new IClasspathAttribute[] { attribute }, newEntry.isExported());
            list.add(newEntry);

            if (!Arrays.equals(rawClasspathEntries, list.toArray(new IClasspathEntry[] {})) || !p.getFile(".classpath").exists()) {
                rawClasspathEntries = list.toArray(new IClasspathEntry[] {});
                javaProject.setRawClasspath(rawClasspathEntries, monitor);
                javaProject.setOutputLocation(p.getFolder(MavenSystemFolders.JAVA.getOutputPath()).getFullPath(), monitor);
            }
        } catch (

        CoreException e) {
            ExceptionHandler.process(e);
        }
    }

    public static void addProjectClasspathEntry(IProgressMonitor monitor, IProject project, List<IClasspathEntry> entries) {
        try {
            Set<IClasspathEntry> classpathentries = new LinkedHashSet<IClasspathEntry>();
            IJavaProject javaProject = JavaCore.create(project);
            IClasspathEntry[] rawClasspathEntries = javaProject.getRawClasspath();
            for (IClasspathEntry entry : rawClasspathEntries) {
                classpathentries.add(entry);
            }
            classpathentries.addAll(entries);
            rawClasspathEntries = classpathentries.toArray(new IClasspathEntry[] {});
            javaProject.setRawClasspath(rawClasspathEntries, monitor);
            javaProject.setOutputLocation(project.getFolder(MavenSystemFolders.JAVA.getOutputPath()).getFullPath(), monitor);
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
    }


    /**
     * Clear compliance settings from project, and set them into Eclipse compliance settings
     *
     * @param javaProject
     */
    private static void clearProjectIndenpendComplianceSettings(IJavaProject javaProject) {

        Map<String, String> projectComplianceOptions = javaProject.getOptions(false);
        if (projectComplianceOptions == null || projectComplianceOptions.isEmpty()) {
            return;
        }
        String compilerCompliance = JavaUtils.getProjectJavaVersion();
        // clear compliance settings from project
        Set<String> keySet = projectComplianceOptions.keySet();
        for (String key : keySet) {
            javaProject.setOption(key, null);
        }
        if (compilerCompliance != null) {
            IEclipsePreferences eclipsePreferences = InstanceScope.INSTANCE.getNode(JavaCore.PLUGIN_ID);
            // set compliance settings to Eclipse
            Map<String, String> complianceOptions = new HashMap<String, String>();
            JavaCore.setComplianceOptions(compilerCompliance, complianceOptions);
            if (!complianceOptions.isEmpty()) {
                Set<Entry<String, String>> entrySet = complianceOptions.entrySet();
                for (Entry<String, String> entry : entrySet) {
                    eclipsePreferences.put(entry.getKey(), entry.getValue());
                }
            }
            try {
                // save changes
                eclipsePreferences.flush();
            } catch (BackingStoreException e) {
                ExceptionHandler.process(e);
            }
        }
    }

}
