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
package org.talend.commons.ui.runtime.exception;

import org.apache.log4j.Level;
import org.apache.log4j.Priority;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.talend.commons.CommonsPlugin;
import org.talend.commons.exception.CommonExceptionHandler;
import org.talend.commons.ui.runtime.CommonUIPlugin;
import org.talend.commons.ui.runtime.i18n.Messages;

/**
 * Exception handling via message box.<br/>
 *
 * $Id: MessageBoxExceptionHandler.java 7038 2007-11-15 14:05:48Z plegall $
 *
 */
public final class MessageBoxExceptionHandler {

    private static Throwable lastShowedAction;

    /**
     * Empty constructor.
     */
    private MessageBoxExceptionHandler() {
    }

    /**
     * Log the exeption then open a message box showing a generic message and exception message.
     *
     * @param ex - exception to log
     */
    public static void process(final Throwable ex) {
        if (CommonUIPlugin.isFullyHeadless() || CommonsPlugin.isJUnitTest()) {
            CommonExceptionHandler.process(ex);
            return;
        }
        final Display display = Display.getCurrent() == null ? Display.getDefault() : Display.getCurrent();
        if (display != null) {
            display.syncExec(new Runnable() {

                @Override
                public void run() {
                    process(ex, display.getActiveShell());
                }
            });
        }
    }

    public static void process(Throwable ex, Shell shell) {
        CommonExceptionHandler.process(ex);

        if (CommonsPlugin.isHeadless() || CommonsPlugin.isJUnitTest()) {
            return;
        }

        if (shell != null) {
            showMessage(ex, shell);
        }
    }

    public static void process(Throwable ex, Shell shell, boolean wrapMessage) {
        CommonExceptionHandler.process(ex);

        if (CommonsPlugin.isHeadless() || CommonsPlugin.isJUnitTest()) {
            return;
        }

        if (shell != null) {
            showMessage(ex, shell, wrapMessage);
        }
    }

    public static void showMessage(Throwable ex, Shell shell) {
        showMessage(ex, shell, true);
    }
    /**
     * Open a message box showing a generic message and exception message.
     *
     * @param ex - exception to show
     */
    public static void showMessage(Throwable ex, Shell shell, boolean wrapMessage) {
        if (ex.equals(lastShowedAction)) {
            return;
        }
        lastShowedAction = ex;

        // TODO smallet use ErrorDialogWidthDetailArea ?
        String title = Messages.getString("commons.error"); //$NON-NLS-1$
        String excepMsg = ex.getMessage();
        //add for tup-19726/19790, as for exception detailMessage will show more details on log area.
        if(ex.getCause()!=null) {
            excepMsg = ex.getCause().getMessage();
        }
        String msg = Messages.getString("exception.errorOccured", excepMsg); //$NON-NLS-1$
        if (!wrapMessage) {
            msg = Messages.getString("exception.message", excepMsg); //$NON-NLS-1$
        }
        Priority priority = CommonExceptionHandler.getPriority(ex);

        if (priority == Level.FATAL || priority == Level.ERROR) {
            ExceptionMessageDialog.openError(shell, title, msg, ex);
        } else if (priority == Level.WARN) {
            ExceptionMessageDialog.openWarning(shell, title, msg, ex);
        } else if (priority == Level.INFO) {
            ExceptionMessageDialog.openInformation(shell, title, msg, ex);
        } else {
            ExceptionMessageDialog.openInformation(shell, title, msg, ex);
        }
    }

    /**
     * bug 17654:import the xml file as the schema will throw error.
     *
     * DOC yhch Comment method "showMessageForSchemaImportXml".
     *
     * @param ex
     * @param shell
     */
    public static void showMessageForSchemaImportXml(Throwable ex, Shell shell) {
        String title = Messages.getString("MessageBoxExceptionHandler.showMessageForSchemaImportXml.unParseXML.title"); //$NON-NLS-1$
        String msg = Messages.getString("MessageBoxExceptionHandler.showMessageForSchemaImportXml.unParseXML.msg"); //$NON-NLS-1$
        MessageDialog.openConfirm(shell, title, msg);
    }
}
