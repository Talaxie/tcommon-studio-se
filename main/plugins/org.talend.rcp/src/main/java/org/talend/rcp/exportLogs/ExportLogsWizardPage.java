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
package org.talend.rcp.exportLogs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.utils.VersionUtils;
import org.talend.commons.utils.workbench.resources.ResourceUtils;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.runtime.maven.MavenArtifact;
import org.talend.core.runtime.services.IGenericService;
import org.talend.core.services.ICoreTisService;
import org.talend.core.ui.branding.IBrandingService;
import org.talend.rcp.i18n.Messages;
import org.talend.repository.ProjectManager;
import org.talend.repository.ui.utils.ZipToFile;
import org.talend.repository.ui.wizards.exportjob.util.ExportJobUtil;
import org.talend.utils.json.JSONArray;
import org.talend.utils.json.JSONException;
import org.talend.utils.json.JSONObject;

import com.sun.management.OperatingSystemMXBean;

/**
 * wzhang class global comment. Detailled comment
 */
public class ExportLogsWizardPage extends WizardPage {

    private Label logsFromArchiveLabel;

    private Text archivePathField;

    private Button browseArchivesButton;

    private String previouslyBrowsedArchive = ""; //$NON-NLS-1$

    private static final String[] FILE_EXPORT_MASK = { "*.zip", "*.*" }; //$NON-NLS-1$ //$NON-NLS-2$

    private String lastPath;

    private static final int CPUTIME = 30;

    private static final int PERCENT = 100;

    private static final int FAULTLENGTH = 10;

    private static final String NEW_LINE = System.lineSeparator();

    private static final String BLANK = "  "; //$NON-NLS-1$
    
    /** The extension used for log files */
    private static final String LOG_EXT = ".log"; //$NON-NLS-1$
    /** The extension markup to use for backup log files*/
    private static final String BACKUP_MARK = ".bak_"; //$NON-NLS-1$

    protected ExportLogsWizardPage(String pageName) {
        super(pageName);
        setDescription(Messages.getString("ExportLogsWizardPage.exportLog")); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControl(Composite parent) {
        Composite workArea = new Composite(parent, SWT.NONE);
        setControl(workArea);
        workArea.setLayout(new GridLayout());
        workArea.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        createLogsRoot(workArea);
    }

    private void createLogsRoot(Composite workArea) {
        Composite projectGroup = new Composite(workArea, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.makeColumnsEqualWidth = false;
        layout.marginWidth = 0;
        projectGroup.setLayout(layout);
        projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        logsFromArchiveLabel = new Label(projectGroup, SWT.NONE);
        // logsFromArchiveRadio.setText(DataTransferMessages.WizardProjectsImportPage_ArchiveSelectTitle);
        logsFromArchiveLabel.setText(Messages.getString("DataTransferMessages.WizardProjectsImportPage_ArchiveSelectTitle")); //$NON-NLS-1$

        archivePathField = new Text(projectGroup, SWT.BORDER);

        archivePathField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        browseArchivesButton = new Button(projectGroup, SWT.PUSH);
        // browseArchivesButton.setText(DataTransferMessages.DataTransfer_browse);
        browseArchivesButton.setText(Messages.getString("DataTransferMessages.DataTransfer_browse")); //$NON-NLS-1$
        setButtonLayoutData(browseArchivesButton);

        this.setPageComplete(false);

        browseArchivesButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                handleLocationArchiveButtonPressed();
            }
        });

        archivePathField.addTraverseListener(new TraverseListener() {

            @Override
            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    e.doit = false;
                    lastPath = archivePathField.getText().trim();
                }
            }
        });

        archivePathField.addFocusListener(new FocusAdapter() {

            @Override
            public void focusLost(org.eclipse.swt.events.FocusEvent e) {
                lastPath = archivePathField.getText().trim();
            }
        });
        archivePathField.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                if (archivePathField.getText() != null && !"".equals(archivePathField.getText().trim())) { //$NON-NLS-1$
                    lastPath = archivePathField.getText().trim();
                    ExportLogsWizardPage.this.setPageComplete(true);
                } else {
                    ExportLogsWizardPage.this.setPageComplete(false);
                }
            }
        });
    }

    protected void handleLocationArchiveButtonPressed() {

        FileDialog dialog = new FileDialog(archivePathField.getShell(), SWT.SAVE);
        dialog.setFilterExtensions(FILE_EXPORT_MASK);
        // dialog.setText(DataTransferMessages.ArchiveExport_selectDestinationTitle);
        dialog.setText(Messages.getString("DataTransferMessages.ArchiveExport_selectDestinationTitle")); //$NON-NLS-1$

        String fileName = archivePathField.getText().trim();
        if (fileName.length() == 0) {
            fileName = previouslyBrowsedArchive;
        }

        if (fileName.length() == 0) {
            // dialog.setFilterPath(IDEWorkbenchPlugin.getPluginWorkspace().getRoot().getLocation().toOSString());
            dialog.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString());
        } else {
            File path = new File(fileName);
            if (path.exists()) {
                dialog.setFilterPath(new Path(fileName).toOSString());
            }
        }

        String selectedArchive = dialog.open();
        if (selectedArchive != null) {
            previouslyBrowsedArchive = selectedArchive;
            archivePathField.setText(previouslyBrowsedArchive);
            lastPath = archivePathField.getText().trim();
        }
    }

    public boolean performCancel() {
        return true;
    }

    public boolean performFinish() {
        if (!checkExportFile()) {
            return false;
        }
        try {
            File file = new File(lastPath);
            exportIni(file);
            exportSysconfig(file);
            exportLogs(file);
            exportPerformanceLogs(file);
            exportStudioInfo(file);
            exportRequiredJson(file);
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
        ExportJobUtil.deleteTempFiles();
        return true;
    }

    private boolean checkExportFile() {
        if (lastPath == null || "".equals(lastPath.trim())) { //$NON-NLS-1$
            MessageDialog.openError(getShell(),
                    Messages.getString("ExportLogsWizardPage.error"), Messages.getString("ExportLogsWizardPage.errorMess")); //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        }
        File file = new File(lastPath.trim());
        if (file.exists()) {
            boolean confirm = MessageDialog.openConfirm(getShell(), Messages.getString("ExportLogsWizardPage.confirm"), //$NON-NLS-1$
                    Messages.getString("ExportLogsWizardPage.confirmMsg")); //$NON-NLS-1$
            return confirm;
        }
        return true;
    }

    private void exportIni(File dest) throws Exception {
        String zipFile = dest.getAbsolutePath();

        String tmpFolder = ExportJobUtil.getTmpFolder();
        File configFolder = new File(Platform.getConfigurationLocation().getURL().toURI());
        File configIniFile = new File(configFolder, "config.ini");
        if (configIniFile.exists()) {
            zipLogFileWithSensitiveDataHidden(zipFile, tmpFolder, configIniFile.getCanonicalPath());
        }

        File installFolder = new File(Platform.getInstallLocation().getURL().toURI());
        File launcherIniFile = null;
        String launcherName = System.getProperty("eclipse.launcher.name");
        if (StringUtils.isNotBlank(launcherName)) {
            launcherIniFile = new File(installFolder, launcherName + ".ini");
        }
        if (launcherIniFile == null || !launcherIniFile.exists()) {
            String os = Platform.getOS();
            String osArch = Platform.getOSArch();
            String ws = Platform.getWS();
            String launcherIniFileSuffix = null;
            if (Platform.OS_MACOSX.equals(os)) {
                launcherIniFileSuffix = "-macosx-cocoa.ini";
            } else if (Platform.OS_WIN32.equals(os)) {
                launcherIniFileSuffix = "-win-" + osArch + ".ini";
            } else {
                launcherIniFileSuffix = "-" + os + "-" + ws + "-" + osArch + ".ini";
            }
            String suffix = launcherIniFileSuffix;
            File[] matchedFiles = installFolder.listFiles(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    if (name.endsWith(suffix)) {
                        return true;
                    }
                    return false;
                }
            });
            for (File file : matchedFiles) {
                zipLogFileWithSensitiveDataHidden(zipFile, tmpFolder, file.getCanonicalPath());
            }
        } else {
            zipLogFileWithSensitiveDataHidden(zipFile, tmpFolder, launcherIniFile.getCanonicalPath());
        }
    }

    private void zipLogFileWithSensitiveDataHidden(String zipFile, String tmpFolder, String logFile) {
        String destFile = new File(tmpFolder + File.separator + new File(logFile).getName()).getAbsolutePath();
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile));
                BufferedWriter writer = new BufferedWriter(new FileWriter(destFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String processedLine = line;
                int equalsIndex = line.indexOf('=');
                if (equalsIndex != -1) {
                    String key = line.substring(0, equalsIndex).trim();
                    if (key.toLowerCase().contains("password")) {
                        processedLine = key + "=" + "***";
                    }
                }
                writer.write(processedLine);
                writer.newLine();
            }
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }

        try {
            ZipToFile.zipFile(tmpFolder, zipFile);
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
    }

    private void exportLogs(File dest) throws Exception {
        String zipFile = dest.getAbsolutePath();

        String tmpFolder = ExportJobUtil.getTmpFolder();
        IPath logFileLocation = Platform.getLogFileLocation();
        String logFile = logFileLocation.toOSString();
        zipLogFile(zipFile, tmpFolder, logFile);// zip .log 
        
        //
        String logFileName = logFileLocation.lastSegment();
        if (logFileName.toLowerCase().endsWith(LOG_EXT)) {
            logFileName = logFileName.substring(0, logFileName.length() - LOG_EXT.length());
        }
        String _backlogPattern = logFileName + BACKUP_MARK + "\\d+";
        if (logFileLocation.lastSegment().toLowerCase().endsWith(LOG_EXT)) {
            _backlogPattern += LOG_EXT;
        }
        
        final String backlogPattern = _backlogPattern;
        IPath parentPath = logFileLocation.removeLastSegments(1);
        String[] back_logs = parentPath.makeAbsolute().toFile().list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches(backlogPattern);
            }
        });
        
        for(String logfile:back_logs) {
            String backlogFile = parentPath.append(logfile).toOSString();
            zipLogFile(zipFile, tmpFolder, backlogFile);//zip .bak_X.log
        }
    }

    private void zipLogFile(String zipFile, String tmpFolder, String logFile) throws Exception {
        try {
            String destFile = new File(tmpFolder + File.separator + new File(logFile).getName()).getAbsolutePath();
            ZipToFile.copyFile(logFile, destFile);
            ZipToFile.zipFile(tmpFolder, zipFile);
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
    }

    private void exportPerformanceLogs(File dest) throws Exception {
        String zipFile = dest.getAbsolutePath();

        String tmpFolder = ExportJobUtil.getTmpFolder();
        
        IPath logFileLocation = Platform.getLogFileLocation();
        IPath parentPath = logFileLocation.removeLastSegments(1);
        
        String performanceLogFile = "performance.log";
        IPath performanceLog = parentPath.append(performanceLogFile);
        zipLogFile(zipFile, tmpFolder, performanceLog.toOSString());//zip performance.log
        
        final String backlogPattern = performanceLogFile + "\\.\\d+";
        String[] back_logs = parentPath.makeAbsolute().toFile().list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches(backlogPattern);
            }
        });
        for(String logfile:back_logs) {
            String backlogFile = parentPath.append(logfile).toOSString();
            zipLogFile(zipFile, tmpFolder, backlogFile);//zip performance.log.X file
        }
    }
    
    private void exportStudioInfo(File dest) {

        StringBuffer info = new StringBuffer();
        info.append("********Studio Information********").append(NEW_LINE); //$NON-NLS-1$
        info.append(NEW_LINE);

        info.append("-----Branding Details-----").append(NEW_LINE); //$NON-NLS-1$
        if (GlobalServiceRegister.getDefault().isServiceRegistered(IBrandingService.class)) {
            IBrandingService brandingService = (IBrandingService) GlobalServiceRegister.getDefault().getService(
                    IBrandingService.class);
            String productName = brandingService.getFullProductName();
            String productInternalVersion = VersionUtils.getInternalVersion();
            info.append("Product Name: ").append(productName).append(NEW_LINE); //$NON-NLS-1$
            info.append("Product Version: ").append(productInternalVersion).append(NEW_LINE); //$NON-NLS-1$
        }
        info.append(NEW_LINE);

        info.append("-----Installed Addons-----").append(NEW_LINE); //$NON-NLS-1$
        IPreferenceStore preferenceStore = PlatformUI.getPreferenceStore();
        String addons = preferenceStore.getString("ADDONS"); //$NON-NLS-1$
        JSONObject jsonObject = null;
        try {
            if (addons != null && !"".equals(addons)) { //$NON-NLS-1$
                jsonObject = new JSONObject(addons);
            }
        } catch (JSONException e) {
            info.append("Failed to get addons information...").append(NEW_LINE); //$NON-NLS-1$
        }
        if (jsonObject != null) {
            JSONArray jsonArray = jsonObject.names();
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        String addon = jsonArray.getString(i);
                        info.append(addon).append(NEW_LINE);
                    } catch (JSONException e) {
                        info.append("Failed to get addon information...").append(NEW_LINE); //$NON-NLS-1$
                    }
                }
            }
        }
        info.append(NEW_LINE);

        info.append("-----Installed Components-----").append(NEW_LINE); //$NON-NLS-1$
        if (GlobalServiceRegister.getDefault().isServiceRegistered(IGenericService.class)) {
            IGenericService genericService = (IGenericService) GlobalServiceRegister.getDefault().getService(
                    IGenericService.class);
            List<Map<String, String>> componentsInfo = genericService.getAllGenericComponentsInfo();
            for (Map<String, String> componentInfo : componentsInfo) {
                for (Map.Entry<String, String> entry : componentInfo.entrySet()) {
                    info.append(entry.getKey()).append(": ").append(entry.getValue()).append(BLANK); //$NON-NLS-1$
                }
                info.append(NEW_LINE);
            }
            info.append(NEW_LINE);
        }

        info.append("-----Installed Patches-----").append(NEW_LINE); //$NON-NLS-1$
        if (GlobalServiceRegister.getDefault().isServiceRegistered(ICoreTisService.class)) {
            ICoreTisService coreTisService = (ICoreTisService) GlobalServiceRegister.getDefault().getService(
                    ICoreTisService.class);
            List<MavenArtifact> installedPatchArtifacts = null;
            try {
                installedPatchArtifacts = coreTisService.getInstalledPatchArtifacts();
            } catch (BackingStoreException e) {
                info.append("Failed to get installed patches information...").append(NEW_LINE); //$NON-NLS-1$
                ExceptionHandler.process(e);
            }
            if (installedPatchArtifacts != null) {
                for (MavenArtifact artifact : installedPatchArtifacts) {
                    info.append("GroupId: ").append(artifact.getGroupId()).append(BLANK); //$NON-NLS-1$
                    info.append("Version: ").append(artifact.getVersion()).append(BLANK); //$NON-NLS-1$
                    info.append("ArtifactId: ").append(artifact.getArtifactId()).append(NEW_LINE); //$NON-NLS-1$
                }
            }
        }
        writeToFile(dest, "studioInfo.log", info); //$NON-NLS-1$
    }
    
    private void exportRequiredJson(File dest) throws Exception {
        IFile featureFile = ResourceUtils.getProject(ProjectManager.getInstance().getCurrentProject()).getFolder(".settings") //$NON-NLS-1$
                .getFile("requiredFeatures.json"); //$NON-NLS-1$
        if (featureFile.exists()) {
            String zipFile = dest.getAbsolutePath();
            String tmpFolder = ExportJobUtil.getTmpFolder();
            zipLogFile(zipFile, tmpFolder, featureFile.getLocation().toPortableString());
        }
    }

    private void exportSysconfig(File dest) {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage memoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage memoryNoHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
        long used = memoryUsage.getUsed() / (1024 * 1024);
        String usedMemo = String.valueOf(used) + "MB"; //$NON-NLS-1$
        long max = memoryUsage.getMax() / (1024 * 1024);
        String maxMemo = String.valueOf(max) + "MB"; //$NON-NLS-1$
        long committed = memoryUsage.getCommitted() / (1024 * 1024);
        String committedMemo = String.valueOf(committed) + "MB"; //$NON-NLS-1$

        long noHeapUsed = memoryNoHeapUsage.getUsed() / (1024 * 1024);
        String noHeapUsedMemo = String.valueOf(noHeapUsed) + "MB"; //$NON-NLS-1$
        long noHeapMaxUnUsed = memoryNoHeapUsage.getMax() / (1024 * 1024);
        String noHeapMaxUnUsedMemo = String.valueOf(noHeapMaxUnUsed) + "MB"; //$NON-NLS-1$
        long noHeapCommitted = memoryNoHeapUsage.getCommitted() / (1024 * 1024);
        String noHeapCommittedMemo = String.valueOf(noHeapCommitted) + "MB"; //$NON-NLS-1$

        OperatingSystemMXBean osmxb = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long totalPhysicalMemo = osmxb.getTotalPhysicalMemorySize() / (1024 * 1024);
        String totalPhysicalMemorySize = String.valueOf(totalPhysicalMemo) + "MB"; //$NON-NLS-1$
        long freePhysicalMemo = osmxb.getFreePhysicalMemorySize() / (1024 * 1024);
        String freePhysicalMemorySize = String.valueOf(freePhysicalMemo) + "MB"; //$NON-NLS-1$

        // get thread count
        ThreadGroup parentThread;
        for (parentThread = Thread.currentThread().getThreadGroup(); parentThread.getParent() != null; parentThread = parentThread
                .getParent()) {
            ;
        }
        int totalThread = parentThread.activeCount();

        String totalThreadCount = String.valueOf(totalThread);

        String osName = System.getProperty("os.name"); //$NON-NLS-1$
        // get CPU ratio, need a little long time.
        int cpuRatio = 0;
        if (osName.toLowerCase().startsWith("windows")) { //$NON-NLS-1$
            cpuRatio = (int) this.getCpuRatioForWindows();
        }
        String cpuUsed = String.valueOf(cpuRatio) + "%"; //$NON-NLS-1$
        StringBuffer sb = new StringBuffer();

        Properties p = new Properties();
        p.putAll(System.getProperties());
        sb.append(" *** System properties\n"); //$NON-NLS-1$
        for (Entry<Object, Object> en : p.entrySet()) {
            sb.append(en.getKey() + "=" + en.getValue() + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        p.clear();
        p.put("Current CPU usage", cpuUsed); //$NON-NLS-1$
        p.put("Current thread count", totalThreadCount); //$NON-NLS-1$
        p.put("Total physical memory", totalPhysicalMemorySize); //$NON-NLS-1$
        p.put("Free physical memory", freePhysicalMemorySize); //$NON-NLS-1$
        p.put("Heap usage memory", usedMemo); //$NON-NLS-1$
        p.put("Heap maximum memory can be used", maxMemo); //$NON-NLS-1$
        p.put("Heap committed memory for JVM", committedMemo); //$NON-NLS-1$
        p.put("Non-Heap usage memory", noHeapUsedMemo); //$NON-NLS-1$
        p.put("Non-Heap maximum memory can be used", noHeapMaxUnUsedMemo); //$NON-NLS-1$
        p.put("Non-Heap committed memory for JVM", noHeapCommittedMemo); //$NON-NLS-1$

        sb.append("\n ***CPU&Memory properties\n"); //$NON-NLS-1$
        for (Entry<Object, Object> en : p.entrySet()) {
            sb.append(en.getKey() + "=" + en.getValue() + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        StringBuffer processedData = processSensitiveDataHidden(sb.toString());
        writeToFile(dest, ".sysConfig", processedData); //$NON-NLS-1$
    }

    private StringBuffer processSensitiveDataHidden(String data) {
        StringBuffer processedData = new StringBuffer();
        while (true) {
            String line;
            int index = data.indexOf("\n");
            if (index == -1) {
                line = data.toString();
                String processedLine = line;
                int equalsIndex = line.indexOf('=');
                if (equalsIndex != -1) {
                    String key = line.substring(0, equalsIndex).trim();
                    if (key.toLowerCase().contains("password")) {
                        processedLine = key + "=" + "***";
                    }
                }
                processedData.append(processedLine);
                break;
            }
            line = data.substring(0, index);
            String processedLine = line;
            int equalsIndex = line.indexOf('=');
            if (equalsIndex != -1) {
                String key = line.substring(0, equalsIndex).trim();
                if (key.toLowerCase().contains("password")) {
                    processedLine = key + "=" + "***";
                }
            }
            processedData.append(processedLine).append("\n");
            data = data.substring(index + 1);
        }
        return processedData;
    }

    private void writeToFile(File dest, String fileName, StringBuffer sb) {
        String zipFile = dest.getAbsolutePath();
        String tmpFolder = ExportJobUtil.getTmpFolder();
        String destFile = new File(tmpFolder + File.separator + new File(fileName)).getAbsolutePath();
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(destFile);
            out.write(sb.toString().getBytes());
            out.flush();
            ZipToFile.zipFile(tmpFolder, zipFile);
        } catch (Exception e) {
            ExceptionHandler.process(e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                ExceptionHandler.process(e);
            }
        }
    }

    private double getCpuRatioForWindows() {
        try {
            String procCmd = System.getenv("windir") + "\\system32\\wbem\\wmic.exe process get Caption,CommandLine," //$NON-NLS-1$ //$NON-NLS-2$
                    + "KernelModeTime,ReadOperationCount,ThreadCount,UserModeTime,WriteOperationCount"; //$NON-NLS-1$
            long[] c0 = readCpu(Runtime.getRuntime().exec(procCmd));
            Thread.sleep(CPUTIME);
            long[] c1 = readCpu(Runtime.getRuntime().exec(procCmd));
            if (c0 != null && c1 != null) {
                long idletime = c1[0] - c0[0];
                long busytime = c1[1] - c0[1];
                return Double.valueOf(PERCENT * (busytime) / (busytime + idletime)).doubleValue();
            } else {
                return 0.0;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0.0;
        }
    }

    private long[] readCpu(final Process proc) {
        long[] retn = new long[2];
        try {
            proc.getOutputStream().close();
            InputStreamReader ir = new InputStreamReader(proc.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            String line = input.readLine();
            if (line == null || line.length() < FAULTLENGTH) {
                return null;
            }
            int capidx = line.indexOf("Caption"); //$NON-NLS-1$
            int cmdidx = line.indexOf("CommandLine"); //$NON-NLS-1$
            int rocidx = line.indexOf("ReadOperationCount"); //$NON-NLS-1$
            int umtidx = line.indexOf("UserModeTime"); //$NON-NLS-1$
            int kmtidx = line.indexOf("KernelModeTime"); //$NON-NLS-1$
            int wocidx = line.indexOf("WriteOperationCount"); //$NON-NLS-1$
            long idletime = 0;
            long kneltime = 0;
            long usertime = 0;
            while ((line = input.readLine()) != null) {
                if (line.length() < wocidx) {
                    continue;
                }
                String caption = substring(line, capidx, cmdidx - 1).trim();
                String cmd = substring(line, cmdidx, kmtidx - 1).trim();
                if (cmd.indexOf("wmic.exe") >= 0) { //$NON-NLS-1$
                    continue;
                }
                if (caption.equals("System Idle Process") || caption.equals("System")) { //$NON-NLS-1$ //$NON-NLS-2$
                    idletime += Long.valueOf(substring(line, kmtidx, rocidx - 1).trim()).longValue();
                    idletime += Long.valueOf(substring(line, umtidx, wocidx - 1).trim()).longValue();
                    continue;
                }
                kneltime += Long.valueOf(substring(line, kmtidx, rocidx - 1).trim()).longValue();
                usertime += Long.valueOf(substring(line, umtidx, wocidx - 1).trim()).longValue();
            }
            retn[0] = idletime;
            retn[1] = kneltime + usertime;
            return retn;
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                proc.getInputStream().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static String substring(String src, int startIndex, int endIndex) {
        byte[] b = src.getBytes();
        String tgt = ""; //$NON-NLS-1$
        for (int i = startIndex; i <= endIndex; i++) {
            tgt += (char) b[i];
        }
        return tgt;
    }

}
