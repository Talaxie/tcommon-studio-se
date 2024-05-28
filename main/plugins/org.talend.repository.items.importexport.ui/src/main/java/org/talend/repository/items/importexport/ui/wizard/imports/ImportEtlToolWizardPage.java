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
package org.talend.repository.items.importexport.ui.wizard.imports;

import java.awt.FlowLayout;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import javax.swing.*;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.wizards.datatransfer.TarException;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.LoginException;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.runtime.model.emf.provider.EmfResourcesFactoryReader;
import org.talend.commons.runtime.model.emf.provider.ResourceOption;
import org.talend.commons.runtime.model.repository.ERepositoryStatus;
import org.talend.commons.runtime.service.ITaCoKitService;
import org.talend.commons.ui.runtime.image.EImage;
import org.talend.commons.ui.runtime.image.ImageProvider;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.PluginChecker;
import org.talend.core.model.properties.Item;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryPrefConstants;
import org.talend.core.model.repository.RepositoryManager;
import org.talend.core.model.repository.RepositoryViewObject;
import org.talend.core.model.utils.TalendPropertiesUtil;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.service.IExchangeService;
import org.talend.core.service.IStudioLiteP2Service;
import org.talend.core.service.IStudioLiteP2Service.IInstallableUnitInfo;
import org.talend.core.ui.advanced.composite.FilteredCheckboxTree;
import org.talend.core.ui.branding.IBrandingService;
import org.talend.core.ui.component.ComponentPaletteUtilities;
import org.talend.designer.core.IMultiPageTalendEditor;
import org.talend.designer.maven.tools.AggregatorPomsHelper;
import org.talend.designer.maven.tools.MavenPomSynchronizer;
import org.talend.migration.MigrationReportHelper;
import org.talend.repository.items.importexport.handlers.ImportExportHandlersManager;
import org.talend.repository.items.importexport.handlers.imports.IImportItemsHandler;
import org.talend.repository.items.importexport.handlers.imports.ImportBasicHandler;
import org.talend.repository.items.importexport.handlers.imports.ImportCacheHelper;
import org.talend.repository.items.importexport.handlers.imports.ImportDependencyRelationsHelper;
import org.talend.repository.items.importexport.handlers.model.EmptyFolderImportItem;
import org.talend.repository.items.importexport.handlers.model.ImportItem;
import org.talend.repository.items.importexport.manager.ResourcesManager;
import org.talend.repository.items.importexport.ui.dialog.ShowErrorsDuringImportItemsDialog;
import org.talend.repository.items.importexport.ui.i18n.Messages;
import org.talend.repository.items.importexport.ui.managers.FileResourcesUnityManager;
import org.talend.repository.items.importexport.ui.managers.ResourcesManagerFactory;
import org.talend.repository.items.importexport.ui.wizard.imports.providers.ImportItemsViewerContentProvider;
import org.talend.repository.items.importexport.ui.wizard.imports.providers.ImportItemsViewerFilter;
import org.talend.repository.items.importexport.ui.wizard.imports.providers.ImportItemsViewerLabelProvider;
import org.talend.repository.items.importexport.ui.wizard.imports.providers.ImportItemsViewerSorter;
import org.talend.repository.items.importexport.wizard.models.FolderImportNode;
import org.talend.repository.items.importexport.wizard.models.ImportNode;
import org.talend.repository.items.importexport.wizard.models.ImportNodesBuilder;
import org.talend.repository.items.importexport.wizard.models.ItemImportNode;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.IRepositoryNode;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.RepositoryNodeUtilities;
import org.talend.repository.ui.dialog.AProgressMonitorDialogWithCancel;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.talend.core.ui.webService.Webhook;
import org.talend.core.prefs.ITalendCorePrefConstants;
import org.talend.core.ui.CoreUIPlugin;
import org.talend.repository.ProjectManager;
import org.talend.core.context.RepositoryContext;
import org.talend.core.CorePlugin;
import org.talend.core.context.Context;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.talend.core.model.properties.ProcessItem;
import org.talend.commons.exception.CommonExceptionHandler;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.repository.seeker.RepositorySeekerManager;
import org.talend.core.model.utils.RepositoryManagerHelper;
import org.eclipse.jface.viewers.StructuredSelection;
import org.talend.commons.exception.CommonExceptionHandler;
import org.talend.repository.ui.views.IRepositoryView;
import org.talend.core.model.process.IProcess2;
import org.talend.core.runtime.CoreRuntimePlugin;
import org.talend.core.model.update.RepositoryUpdateManager;
import org.eclipse.ui.IWorkbenchPage;
import org.talend.core.model.properties.Property;
import org.talend.core.ui.editor.JobEditorInput;

import org.osgi.framework.FrameworkUtil;
import javax.swing.*;

/*
import org.talend.designer.core.ui.AbstractMultiPageTalendEditor;
import org.talend.designer.core.ui.MultiPageTalendEditor;
*/

/**
 *
 * DOC ggu class global comment. Detailled comment
 */
public class ImportEtlToolWizardPage extends WizardPage {

    private static final Logger LOGGER = Logger.getLogger(ImportEtlToolWizardPage.class);

    private static final String TYPE_BEANS = "BEANS";
    private static final String TALEND_FILE_NAME = "talend.project";

    private IStructuredSelection selection;

    private final ImportExportHandlersManager importManager = new ImportExportHandlersManager();

    private Button regenIdBtn;

    private String projectLabel;

    private TreeViewer jobTreeViewer;

    private static ImportNodesBuilder nodesBuilder = new ImportNodesBuilder();

	private class Job {
		private String sequenceur;
		private String type;

		private boolean checked;
		
		public Job(String sequenceur, String type) {
			this.sequenceur = sequenceur;
			this.type = type;
		}
		
		public String getSequenceur() {
			return sequenceur;
		}
		
		public String getType() {
			return type;
		}
		
		public boolean isChecked() {
			return checked;
		}
		
		public void setChecked(boolean checked) {
			this.checked = checked;
		}
	}

	private class MyLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof Job) {
				return ((Job) element).getSequenceur();
			}
			return super.getText(element);
		}
	}

    /**
     *
     * DOC ggu ImportEtlToolWizardPage constructor comment.
     *
     * @param pageName
     */
    public ImportEtlToolWizardPage(String pageName, IStructuredSelection s) {
        super(pageName);
        this.selection = s;
        setTitle("EtlTool"); //$NON-NLS-1$
        setDescription(Messages.getString("ImportEtlToolWizardPage_importDescription")); //$NON-NLS-1$
        setImageDescriptor(ImageProvider.getImageDesc(EImage.ETLTOOL_64));
        projectLabel = ProjectManager.getInstance().getCurrentProject().getTechnicalLabel();
    }

    public IStructuredSelection getSelection() {
        return this.selection;
    }

    @Override
    public void createControl(Composite parent) {
        ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL);
        setControl(scrolledComposite);
        scrolledComposite.setLayout(new GridLayout());
        scrolledComposite.setLayoutData( new GridData(GridData.FILL_BOTH));
        
        Composite composite = new Composite(scrolledComposite, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        scrolledComposite.setContent(composite);
        
        if (!CoreUIPlugin.getDefault().getPreferenceStore().getBoolean(ITalendCorePrefConstants.WEBHOOK_ETLTOOL_ENABLED)) {
            Label noWebhookLabel = new Label(composite, SWT.NONE);
            noWebhookLabel.setText("No webhook defined !");
        } else {
            Text searchText = new Text(composite, SWT.BORDER);
            searchText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            searchText.setText("");
            searchText.setMessage("Search...");

            Button searchButton = new Button(composite, SWT.NONE);
            searchButton.setImage(ImageProvider.getImage(EImage.REFRESH_ICON));
            searchButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
	        searchButton.addSelectionListener(new SelectionAdapter() {
	            @Override
	            public void widgetSelected(SelectionEvent e) {
                    fillJobs();
	            }
	        });

            jobTreeViewer = new CheckboxTreeViewer(composite, SWT.BORDER);
            jobTreeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
            jobTreeViewer.setContentProvider(new ITreeContentProvider() {
                @Override
                public Object[] getElements(Object inputElement) {
                    if (inputElement instanceof List) {
                        return ((List<Job>) inputElement).toArray();
                    }
                    return null;
                }

                @Override
                public Object[] getChildren(Object parentElement) {
                    return null;
                }

                @Override
                public Object getParent(Object element) {
                    return null;
                }

                @Override
                public boolean hasChildren(Object element) {
                    return false;
                }
            });
            jobTreeViewer.setLabelProvider((IBaseLabelProvider)new MyLabelProvider());

            // Ajoutez une case à cocher à chaque élément du TreeViewer
            ((CheckboxTreeViewer) jobTreeViewer).setCheckStateProvider(new ICheckStateProvider() {
                @Override
                public boolean isChecked(Object element) {
                    if (element instanceof Job) {
                        return ((Job) element).isChecked();
                    }
                    return false;
                }
                
                @Override
                public boolean isGrayed(Object element) {
                    return false;
                }
            });

            // Ajoutez un ModifyListener pour mettre à jour le filtre de la table
            searchText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    String filter = searchText.getText();
                    if (filter.isEmpty()) {
                        jobTreeViewer.resetFilters();
                    } else {
                        jobTreeViewer.setFilters(new ViewerFilter[] { new ViewerFilter() {
                            @Override
                            public boolean select(Viewer jobTreeViewer, Object parentElement, Object element) {
                                if (element instanceof Job) {
                                    return ((Job) element).getSequenceur().toLowerCase().contains(filter.toLowerCase());
                                }
                                return true;
                            }
                        } });
                    }
                }
            });
        }
        fillJobs();

        scrolledComposite.setContent(composite);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);
        scrolledComposite.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        Dialog.applyDialogFont(composite);
    }

    public void fillJobs() {
        try {
            // Open wait dialog
            Webhook webhook = new Webhook();
            webhook.loadingDialogOpen();

            List<HashMap<String, String>> jobItems = Webhook.projetTree("ref_DEV", projectLabel, "Talend");
            List<Job> jobs = new ArrayList<>();
            for(HashMap<String, String> jobItem:jobItems){  
                jobs.add(new Job(jobItem.get("Sequenceur"), jobItem.get("Type")));
            }
            jobTreeViewer.setInput(jobs);
            
            // Close wait dialog
            webhook.loadingDialogClose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean importItems(String zipPath, IProgressMonitor monitor, final boolean overwrite, final boolean openThem, boolean needMigrationTask) throws IOException {
        ZipFile srcZipFile = new ZipFile(zipPath);
        final ResourcesManager resourcesManager = ResourcesManagerFactory.getInstance().createResourcesManager(srcZipFile);
        final ResourceOption importOption = ResourceOption.DEMO_IMPORTATION;
        try {
            EmfResourcesFactoryReader.INSTANCE.addOption(importOption);

            resourcesManager.collectPath2Object(srcZipFile);
            final ImportExportHandlersManager importManager = new ImportExportHandlersManager();
            final List<ImportItem> items = populateItems(importManager, resourcesManager, monitor, overwrite);
            final List<String> itemIds = new ArrayList<String>();

            for (ImportItem itemRecord : items) {
                Item item = itemRecord.getProperty().getItem();
                if (item instanceof ProcessItem) {
                    // only select jobs
                    itemIds.add(item.getProperty().getId());
                }
                IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
                if (item.getState().isLocked()) {
                    factory.unlock(item);
                }
                ERepositoryStatus status = factory.getStatus(item);
                if (status != null && status == ERepositoryStatus.LOCK_BY_USER) {
                    factory.unlock(item);
                }
                if (!needMigrationTask) {
                    itemRecord.setMigrationTasksToApply(null);
                }
            }
            // importManager.importItemRecords(new NullProgressMonitor(), resourcesManager, items, overwrite,
            // nodesBuilder.getAllImportItemRecords(), null);
            if (items != null && !items.isEmpty()) {
                importManager.importItemRecords(monitor, resourcesManager, items, overwrite, nodesBuilder.getAllImportItemRecords(), null);
            }
        } catch (Exception e) {
            CommonExceptionHandler.process(e);
        } finally {
            // clean
            if (resourcesManager != null) {
                resourcesManager.closeResource();
            }
            nodesBuilder.clear();

            EmfResourcesFactoryReader.INSTANCE.removOption(importOption);
        }
        return true;
    }

    private static List<ImportItem> populateItems(final ImportExportHandlersManager importManager, final ResourcesManager resourcesManager, IProgressMonitor monitor, final boolean overwrite) {
        List<ImportItem> selectedItemRecords = new ArrayList<ImportItem>();
        nodesBuilder.clear();
        if (resourcesManager != null) { // if resource is not init successfully.
            try {
                // List<ImportItem> items = importManager.populateImportingItems(resourcesManager, overwrite,
                // new NullProgressMonitor(), true);
                List<ImportItem> items = importManager.populateImportingItems(resourcesManager, overwrite, monitor, true);
                nodesBuilder.addItems(items);
            } catch (Exception e) {
                CommonExceptionHandler.process(e);
            }
        }
        ImportItem[] allImportItemRecords = nodesBuilder.getAllImportItemRecords();
        selectedItemRecords.addAll(Arrays.asList(allImportItemRecords));
        Iterator<ImportItem> itemIterator = selectedItemRecords.iterator();
        while (itemIterator.hasNext()) {
            ImportItem item = itemIterator.next();
            if (!item.isValid()) {
                itemIterator.remove();
            }
        }
        return selectedItemRecords;
    }

    private static boolean isJobAlreadyOpened(String jobName) {
        List<IProcess2> openedProcessList = CoreRuntimePlugin.getInstance().getDesignerCoreService()
                .getOpenedProcess(RepositoryUpdateManager.getEditors());
        if (openedProcessList == null || openedProcessList.isEmpty()) {
            return false;
        }
        for (IProcess2 process : openedProcessList) {
            if (jobName.equals(process.getName())) {
                return true;
            }
        }
        return false;
    }

    /*
    private static String getEditorId() {
        return MultiPageTalendEditor.ID;
    }
    */

    @Override
    public boolean isPageComplete() {
        return super.isPageComplete();
    }

    public boolean performCancel() {
        return true;
    }

    public boolean performFinish() {
        // Open wait dialog
        Webhook webhook = new Webhook();
        webhook.loadingDialogOpen();

        Object[] checkedElements = ((CheckboxTreeViewer) jobTreeViewer).getCheckedElements();
        for (Object element : checkedElements) {
            try {
                String sequenceur = ((Job) element).getSequenceur(); // "SYNCLI_000_Master";
                HashMap<String, String> job = Webhook.JobArchiveGet("ref_DEV", projectLabel, sequenceur, "Talend");
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("performFinish");
                    LOGGER.info(job);
                }
                
                /*
                String fileUrl = job.get("fileUrl");
                String workspaceLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
                String jobZipPath = workspaceLocation + File.separator + sequenceur + ".zip";
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(fileUrl);
                    LOGGER.info(workspaceLocation);
                    LOGGER.info(jobZipPath);
                }
                Webhook.downloadFile(fileUrl, jobZipPath);
                */
                String jobZipPath = job.get("jobZipPath");
                importItems(jobZipPath, new NullProgressMonitor(), true, true, false);
                File jobFile = new File(jobZipPath);
                jobFile.delete();
                // openJob(sequenceur);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Close wait dialog
        webhook.loadingDialogClose();

        return true;
    }
}
