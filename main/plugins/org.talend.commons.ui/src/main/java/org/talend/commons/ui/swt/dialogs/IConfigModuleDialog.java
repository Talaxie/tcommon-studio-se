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
package org.talend.commons.ui.swt.dialogs;

import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.talend.commons.ui.utils.image.ColorUtils;

/**
 * created by wchen on Sep 25, 2017 Detailled comment
 *
 */
public interface IConfigModuleDialog {

    public Color warningColor = ColorUtils.getCacheColor(new RGB(255, 175, 10));

    public String getModuleName();

    public String getMavenURI();

    public int open();
    
    public Map<String, String> getModulesMVNUrls();

}
