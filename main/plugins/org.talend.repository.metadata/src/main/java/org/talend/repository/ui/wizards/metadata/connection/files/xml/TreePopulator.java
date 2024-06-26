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
package org.talend.repository.ui.wizards.metadata.connection.files.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.commons.io.IOUtils;
import org.apache.xerces.xs.XSModel;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.xsd.XSDSchema;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.runtime.exception.ExceptionHandler;
import org.talend.datatools.xml.utils.ATreeNode;
import org.talend.datatools.xml.utils.OdaException;
import org.talend.datatools.xml.utils.SchemaPopulationUtil;
import org.talend.datatools.xml.utils.UtilConstants;
import org.talend.datatools.xml.utils.XSDPopulationUtil2;
import org.talend.metadata.managment.ui.wizard.metadata.xml.utils.CopyDeleteFileUtilForWizard;
import org.talend.repository.ui.wizards.metadata.connection.files.json.AbstractTreePopulator;

/**
 * DOC amaumont class global comment. Detailled comment <br/>
 *
 * $Id$
 *
 */
public class TreePopulator extends AbstractTreePopulator {

    private final BidiMap xPathToTreeItem = new DualHashBidiMap();

    /**
     * Use the constructor with TreeViewer instead.
     *
     * @param tree
     * @deprecated
     */
    @Deprecated
    public TreePopulator(Tree tree) {
        super();
        // no virtual mode on this case.
        treeViewer = new TreeViewer(tree);
        treeViewer.setContentProvider(new ATreeNodeContentProvider());
        treeViewer.setLabelProvider(new VirtualXmlTreeLabelProvider());
        treeViewer.setUseHashlookup(true);
    }

    /**
     * DOC amaumont TreePopulator constructor comment.
     *
     * @param availableXmlTree
     */
    public TreePopulator(TreeViewer availableXmlTree) {
        super();
        this.treeViewer = availableXmlTree;
    }

    /**
     * populate xml tree.
     *
     */
    @Override
    public boolean populateTree(String filePath, ATreeNode treeNode) {
        return populateTree(filePath, treeNode, null);
    }

    public boolean isValidFile(String filePath) {
        File file = new File(filePath);
        SAXReader saxReader = new SAXReader();
        URL url;
        try {
            url = file.toURI().toURL();
            Document doc = saxReader.read(url.getFile());
        } catch (MalformedURLException e) {
            ExceptionHandler.process(e);
        } catch (DocumentException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean populateTree(String filePath, ATreeNode treeNode, String selectedEntity) {
        if (!isValidFile(filePath)) {
            Display.getDefault().syncExec(new Runnable() {

                @Override
                public void run() {
                    // MessageDialog.openError(Display.getDefault().getActiveShell(), "",
                    // Messages.getString("dataset.error.populateXMLTree"));
                }

            });
            return false;
        }
        xPathToTreeItem.clear();
        if (filePath != null && !filePath.equals("")) { //$NON-NLS-1$
            String newFilePath;
            try {
                newFilePath = CopyDeleteFileUtilForWizard.copyToTemp(filePath);
            } catch (PersistenceException e1) {
                newFilePath = filePath;
            }
            List<String> attList = CopyDeleteFileUtilForWizard.getComplexNodes(filePath);
            try {
                if (attList != null && !attList.isEmpty()) {
                    treeNode = SchemaPopulationUtil.getSchemaTree(newFilePath, true, false, limit, attList);
                } else {
                    treeNode = SchemaPopulationUtil.getSchemaTree(newFilePath, true, limit);
                }
            } catch (MalformedURLException e) {
                ExceptionHandler.process(e);
            } catch (OdaException e) {
                ExceptionHandler.process(e);
            } catch (URISyntaxException e) {
                ExceptionHandler.process(e);
            } catch (Exception e) {
                return false;
            }
            if (treeNode == null || treeNode.getChildren().length == 0) {
                // OdaException ex = new OdaException(Messages.getString("dataset.error.populateXMLTree"));
                // //$NON-NLS-1$
                // ExceptionHandler.process(ex);
                return false;
            } else {
                boolean isXml = newFilePath.toUpperCase().endsWith(".XML");
                Object[] childs = treeNode.getChildren();
                if (selectedEntity != null) {
                    Object selected = null;
                    for (Object child : childs) {
                        if (child instanceof ATreeNode) {
                            if (selectedEntity.equals(((ATreeNode) child).getValue())) {
                                selected = child;
                                break;
                            }
                        }
                    }
                    if (selected == null && childs.length > 0) {
                        selected = childs[0];
                    }
                    treeViewer.setInput(new Object[] { selected });
                    treeViewer.expandToLevel(3);
                } else {
                    String originalJsonFilePath = getOriginfilePath();
                    if(null != originalJsonFilePath && !"".equals(originalJsonFilePath.trim())) {
                        File file = new File(originalJsonFilePath);
                        if (file.exists()) {
                            if (!file.isDirectory()) {
                                try(InputStream input = new FileInputStream(file);) {
                                    String jsonStr = IOUtils.toString(input, getEncoding());
                                    if(barceType(jsonStr) == 0 && childs.length == 1) {
                                        if(isNeedAddRoot(jsonStr)) {
                                            childs = ((ATreeNode)childs[0]).getChildren();
                                        }
                                    } else if(barceType(jsonStr) == 1 && childs.length == 1) {
                                        ATreeNode objectNode = (ATreeNode) ((ATreeNode)childs[0]).getChildren()[0];
                                        childs = objectNode.getChildren();
                                    }
                                } catch (IOException e) {
                                    ExceptionHandler.process(e);
                                }
                            }
                        }
                    }
                    
                	treeViewer.setInput(childs);
                	treeViewer.expandToLevel(3);
                }
                this.filePath = filePath;
                return true;
            }
        }
        return false;
    }
    
    private int barceType(String originalJsonString) {
        for (int c = 0; c < originalJsonString.length(); ++c) {
            if (originalJsonString.charAt(c) == '{') {
                return 0; //brace
            } else if (originalJsonString.charAt(c) == '[') {
                return 1; //bracket
            }
        }
        
        return -1;
    }
    private boolean isNeedAddRoot(String originalJsonString) {
        boolean isNeedAddRoot = false;
        net.sf.json.JSONObject jso = net.sf.json.JSONObject.fromObject(originalJsonString);
        String jsonKey = "";
        Object firstObject = null;
        if (jso.names().size() == 1) {
            jsonKey = jso.names().get(0).toString();
            firstObject = jso.get(jsonKey);
        }
        if (jso.size() > 1
                || (firstObject != null && firstObject instanceof net.sf.json.JSONArray && ((net.sf.json.JSONArray) firstObject)
                        .size() > 1)) {
            isNeedAddRoot = true;
        }
        return isNeedAddRoot;
    }
    
    public boolean populateTree(XSModel xsModel, ATreeNode selectedNode, List<ATreeNode> treeNodes) {
        xPathToTreeItem.clear();
        ATreeNode treeNode = null;
        if (xsModel != null) {
            try {
                treeNode = SchemaPopulationUtil.getSchemaTree(xsModel, selectedNode, true);
                if (treeNodes != null && treeNode.getChildren().length > 0) {
                    for (Object obj : treeNode.getChildren()) {
                        if (obj instanceof ATreeNode) {
                            treeNodes.add((ATreeNode) obj);
                        }
                    }
                }

            } catch (MalformedURLException e) {
                ExceptionHandler.process(e);
            } catch (OdaException e) {
                ExceptionHandler.process(e);
            } catch (URISyntaxException e) {
                ExceptionHandler.process(e);
            } catch (Exception e) {
                return false;
            }
            if (treeNode == null || treeNode.getChildren().length == 0) {
                return false;
            } else {
                Object[] childs = treeNode.getChildren();
                treeViewer.setInput(childs);
                treeViewer.expandToLevel(3);
                return true;
            }
        }
        return false;
    }

    public boolean populateTree(XSDSchema schema, ATreeNode selectedNode, List<ATreeNode> treeNodes) {
        return populateTree(null, schema, selectedNode, treeNodes);
    }

    public boolean populateTree(XSDPopulationUtil2 popUtil, XSDSchema schema, ATreeNode selectedNode, List<ATreeNode> treeNodes) {
        xPathToTreeItem.clear();
        ATreeNode treeNode = null;
        if (schema != null) {
            try {
                if (popUtil == null) {
                    treeNode = SchemaPopulationUtil.getSchemaTree(schema, selectedNode);
                } else {
                    treeNode = SchemaPopulationUtil.getSchemaTree(popUtil, schema, selectedNode);
                }
                if (treeNodes != null) {
                    treeNodes.add(treeNode);
                }
            } catch (MalformedURLException e) {
                ExceptionHandler.process(e);
            } catch (OdaException e) {
                ExceptionHandler.process(e);
            } catch (URISyntaxException e) {
                ExceptionHandler.process(e);
            } catch (Exception e) {
                ExceptionHandler.process(e);
                return false;
            }
            if (treeNode == null) {
                return false;
            } else {
            	treeViewer.setInput(new Object[] { treeNode });
            	treeViewer.expandToLevel(3);
                return true;
            }
        }
        return false;
    }

    @Override
    public TreeItem getTreeItem(String absoluteXPath) {
        TreeItem[] items = treeViewer.getTree().getItems();
        String path = absoluteXPath;
        String targetName = absoluteXPath;
        if (targetName.contains(UtilConstants.XPATH_SLASH)) {
            String[] names = targetName.split(UtilConstants.XPATH_SLASH);
            if (names != null && 0 < names.length) {
                targetName = names[names.length - 1];
            }
        }
        TreeItem item = null;
        while (!path.isEmpty()) {
            boolean flag = false;
            for (TreeItem curItem : items) {
                if (targetName != null && curItem.getText().equals(targetName)) {
                    if (path.startsWith("/" + curItem.getText())) { //$NON-NLS-1$
                        flag = true;
                        item = curItem;
                        path = path.replaceFirst("/" + curItem.getText(), ""); //$NON-NLS-1$//$NON-NLS-2$
                        break;
                    }
                } else {
                    String tempRootPath = curItem.getText() + UtilConstants.XPATH_SLASH;
                    if (path.startsWith("/" + tempRootPath)) { //$NON-NLS-1$
                        flag = true;
                        item = curItem;
                        path = path.replaceFirst("/" + curItem.getText(), ""); //$NON-NLS-1$//$NON-NLS-2$
                        break;
                    }
                }

            }
            if (!path.isEmpty() && item != null) {
                items = item.getItems();
            }
            if (!flag) {
                break;
            }
        }
        return item;
    }

    @Override
    public String getAbsoluteXPath(TreeItem treeItem) {
        TreeItem item = treeItem;
        String path = ""; //$NON-NLS-1$
        while (item != null) {
            path = "/" + item.getText() + path; //$NON-NLS-1$
            item = item.getParentItem();
        }
        return path;
    }

    public static int getLimit() {
        return AbstractTreePopulator.getLimit();
    }

    public static int getMaximumRowsToPreview() {
        return AbstractTreePopulator.getMaximumRowsToPreview();
    }

    @Override
    public void configureDefaultTreeViewer() {
        if (treeViewer.getContentProvider() != null) {
        	treeViewer.setInput(null);
        }
        treeViewer.setContentProvider(new ATreeNodeContentProvider());
        treeViewer.setLabelProvider(new VirtualXmlTreeLabelProvider());
    }
   
}
