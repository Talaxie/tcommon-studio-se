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
package org.talend.designer.maven.template;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.utils.generation.JavaUtils;
import org.talend.core.model.routines.CodesJarInfo;
import org.talend.core.runtime.projectsetting.IProjectSettingPreferenceConstants;
import org.talend.core.runtime.projectsetting.IProjectSettingTemplateConstants;
import org.talend.designer.maven.DesignerMavenPlugin;
import org.talend.designer.maven.model.MergedModel;
import org.talend.designer.maven.model.TalendMavenConstants;
import org.talend.designer.maven.setting.project.IProjectSettingManagerProvider;
import org.talend.designer.maven.tools.MergeModelTool;
import org.talend.designer.maven.tools.extension.PomExtensionRegistry;
import org.talend.designer.maven.utils.PomIdsHelper;
import org.talend.designer.maven.utils.PomUtil;
import org.talend.repository.ProjectManager;

/**
 * created by ggu on 5 Feb 2015 Detailled comment
 *
 */
public class MavenTemplateManager {

    public static final String KEY_PROJECT_NAME = "ProjectName"; //$NON-NLS-1$

    public static Map<String, AbstractMavenTemplateManager> getTemplateManagerMap() {
        return MavenTemplateManagerRegistry.getInstance().getTemplateManagerMap();
    }

    public static Map<String, IProjectSettingManagerProvider> getProjectSettingManagerMap() {
        return MavenTemplateManagerRegistry.getInstance().getProjectSettingManagerMap();
    }

    /**
     * 1. get the template from the file template under the folder first.
     *
     * 2. if file template is not existed, try to get th template from project setting.
     *
     * 3. if the project setting is not set still, try to get the template from the bundle template.
     *
     */
    @SuppressWarnings("resource")
    public static InputStream getTemplateStream(File templateFile, String projectSettingKey, String bundleName,
            String bundleTemplatePath, Map<String, Object> parameters) throws Exception {
        InputStream stream = null;
        // 1. from file template dirctly.
        if (templateFile != null && templateFile.exists()) {
            // will close it later.
            stream = new BufferedInputStream(new FileInputStream(templateFile));
        }

        // 2. from project setting
        if (stream == null && projectSettingKey != null) {
            stream = getProjectSettingStream(projectSettingKey, parameters);
        }
        // 3. from bundle template.
        if (stream == null && bundleName != null && bundleTemplatePath != null) {
            stream = getBundleTemplateStream(bundleName, bundleTemplatePath);
        }
        return stream;
    }

    public static String getTemplateContent(File templateFile, String projectSettingKey, String bundleName,
            String bundleTemplatePath, Map<String, Object> parameters) throws Exception {
        return getContentFromInputStream(getTemplateStream(templateFile, projectSettingKey, bundleName, bundleTemplatePath,
                parameters));
    }

    /**
     *
     * get the template file stream from bundle.
     */
    public static InputStream getBundleTemplateStream(String bundleName, String bundleTemplatePath) throws Exception {
        if (bundleName == null || bundleTemplatePath == null) {
            return null;
        }
        Map<String, AbstractMavenTemplateManager> templateManagerMap = MavenTemplateManager.getTemplateManagerMap();
        AbstractMavenTemplateManager templateManager = templateManagerMap.get(bundleName);
        if (templateManager != null) {
            return templateManager.readBundleStream(bundleTemplatePath);
        }
        return null;
    }

    public static String getBundleTemplateContent(String bundleName, String templatePath) throws Exception {
        return getContentFromInputStream(getBundleTemplateStream(bundleName, templatePath));
    }

    /**
     * try to find the template setting in project setting one by one.
     */
    public static String getProjectSettingValue(String key, Map<String, Object> parameters) {
        Map<String, AbstractMavenTemplateManager> templateManagerMap = MavenTemplateManager.getTemplateManagerMap();

        for (String bundleName : templateManagerMap.keySet()) {
            AbstractMavenTemplateManager templateManager = templateManagerMap.get(bundleName);
            try {
                InputStream steam = templateManager.readProjectSettingStream(key, parameters);
                if (steam != null) {
                    String content = MavenTemplateManager.getContentFromInputStream(steam);
                    if (content != null) {
                        return content;
                    }
                }
            } catch (Exception e) {
                // try to find another one
            }
        }
        return null;
    }

    /**
     * try to find the template setting in project setting one by one.
     */
    public static InputStream getProjectSettingStream(String key, Map<String, Object> parameters) {
        Map<String, AbstractMavenTemplateManager> templateManagerMap = MavenTemplateManager.getTemplateManagerMap();

        for (String bundleName : templateManagerMap.keySet()) {
            AbstractMavenTemplateManager templateManager = templateManagerMap.get(bundleName);
            try {
                InputStream steam = templateManager.readProjectSettingStream(key, parameters);
                if (steam != null) {
                    return steam;
                }
            } catch (Exception e) {
                // try to find another one
            }
        }
        return null;
    }

    public static String getContentFromInputStream(InputStream is) throws IOException {
        if (is != null) {
            try {
                StringWriter sw = new StringWriter(1000);
                int c = 0;
                while ((c = is.read()) != -1) {
                    sw.write(c);
                }
                return sw.toString();
            } finally {
                is.close();
            }
        }
        return null;
    }

    public static void saveContent(IFile targetFile, String content, boolean overwrite) throws CoreException {
        if (content != null) {
            ByteArrayInputStream source = new ByteArrayInputStream(content.getBytes());
            if (targetFile.exists()) {
                targetFile.setContents(source, true, false, new NullProgressMonitor());
            } else {
                targetFile.create(source, true, new NullProgressMonitor());
            }
        }
    }

    public static void saveContent(File targetFile, String content, boolean overwrite) throws IOException {
        if (targetFile.exists()) {
            if (!overwrite) {
                // throw new IOException("The file have existed, must delete or with overwrite option.");
                return; // nothing to do, keep it.
            }
            if (!targetFile.isFile()) {
                throw new IOException("Can't write the template to directory, must be file.");
            }
        }

        if (content != null) {
            FileWriter writer = null;
            try {
                writer = new FileWriter(targetFile);
                writer.write(content);
                writer.flush();
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        }
    }

    /**
     * Try to load the project template from bundle, if load failed, use default instead.
     */
    public static Model getCodeProjectTemplateModel(Map<String, Object> parameters) {
        Model basicModel = getBasicProjectPomTemplateModel(parameters);
        Model defaultModel = getDefaultProjectModel(parameters);
        if (defaultModel == null) {
            defaultModel = basicModel;
        }
        Model customModel = getCustomProjectModel(parameters);
        MergedModel mergedModel = new MergeModelTool().mergeModel(defaultModel, customModel);
        Model model = mergedModel.getModel();
        if (model != null) {
            Map<ETalendMavenVariables, String> variablesValuesMap = new HashMap<>();
            variablesValuesMap.put(ETalendMavenVariables.ProjectGroupId, basicModel.getGroupId());
            variablesValuesMap.put(ETalendMavenVariables.ProjectArtifactId, basicModel.getArtifactId());
            variablesValuesMap.put(ETalendMavenVariables.ProjectVersion, basicModel.getVersion());
            variablesValuesMap.put(ETalendMavenVariables.ProjectName, PomUtil.getProjectNameFromTemplateParameter(parameters));

            model.setGroupId(ETalendMavenVariables.replaceVariables(model.getGroupId(), variablesValuesMap));
            model.setArtifactId(ETalendMavenVariables.replaceVariables(model.getArtifactId(), variablesValuesMap));
            model.setVersion(ETalendMavenVariables.replaceVariables(model.getVersion(), variablesValuesMap));
            model.setName(ETalendMavenVariables.replaceVariables(model.getName(), variablesValuesMap));

            setJavaVersionForModel(model, variablesValuesMap);
        }

        return model;
    }

    public static Model getDefaultProjectModel(Map<String, Object> parameters) {
        try {
            InputStream stream = MavenTemplateManager.getTemplateStream(null, null, DesignerMavenPlugin.PLUGIN_ID,
                    IProjectSettingTemplateConstants.PATH_GENERAL + '/'
                            + IProjectSettingTemplateConstants.PROJECT_TEMPLATE_FILE_NAME,
                    parameters);
            if (stream != null) {
                Model model = MavenPlugin.getMavenModelManager().readMavenModel(stream);
                PomExtensionRegistry.getInstance().updateProjectPom(model);
                PomExtensionRegistry.getInstance().updatePomTemplate(model);
                Properties properties = model.getProperties();
                properties.put("talend.project.name", PomUtil.getProjectNameFromTemplateParameter(parameters)); //$NON-NLS-1$
                return model;
            }
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
        return null;
    }

    public static Model getCustomProjectModel(Map<String, Object> parameters) {
        try {
            InputStream stream = MavenTemplateManager.getTemplateStream(null,
                    IProjectSettingPreferenceConstants.TEMPLATE_PROJECT_POM, null, null, parameters);
            if (stream != null) {
                return MavenPlugin.getMavenModelManager().readMavenModel(stream);
            }
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
        return null;
    }

    private static void setJavaVersionForModel(Model model, Map<ETalendMavenVariables, String> variablesValuesMap) {
        String javaVersion = JavaUtils.getProjectJavaVersion();
        if (javaVersion == null || javaVersion.trim().equals("")) { //$NON-NLS-1$
            javaVersion = JavaUtils.DEFAULT_VERSION;
        }
        variablesValuesMap.put(ETalendMavenVariables.JavaVersion, javaVersion);
        Plugin plugin = model.getBuild().getPluginManagement().getPluginsAsMap().get("org.apache.maven.plugins:maven-compiler-plugin"); //$NON-NLS-1$
        Object object = plugin.getConfiguration();
        if (object instanceof Xpp3Dom) {
            Xpp3Dom configNode = (Xpp3Dom) object;
            Xpp3Dom sourceNode = configNode.getChild("source"); //$NON-NLS-1$
            Xpp3Dom targetNode = configNode.getChild("target"); //$NON-NLS-1$
            if (!javaVersion.equals(sourceNode.getValue())) {
                sourceNode.setValue(ETalendMavenVariables.replaceVariables(sourceNode.getValue(), variablesValuesMap));
                targetNode.setValue(ETalendMavenVariables.replaceVariables(targetNode.getValue(), variablesValuesMap));
            }
        }
    }

    private static Model getBasicProjectPomTemplateModel(Map<String, Object> parameters) {
        String projectTechName = PomUtil.getProjectNameFromTemplateParameter(parameters);
        Model templateCodeProjectMOdel = new Model();
        templateCodeProjectMOdel.setGroupId(PomIdsHelper.getProjectGroupId(projectTechName));
        templateCodeProjectMOdel.setArtifactId(PomIdsHelper.getProjectArtifactId());
        templateCodeProjectMOdel.setVersion(PomIdsHelper.getProjectVersion(projectTechName));
        templateCodeProjectMOdel.setPackaging(TalendMavenConstants.PACKAGING_POM);

        return templateCodeProjectMOdel;
    }

    public static Model getRoutinesTempalteModel(String projectTechName) {
        Model defaultModel = createDefaultCodesTempalteModel(
                PomIdsHelper.getCodesGroupId(projectTechName, TalendMavenConstants.DEFAULT_CODE),
                TalendMavenConstants.DEFAULT_ROUTINES_ARTIFACT_ID, PomIdsHelper.getCodesVersion(projectTechName));
        return getCodesModelFromGeneralTemplate(defaultModel, projectTechName, "Routines", //$NON-NLS-1$
                JavaUtils.JAVA_ROUTINES_DIRECTORY);
    }
    
    public static Model getRoutinesJarTempalteModel(CodesJarInfo info) {
        String projectTechName = info.getProjectTechName();
        String label = info.getLabel();
        Model defaultModel = createDefaultCodesTempalteModel(PomIdsHelper.getCodesJarGroupId(info), label.toLowerCase(),
                PomIdsHelper.getCodesJarVersion(projectTechName));
        return getCodesModelFromGeneralTemplate(defaultModel, projectTechName, label.toLowerCase(),
                JavaUtils.JAVA_ROUTINESJAR_DIRECTORY);
    }

    public static Model getBeansTempalteModel(String projectTechName) {
        Model defaultModel = createDefaultCodesTempalteModel(
                PomIdsHelper.getCodesGroupId(projectTechName, TalendMavenConstants.DEFAULT_BEAN),
                TalendMavenConstants.DEFAULT_BEANS_ARTIFACT_ID, PomIdsHelper.getCodesVersion(projectTechName));
        return getCodesModelFromGeneralTemplate(defaultModel, projectTechName, "Beans", JavaUtils.JAVA_BEANS_DIRECTORY); //$NON-NLS-1$
    }
    
    public static Model getBeansJarTempalteModel(CodesJarInfo info) {
        String projectTechName = info.getProjectTechName();
        String label = info.getLabel();
        Model defaultModel = createDefaultCodesTempalteModel(PomIdsHelper.getCodesJarGroupId(info), label.toLowerCase(),
                PomIdsHelper.getCodesJarVersion(projectTechName));
        return getCodesModelFromGeneralTemplate(defaultModel, projectTechName, label.toLowerCase(),
                JavaUtils.JAVA_BEANSJAR_DIRECTORY);
    }

    private static Model createDefaultCodesTempalteModel(String groupId, String artifactId, String version) {
        Model templateRoutinesModel = new Model();

        templateRoutinesModel.setGroupId(groupId);
        templateRoutinesModel.setArtifactId(artifactId);
        templateRoutinesModel.setVersion(version);

        return templateRoutinesModel;
    }

    /**
     * Try to load the project template from bundle, if load failed, use default instead.
     */
    private static Model getCodesModelFromGeneralTemplate(Model defaultModel, String projectTechName, String codesName,
            String codesPackage) {
        try {
            if (projectTechName == null) {
                projectTechName = ProjectManager.getInstance().getCurrentProject().getTechnicalLabel();
            }
            InputStream stream = MavenTemplateManager.getBundleTemplateStream(DesignerMavenPlugin.PLUGIN_ID,
                    IProjectSettingTemplateConstants.PATH_GENERAL + '/'
                            + IProjectSettingTemplateConstants.POM_CODES_TEMPLATE_FILE_NAME);
            if (stream != null) {
                Model model = MavenPlugin.getMavenModelManager().readMavenModel(stream);

                Map<ETalendMavenVariables, String> variablesValuesMap = new HashMap<ETalendMavenVariables, String>();
                variablesValuesMap.put(ETalendMavenVariables.CodesGroupId, defaultModel.getGroupId());
                variablesValuesMap.put(ETalendMavenVariables.CodesArtifactId, defaultModel.getArtifactId());
                variablesValuesMap.put(ETalendMavenVariables.CodesVersion, defaultModel.getVersion());
                variablesValuesMap.put(ETalendMavenVariables.CodesName, codesName);
                variablesValuesMap.put(ETalendMavenVariables.CodesPackage, codesPackage);

                variablesValuesMap.put(ETalendMavenVariables.ProjectName, projectTechName);

                model.setGroupId(ETalendMavenVariables.replaceVariables(model.getGroupId(), variablesValuesMap));
                model.setArtifactId(ETalendMavenVariables.replaceVariables(model.getArtifactId(), variablesValuesMap));
                model.setVersion(ETalendMavenVariables.replaceVariables(model.getVersion(), variablesValuesMap));
                model.setName(ETalendMavenVariables.replaceVariables(model.getName(), variablesValuesMap));

                Properties properties = model.getProperties();
                Iterator<Object> iterator = properties.keySet().iterator();
                while (iterator.hasNext()) {
                    String key = iterator.next().toString();
                    Object object = properties.get(key);
                    if (object != null) {
                        String oldValue = object.toString();
                        String newValue = ETalendMavenVariables.replaceVariables(oldValue, variablesValuesMap);
                        properties.setProperty(key, newValue);
                    }
                }
                return model;
            }
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
        return defaultModel; // if error, try to use default model
    }

}
