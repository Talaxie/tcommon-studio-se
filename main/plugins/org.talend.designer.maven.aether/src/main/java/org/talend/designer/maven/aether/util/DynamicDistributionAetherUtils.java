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
package org.talend.designer.maven.aether.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RemoteRepository.Builder;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.transfer.MetadataNotFoundException;
import org.eclipse.aether.transfer.MetadataTransferException;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.MavenPlugin;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.designer.maven.aether.DummyDynamicMonitor;
import org.talend.designer.maven.aether.IDynamicMonitor;
import org.talend.designer.maven.aether.RepositorySystemFactory;
import org.talend.designer.maven.aether.node.DependencyNode;
import org.talend.designer.maven.aether.node.ExclusionNode;
import org.talend.designer.maven.aether.selector.DynamicDependencySelector;
import org.talend.designer.maven.aether.selector.DynamicExclusionDependencySelector;
import org.talend.utils.io.FilesUtils;
import org.talend.utils.sugars.TypedReturnCode;

/**
 * DOC cmeng class global comment. Detailled comment
 */
public class DynamicDistributionAetherUtils {

    private static final String EXTENSION_POM = "pom";

    private static final String GAV_SEPERATOR = ":";

    private static Map<String, RepositorySystem> repoSystemMap = new HashMap<>();

    private static Map<String, RepositorySystemSession> sessionMap = new HashMap<>();

    public static DependencyNode collectDepencencies(String remoteUrl, String username, String password, String localPath,
            DependencyNode dependencyNode, IDynamicMonitor monitor, boolean multiThread) throws Exception {
        if (monitor == null) {
            monitor = new DummyDynamicMonitor();
        }

        String groupId = dependencyNode.getGroupId();
        String artifactId = dependencyNode.getArtifactId();
        String extension = dependencyNode.getExtension();
        String classifier = dependencyNode.getClassifier();
        String version = dependencyNode.getVersion();
        String scope = dependencyNode.getScope();

        if (scope == null) {
            scope = JavaScopes.COMPILE;
        }
        if (StringUtils.isBlank(extension)) {
            extension = null;
        }
        if (StringUtils.isBlank(classifier)) {
            classifier = null;
        }

        String key = remoteUrl + " | " + localPath; //$NON-NLS-1$

        RepositorySystem repoSystem = null;
        if (multiThread) {
            repoSystem = MavenLibraryResolverProvider.newRepositorySystemForResolver();
        } else {
            repoSystem = repoSystemMap.get(key);
            if (repoSystem == null) {
                repoSystem = MavenLibraryResolverProvider.newRepositorySystemForResolver();
                repoSystemMap.put(key, repoSystem);
            }
        }
        RepositorySystemSession session = null;
        if (multiThread) {
            session = newSession(repoSystem, localPath, monitor);
        } else {
            session = sessionMap.get(key);
            if (session == null) {
                session = newSession(repoSystem, localPath, monitor);
                sessionMap.put(key, session);
            }
        }
        updateDependencySelector((DefaultRepositorySystemSession) session, monitor);

        org.eclipse.aether.graph.Dependency dependency = new org.eclipse.aether.graph.Dependency(
                new DefaultArtifact(groupId, artifactId, classifier, extension, version), scope);

        List<ExclusionNode> exclusionNodes = dependencyNode.getExclusions();
        if (exclusionNodes != null && !exclusionNodes.isEmpty()) {
            Collection<Exclusion> newExclusions = new LinkedHashSet<>();
            Collection<Exclusion> exclusions = dependency.getExclusions();
            newExclusions.addAll(exclusions);
            for (ExclusionNode exclusionNode : exclusionNodes) {
                Exclusion exclusion = buildExclusion(exclusionNode);
                newExclusions.add(exclusion);
            }
            dependency = dependency.setExclusions(newExclusions);
        }

        Builder builder = new RemoteRepository.Builder("central", "default", remoteUrl); //$NON-NLS-1$ //$NON-NLS-2$
        if (StringUtils.isNotBlank(username)) {
            Authentication auth = new AuthenticationBuilder().addUsername(username).addPassword(password).build();
            builder = builder.setAuthentication(auth);
        }
        RemoteRepository central = builder.build();
        central = new RemoteRepository.Builder(central).setProxy(new TalendAetherProxySelector().getProxy(central)).build();

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        collectRequest.addRepository(central);

        checkCancelOrNot(monitor);
        monitor.writeMessage("\n\n=== Start to collect dependecies of " + dependency.toString() + " ===\n");
        org.eclipse.aether.graph.DependencyNode node = repoSystem.collectDependencies(session, collectRequest).getRoot();
        if (node != null) {
            // since we'll remove all parent from pom.xml, so no need to collect parent poms anymore
            // addParentPoms(monitor, repoSystem, session, central, node, null);
            monitor.writeMessage("=== Collected dependencies:\n");
            monitor.writeMessage(buildDependencyTreeString(node, "    "));
            monitor.writeMessage("\n");
        } else {
            monitor.writeMessage("No dependencies collected.");
        }

        DependencyNode convertedNode = convert(node, central);

        return convertedNode;

    }

    private static Set<String> getAllGAVs(org.eclipse.aether.graph.DependencyNode node,
            Set<org.eclipse.aether.graph.DependencyNode> visited) {
        if (visited == null) {
            visited = new HashSet<>();
        }
        if (visited.contains(node)) {
            return Collections.EMPTY_SET;
        } else {
            visited.add(node);
        }
        Set<String> gavs = new HashSet<String>();
        Artifact artifact = node.getArtifact();
        gavs.add(artifact.getGroupId() + GAV_SEPERATOR + artifact.getArtifactId() + GAV_SEPERATOR + artifact.getVersion());
        List<org.eclipse.aether.graph.DependencyNode> children = node.getChildren();
        if (children != null) {
            for (org.eclipse.aether.graph.DependencyNode dn : children) {
                gavs.addAll(getAllGAVs(dn, visited));
            }
        }
        return gavs;
    }

    private static void addParentPoms(IDynamicMonitor monitor, RepositorySystem repoSystem, RepositorySystemSession session,
            RemoteRepository central, org.eclipse.aether.graph.DependencyNode node, Set<String> existingGAVs) throws Exception {
        checkCancelOrNot(monitor);
        if (existingGAVs == null) {
            existingGAVs = getAllGAVs(node, null);
        }
        Artifact artifact = node.getArtifact();
        try {
            Model pomModel = getPomModel(monitor, repoSystem, session, central, artifact.getGroupId(), artifact.getArtifactId(),
                    artifact.getVersion(), artifact.getClassifier());
            Parent parent = pomModel.getParent();

            while (parent != null) {
                checkCancelOrNot(monitor);
                String key = parent.getGroupId() + GAV_SEPERATOR + parent.getArtifactId() + GAV_SEPERATOR + parent.getVersion();
                if (!existingGAVs.contains(key)) {
                    org.eclipse.aether.graph.DependencyNode dn = new DefaultDependencyNode(
                            new DefaultArtifact(parent.getGroupId(), parent.getArtifactId(), EXTENSION_POM, parent.getVersion()));
                    node.getChildren().add(dn);
                    existingGAVs.add(key);
                }
                Model model = getPomModel(monitor, repoSystem, session, central, parent.getGroupId(), parent.getArtifactId(),
                        parent.getVersion(), null);
                if (model != null) {
                    parent = model.getParent();
                } else {
                    parent = null;
                }
            }

        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }

        List<org.eclipse.aether.graph.DependencyNode> children = node.getChildren();
        if (children != null) {
            for (org.eclipse.aether.graph.DependencyNode dn : children) {
                try {
                    addParentPoms(monitor, repoSystem, session, central, dn, existingGAVs);
                } catch (InterruptedException e) {
                    throw e;
                } catch (Exception e) {
                    ExceptionHandler.process(e);
                }
            }
        }
    }

    private static Model getPomModel(IDynamicMonitor monitor, RepositorySystem repoSystem, RepositorySystemSession session,
            RemoteRepository central, String groupId, String artifactId, String version, String classifier) throws Exception {
        checkCancelOrNot(monitor);
        File pomFile = null;
        try {
            ArtifactRequest ar = new ArtifactRequest();
            Artifact reqArtifact = new DefaultArtifact(groupId, artifactId, "", EXTENSION_POM, version);
            ar.setArtifact(reqArtifact);
            ar.addRepository(central);
            ArtifactResult result = repoSystem.resolveArtifact(session, ar);
            pomFile = result.getArtifact().getFile();
        } catch (Exception e) {
            if (StringUtils.isNotBlank(classifier)) {
                ArtifactRequest ar = new ArtifactRequest();
                Artifact reqArtifact = new DefaultArtifact(groupId, artifactId, classifier, EXTENSION_POM, version);
                ar.setArtifact(reqArtifact);
                ar.addRepository(central);
                ArtifactResult result = repoSystem.resolveArtifact(session, ar);
                pomFile = result.getArtifact().getFile();
            } else {
                throw e;
            }
        }
        if (pomFile != null) {
        	Model m = null;
        	try(InputStream is = new FileInputStream(pomFile)){
        		m = MavenPlugin.getMavenModelManager().readMavenModel(is);
        	}
        	return m;
        } else {
            return null;
        }
    }

    public static List<String> versionRange(String remoteUrl, String username, String password, String localPath, String groupId,
            String artifactId, String baseVersion, String topVersion, IDynamicMonitor monitor) throws Exception {
        if (monitor == null) {
            monitor = new DummyDynamicMonitor();
        }
        RepositorySystem repSystem = MavenLibraryResolverProvider.newRepositorySystemForResolver();
        RepositorySystemSession repSysSession = newSession(repSystem, localPath, monitor);
        updateDependencySelector((DefaultRepositorySystemSession) repSysSession, monitor);

        String base = baseVersion;
        if (base == null || base.isEmpty()) {
            base = "0"; //$NON-NLS-1$
        }
        String range = ":[" + base + ","; //$NON-NLS-1$ //$NON-NLS-2$
        if (topVersion != null && !topVersion.isEmpty()) {
            // :[0,1)
            range = range + topVersion + ")"; //$NON-NLS-1$
        } else {
            // :[0,)
            range = range + ")"; //$NON-NLS-1$
        }

        Artifact artifact = new DefaultArtifact(groupId + GAV_SEPERATOR + artifactId + range); 
        Builder builder = new RemoteRepository.Builder("central", "default", remoteUrl); //$NON-NLS-1$ //$NON-NLS-2$
        if (StringUtils.isNotEmpty(username)) {
            Authentication auth = new AuthenticationBuilder().addUsername(username).addPassword(password).build();
            builder = builder.setAuthentication(auth);
        }
        RemoteRepository central = builder.build();
        central = new RemoteRepository.Builder(central).setProxy(new TalendAetherProxySelector().getProxy(central)).build();

        VersionRangeRequest verRangeRequest = new VersionRangeRequest();
        verRangeRequest.addRepository(central);
        verRangeRequest.setArtifact(artifact);

        checkCancelOrNot(monitor);
        VersionRangeResult rangeResult = repSystem.resolveVersionRange(repSysSession, verRangeRequest);
        List<Version> versions = rangeResult.getVersions();
        Set<String> versionSet = new HashSet<>();
        for (Version version : versions) {
            versionSet.add(version.toString());
        }

        return new ArrayList<String>(versionSet);
    }

    public static String getHighestVersion(String remoteUrl, String username, String password, String localPath, String groupId,
            String artifactId, String baseVersion, String topVersion, IDynamicMonitor monitor) throws Exception {
        // maybe need a compatible limit using baseVersion and topVersion
        if (monitor == null) {
            monitor = new DummyDynamicMonitor();
        }
        RepositorySystem repSystem = MavenLibraryResolverProvider.newRepositorySystemForResolver();
        RepositorySystemSession repSysSession = newSession(repSystem, localPath, monitor);
        updateDependencySelector((DefaultRepositorySystemSession) repSysSession, monitor);

        String base = baseVersion;
        if (base == null || base.isEmpty()) {
            base = "0"; //$NON-NLS-1$
        }
        String range = ":[" + base + ","; //$NON-NLS-1$ //$NON-NLS-2$
        if (topVersion != null && !topVersion.isEmpty()) {
            // :[0,1)
            range = range + topVersion + ")"; //$NON-NLS-1$
        } else {
            // :[0,)
            range = range + ")"; //$NON-NLS-1$
        }

        Artifact artifact = new DefaultArtifact(groupId + GAV_SEPERATOR + artifactId + range); 
        Builder builder = new RemoteRepository.Builder("central", "default", remoteUrl); //$NON-NLS-1$ //$NON-NLS-2$
        if (StringUtils.isNotEmpty(username)) {
            Authentication auth = new AuthenticationBuilder().addUsername(username).addPassword(password).build();
            builder = builder.setAuthentication(auth);
        }
        RemoteRepository central = builder.build();
        central = new RemoteRepository.Builder(central).setProxy(new TalendAetherProxySelector().getProxy(central)).build();

        VersionRangeRequest verRangeRequest = new VersionRangeRequest();
        verRangeRequest.addRepository(central);
        verRangeRequest.setArtifact(artifact);

        checkCancelOrNot(monitor);
        VersionRangeResult rangeResult = repSystem.resolveVersionRange(repSysSession, verRangeRequest);
        if (rangeResult == null) {
            return null;
        } else {
            Version highestVersion = rangeResult.getHighestVersion();
            return highestVersion.toString();
        }
    }

    public static TypedReturnCode checkConnection(String remoteUrl, String username, String password, String groupId,
            String artifactId, String baseVersion, String topVersion, IDynamicMonitor monitor) throws Exception {
        TypedReturnCode<VersionRangeResult> tc = new TypedReturnCode<VersionRangeResult>();
        File tempDirectory = null;
        try {
            if (monitor == null) {
                monitor = new DummyDynamicMonitor();
            }
            tempDirectory = Files.createTempDirectory("nexusTemp_").toFile();
            String localPath = tempDirectory.getPath();
            RepositorySystem repSystem = MavenLibraryResolverProvider.newRepositorySystemForResolver();
            RepositorySystemSession repSysSession = newSession(repSystem, localPath, monitor);
            updateDependencySelector((DefaultRepositorySystemSession) repSysSession, monitor);

            String base = baseVersion;
            if (base == null || base.isEmpty()) {
                base = "0"; //$NON-NLS-1$
            }
            String range = ":[" + base + ","; //$NON-NLS-1$ //$NON-NLS-2$
            if (topVersion != null && !topVersion.isEmpty()) {
                // :[0,1)
                range = range + topVersion + ")"; //$NON-NLS-1$
            } else {
                // :[0,)
                range = range + ")"; //$NON-NLS-1$
            }

            Artifact artifact = new DefaultArtifact(groupId + GAV_SEPERATOR + artifactId + range);
            Builder builder = new RemoteRepository.Builder("central", "default", remoteUrl); //$NON-NLS-1$ //$NON-NLS-2$
            if (StringUtils.isNotEmpty(username)) {
                Authentication auth = new AuthenticationBuilder().addUsername(username).addPassword(password).build();
                builder = builder.setAuthentication(auth);
            }
            RemoteRepository central = builder.build();
            central = new RemoteRepository.Builder(central).setProxy(new TalendAetherProxySelector().getProxy(central)).build();

            VersionRangeRequest verRangeRequest = new VersionRangeRequest();
            verRangeRequest.addRepository(central);
            verRangeRequest.setArtifact(artifact);

            checkCancelOrNot(monitor);
            VersionRangeResult rangeResult = repSystem.resolveVersionRange(repSysSession, verRangeRequest);
            if (rangeResult == null) {
                tc.setOk(false);
                return tc;
            } else {
                // have version ragne >> success
                if (rangeResult.getHighestVersion() != null) {
                    tc.setOk(true);
                    tc.setObject(rangeResult);
                    return tc;
                }
                // no version
                if (rangeResult.getExceptions() == null || rangeResult.getExceptions().size() < 1) {
                    tc.setOk(false);
                    return tc;
                }
                // 1. remoteRepository exception is MetadataNotFoundException: connected => failed
                for (Exception e : rangeResult.getExceptions()) {
                    if (e instanceof MetadataNotFoundException
                            && central.equals(((MetadataNotFoundException) e).getRepository())) {
                        tc.setOk(false);
                        tc.setMessage(e.getMessage());
                        ExceptionHandler.process(e);
                        return tc;
                    }
                }
                // 2. remoteRepository exception is MetadataTransferException: connect failed
                for (Exception e : rangeResult.getExceptions()) {
                    if (e instanceof MetadataTransferException
                            && central.equals(((MetadataTransferException) e).getRepository())) {
                        tc.setOk(false);
                        tc.setMessage(e.getCause().getMessage());
                        ExceptionHandler.process(e);
                        return tc;
                    }
                }
                // 3. no remoteRepository exception, throw first exception, log other exception
                tc.setOk(false);
                tc.setMessage(rangeResult.getExceptions().get(0).getMessage());
                for (Exception e : rangeResult.getExceptions()) {
                    ExceptionHandler.process(e);
                }
                return tc;
            }
        } finally {
            if (tempDirectory != null) {
                FilesUtils.deleteFolder(tempDirectory, true);
            }
        }

    }

    private static DependencyNode convert(org.eclipse.aether.graph.DependencyNode node, RemoteRepository remoteRepository)
            throws Exception {
        // List<RemoteRepository> repositories = node.getRepositories();
        // if (repositories != null && !repositories.isEmpty()) {
        // if (!repositories.contains(remoteRepository)) {
        // throw new Exception("Bad remoteRepository: Jar must be able to be downloaded from user configured
        // repository!"); //$NON-NLS-1$
        // }
        // }
        DependencyNode convertedNode = new DependencyNode();

        Artifact artifact = node.getArtifact();
        convertedNode.setArtifactId(artifact.getArtifactId());
        convertedNode.setClassifier(artifact.getClassifier());
        convertedNode.setExtension(artifact.getExtension());
        convertedNode.setGroupId(artifact.getGroupId());
        convertedNode.setVersion(artifact.getVersion());

        List<org.eclipse.aether.graph.DependencyNode> children = node.getChildren();
        List<DependencyNode> convertedChildren = new ArrayList<>();
        convertedNode.setDependencies(convertedChildren);

        if (children != null) {
            for (org.eclipse.aether.graph.DependencyNode child : children) {
                DependencyNode convertedChild = convert(child, remoteRepository);
                convertedChildren.add(convertedChild);
            }
        }

        return convertedNode;
    }

    // private static void getAllArtifact(DependencyNode node, List<Artifact> list) {
    // if (node == null) {
    // return;
    // }
    // list.add(node.getArtifact());
    // List<DependencyNode> children = node.getChildren();
    // for (DependencyNode dn : children) {
    // getAllArtifact(dn, list);
    // }
    // }

    private static RepositorySystemSession newSession(RepositorySystem system, String repositoryPath, IDynamicMonitor monitor)
            throws CoreException {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository localRepo = new LocalRepository(repositoryPath);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        session.setProxySelector(new TalendAetherProxySelector());
        session.setIgnoreArtifactDescriptorRepositories(RepositorySystemFactory.isIgnoreArtifactDescriptorRepositories());

        updateDependencySelector(session, monitor);

        // DependencyManager defaultDependencyManager = session.getDependencyManager();
        // DynamicDependencyManager newDependencyManager = new DynamicDependencyManager();
        // newDependencyManager.setProxy(defaultDependencyManager);
        // session.setDependencyManager(newDependencyManager);

        return session;
    }

    private static void updateDependencySelector(DefaultRepositorySystemSession session, IDynamicMonitor monitor) {
        session.setDependencySelector(getDependencySelector(monitor));
    }

    private static DependencySelector getDependencySelector(IDynamicMonitor monitor) {
        DynamicExclusionDependencySelector exclusionSelector = new DynamicExclusionDependencySelector();
        exclusionSelector.setMonitor(monitor);
        DependencySelector defaultSelector = new AndDependencySelector(
                new DependencySelector[] { new ScopeDependencySelector(new String[] { JavaScopes.TEST, JavaScopes.PROVIDED }),
                        new OptionalDependencySelector(), exclusionSelector });
        DynamicDependencySelector newSelector = new DynamicDependencySelector();
        newSelector.setProxy(defaultSelector);
        newSelector.setMonitor(monitor);
        return newSelector;
    }

    private static Exclusion buildExclusion(ExclusionNode exclusionNode) throws Exception {
        Exclusion exclusion = null;

        String exclusionGroupId = exclusionNode.getGroupId();
        String exclusionArtifactId = exclusionNode.getArtifactId();
        String exclusionClassifier = exclusionNode.getClassifier();
        String exclusionExtension = exclusionNode.getExtension();
        String exclusionVersion = exclusionNode.getVersion();
        if (StringUtils.isNotEmpty(exclusionVersion)) {
            throw new UnsupportedOperationException(
                    "Currently don't support to exclude special version, please support it if needed.");
        }
        exclusion = new Exclusion(exclusionGroupId, exclusionArtifactId, exclusionClassifier, exclusionExtension);

        return exclusion;
    }

    public static String buildDependencyTreeString(org.eclipse.aether.graph.DependencyNode node, String tab) throws Exception {
        String result = ""; //$NON-NLS-1$
        StringWriter strWriter = null;
        PrintWriter printWriter = null;
        try {
            strWriter = new StringWriter();
            printWriter = new PrintWriter(strWriter);
            node.accept(new ConsoleDependencyGraphDumper(printWriter, tab));
            printWriter.flush();
            strWriter.flush();
            StringBuffer buffer = strWriter.getBuffer();
            if (buffer != null) {
                result = buffer.toString();
            }
            return result;
        } finally {
            if (printWriter != null) {
                try {
                    printWriter.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (strWriter != null) {
                try {
                    strWriter.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static List<String> filterVersions(List<String> versions, String versionRange) throws Exception {
        List<String> filteredVersions = new ArrayList<>();
        VersionScheme versionScheme = new GenericVersionScheme();

        VersionConstraint versionConstraint = versionScheme.parseVersionConstraint(versionRange);

        for (String versionString : versions) {
            Version version = versionScheme.parseVersion(versionString);
            if (versionConstraint.containsVersion(version)) {
                filteredVersions.add(versionString);
            }
        }

        return filteredVersions;
    }

    public static void checkCancelOrNot(IDynamicMonitor monitor) throws InterruptedException {
        if (monitor != null) {
            if (monitor.isCanceled() || Thread.interrupted()) {
                throw new InterruptedException("User canceled.");
            }
        }
    }

}
