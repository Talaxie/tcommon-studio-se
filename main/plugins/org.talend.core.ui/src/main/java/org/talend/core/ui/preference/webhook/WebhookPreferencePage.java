// ============================================================================
//
// Copyright (C) 2006-2021 Talaxie Inc. - www.deilink.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talaxie SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.core.ui.preference.collector;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.talend.core.prefs.ITalendCorePrefConstants;
import org.talend.core.service.IRemoteService;
import org.talend.core.ui.CoreUIPlugin;
import org.talend.core.ui.i18n.Messages;
import org.talend.core.ui.token.TokenCollectorFactory;
import org.talend.core.ui.webService.Webhook;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.talend.commons.ui.gmf.util.DisplayUtils;

/**
 * ggu class global comment. Detailled comment
 */
public class WebhookPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private StringFieldEditor frontHostField;
    private StringFieldEditor backHostField;
    private StringFieldEditor bearerField;

    public WebhookPreferencePage() {
        super(GRID);
        setPreferenceStore(CoreUIPlugin.getDefault().getPreferenceStore());
        setDescription("Les webhooks sont un moyen de fournir à d’autres applications des informations en temps réel. Ils permettent d’envoyer une notification à une autre application lorsqu’un événement donné se produit."); //$NON-NLS-1$
    }

    @Override
    public void init(IWorkbench workbench) {

    }

    @Override
    protected void createFieldEditors() {
        Composite fieldEditorParent = getFieldEditorParent();

        addField(new BooleanFieldEditor(ITalendCorePrefConstants.WEBHOOK_ENABLED, "Active", fieldEditorParent));

        frontHostField = new StringFieldEditor(ITalendCorePrefConstants.WEBHOOK_ENABLED, "Front host", fieldEditorParent);
        addField(frontHostField);
        backHostField = new StringFieldEditor(ITalendCorePrefConstants.WEBHOOK_BACK_HOST, "Back host", fieldEditorParent);
        addField(backHostField);
        bearerField = new StringFieldEditor(ITalendCorePrefConstants.WEBHOOK_BEARER, "Bearer", fieldEditorParent);
        addField(bearerField);

        Button importZip = new Button(getFieldEditorParent(), SWT.FLAT);
        importZip.setText("Test"); //$NON-NLS-1$
        importZip.setLayoutData(new GridData());
        importZip.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                Boolean result = Webhook.backTest(backHostField.getStringValue(), bearerField.getStringValue());
                String message = "Test ko !\n"; //$NON-NLS-1$
                if (result) {
                    message = "Test ok !\n"; //$NON-NLS-1$
                }
                MessageDialog messageDialog = new MessageDialog(
                    DisplayUtils.getDefaultShell(false),
                    "Talaxie - Webhook test", //$NON-NLS-1$
                    null,
                    message, //$NON-NLS-1$
                    MessageDialog.CONFIRM,
                    new String[] {
                        IDialogConstants.OK_LABEL,
                        IDialogConstants.CANCEL_LABEL
                    },
                    0
                ); //$NON-NLS-1$
                if (messageDialog.open() == 0) {
                    // TODO
                }

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                // Nothing to do
            }
        });                
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#performOk()
     */
    @Override
    public boolean performOk() {
        final IPreferenceStore preferenceStore = CoreUIPlugin.getDefault().getPreferenceStore();
        boolean valueBeforeOk = preferenceStore.getBoolean(ITalendCorePrefConstants.DATA_COLLECTOR_ENABLED);

        boolean ok = super.performOk();

        if (valueBeforeOk != preferenceStore.getBoolean(ITalendCorePrefConstants.DATA_COLLECTOR_ENABLED)) {
            TokenCollectorFactory.getFactory().send();
        }
        return ok;
    }
}
