// ============================================================================
//
// Copyright (C) 2006-2023 Talaxie Inc. - www.deilink.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talaxie SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.rcp.intro;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.AbstractSourceProvider;
import org.talend.core.PluginChecker;
import org.talend.core.ui.branding.IBrandingService;

public class HelpMenuProvider extends AbstractSourceProvider {

    private static final String IS_TIS = "HelpMenuProvider.isTis";//$NON-NLS-1$

    @Override
    public Map getCurrentState() {
        Map<String, Boolean> stateMap = new HashMap<String, Boolean>();
        stateMap.put(IS_TIS, testIfShouldBeShown());
        return stateMap;
    }

    private boolean testIfShouldBeShown() {
        boolean isTis = PluginChecker.isTIS();
        boolean isPoweredbyTalend = IBrandingService.get().isPoweredbyTalend();
        return isTis && isPoweredbyTalend;
    }

    @Override
    public void dispose() {
    }

    @Override
    public String[] getProvidedSourceNames() {
        return new String[] { IS_TIS };
    }
}
