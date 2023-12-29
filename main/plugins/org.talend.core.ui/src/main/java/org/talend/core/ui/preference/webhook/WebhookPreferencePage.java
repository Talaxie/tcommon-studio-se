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

/**
 * ggu class global comment. Detailled comment
 */
public class WebhookPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public WebhookPreferencePage() {
        super(GRID);
        setPreferenceStore(CoreUIPlugin.getDefault().getPreferenceStore());
        setDescription("Talaxie, coming soon !"); //$NON-NLS-1$
    }

    @Override
    public void init(IWorkbench workbench) {

    }

    @Override
    protected void createFieldEditors() {
        addField(new BooleanFieldEditor(ITalendCorePrefConstants.WEBHOOK_ENABLED,
                "Active", getFieldEditorParent()));

        addField(new StringFieldEditor(ITalendCorePrefConstants.WEBHOOK_FRONT_HOST,
                "Front host", getFieldEditorParent()));

        addField(new StringFieldEditor(ITalendCorePrefConstants.WEBHOOK_BACK_HOST,
                "Back host", getFieldEditorParent()));

        addField(new StringFieldEditor(ITalendCorePrefConstants.WEBHOOK_BEARER,
                "Bearer", getFieldEditorParent()));
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
