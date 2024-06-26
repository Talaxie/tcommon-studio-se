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
package org.talend.utils.xml;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;

import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public class XmlUtils {

    public static Transformer getXmlSecureTransform() throws TransformerConfigurationException {
        Transformer transformer = getXmlSecureTransformerFactory().newTransformer();
        return transformer;
    }

    public static TransformerFactory getXmlSecureTransformerFactory() {
        TransformerFactory transFactory = TransformerFactory.newInstance();
        try {
            transFactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }

        try {
            transFactory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transFactory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        } catch (IllegalArgumentException ex) {
            // Just catch this, as Xalan doesn't support the above
        }
        return transFactory;
    }

    public static DocumentBuilderFactory getSecureDocumentBuilderFactory() {
        return getSecureDocumentBuilderFactory(true);
    }

    public static DocumentBuilderFactory getSecureDocumentBuilderFactory(boolean disAllowDoctypeDecl) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            if (disAllowDoctypeDecl) {
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); //$NON-NLS-1$
            }
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        return factory;
    }

    public static SAXParserFactory getSecureSAXParserFactory() {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXNotRecognizedException e) {
            throw new RuntimeException(e);
        } catch (SAXNotSupportedException e) {
            throw new RuntimeException(e);
        }
        return factory;
    }
}
