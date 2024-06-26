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
package org.talend.core.ui.context.nattableTree;

import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.painter.cell.TextPainter;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.CellStyleProxy;
import org.eclipse.nebula.widgets.nattable.style.IStyle;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

/**
 * created by ldong on Jul 17, 2014 Detailled comment
 *
 */
public class ContextAutoResizeTextPainter extends TextPainter {

    private boolean changeBackgroundColor = false;

    private boolean containsRowName = false;

    public ContextAutoResizeTextPainter(boolean wrapText, boolean paintBg, boolean calculate) {
        super(wrapText, paintBg, calculate);
    }

    @Override
    public void paintCell(ILayerCell cell, GC gc, Rectangle rectangle, IConfigRegistry configRegistry) {
        super.paintCell(cell, gc, rectangle, configRegistry);
    }

    @Override
    public void setupGCFromConfig(GC gc, IStyle cellStyle) {
        super.setupGCFromConfig(gc, cellStyle);
        if (containsRowName) {
            gc.setForeground(GUIHelper.COLOR_RED);
        }
    }

    public void setChangeBackgroundColor(boolean isChange) {
        changeBackgroundColor = isChange;
    }

    public void setContainsRowName(boolean containsRowName) {
        this.containsRowName = containsRowName;
    }

	@Override
	protected void setNewMinLength(ILayerCell cell, int contentWidth) {
		// for bug TDI-31430. Override to avoid that the column width couldn't set to 0.
	}
}
