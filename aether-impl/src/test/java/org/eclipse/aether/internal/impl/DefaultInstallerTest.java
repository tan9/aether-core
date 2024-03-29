/*******************************************************************************
 * Copyright (c) 2010, 2013 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.aether.internal.impl;

import static org.eclipse.aether.internal.test.util.RecordingRepositoryListener.Type.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallResult;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.internal.impl.DefaultFileProcessor;
import org.eclipse.aether.internal.impl.DefaultInstaller;
import org.eclipse.aether.internal.test.util.RecordingRepositoryListener;
import org.eclipse.aether.internal.test.util.TestFileProcessor;
import org.eclipse.aether.internal.test.util.TestFileUtils;
import org.eclipse.aether.internal.test.util.TestLocalRepositoryManager;
import org.eclipse.aether.internal.test.util.TestUtils;
import org.eclipse.aether.internal.test.util.RecordingRepositoryListener.EventWrapper;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.metadata.Metadata.Nature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DefaultInstallerTest
{

    private Artifact artifact;

    private DefaultMetadata metadata;

    private DefaultRepositorySystemSession session;

    private String localArtifactPath;

    private String localMetadataPath;

    private DefaultInstaller installer;

    private InstallRequest request;

    private RecordingRepositoryListener listener;

    private File localArtifactFile;

    private TestLocalRepositoryManager lrm;

    @Before
    public void setup()
        throws IOException
    {
        artifact = new DefaultArtifact( "gid", "aid", "jar", "ver" );
        artifact = artifact.setFile( TestFileUtils.createTempFile( "artifact".getBytes(), 1 ) );
        metadata =
            new DefaultMetadata( "gid", "aid", "ver", "type", Nature.RELEASE_OR_SNAPSHOT,
                                 TestFileUtils.createTempFile( "metadata".getBytes(), 1 ) );

        session = TestUtils.newSession();
        localArtifactPath = session.getLocalRepositoryManager().getPathForLocalArtifact( artifact );
        localMetadataPath = session.getLocalRepositoryManager().getPathForLocalMetadata( metadata );

        localArtifactFile = new File( session.getLocalRepository().getBasedir(), localArtifactPath );

        installer = new DefaultInstaller();
        installer.setFileProcessor( new TestFileProcessor() );
        installer.setRepositoryEventDispatcher( new StubRepositoryEventDispatcher() );
        installer.setSyncContextFactory( new StubSyncContextFactory() );
        request = new InstallRequest();
        listener = new RecordingRepositoryListener();
        session.setRepositoryListener( listener );

        lrm = (TestLocalRepositoryManager) session.getLocalRepositoryManager();

        TestFileUtils.delete( session.getLocalRepository().getBasedir() );
    }

    @After
    public void teardown()
        throws Exception
    {
        TestFileUtils.delete( session.getLocalRepository().getBasedir() );
    }

    @Test
    public void testSuccessfulInstall()
        throws InstallationException, UnsupportedEncodingException, IOException
    {
        File artifactFile =
            new File( session.getLocalRepositoryManager().getRepository().getBasedir(), localArtifactPath );
        File metadataFile =
            new File( session.getLocalRepositoryManager().getRepository().getBasedir(), localMetadataPath );

        artifactFile.delete();
        metadataFile.delete();

        request.addArtifact( artifact );
        request.addMetadata( metadata );

        InstallResult result = installer.install( session, request );

        assertTrue( artifactFile.exists() );
        TestFileUtils.assertContent( "artifact".getBytes( "UTF-8" ), artifactFile );

        assertTrue( metadataFile.exists() );
        TestFileUtils.assertContent( "metadata".getBytes( "UTF-8" ), metadataFile );

        assertEquals( result.getRequest(), request );

        assertEquals( result.getArtifacts().size(), 1 );
        assertTrue( result.getArtifacts().contains( artifact ) );

        assertEquals( result.getMetadata().size(), 1 );
        assertTrue( result.getMetadata().contains( metadata ) );

        assertEquals( 1, lrm.getMetadataRegistration().size() );
        assertTrue( lrm.getMetadataRegistration().contains( metadata ) );
        assertEquals( 1, lrm.getArtifactRegistration().size() );
        assertTrue( lrm.getArtifactRegistration().contains( artifact ) );
    }

    @Test( expected = InstallationException.class )
    public void testNullArtifactFile()
        throws InstallationException
    {
        InstallRequest request = new InstallRequest();
        request.addArtifact( artifact.setFile( null ) );

        installer.install( session, request );
    }

    @Test( expected = InstallationException.class )
    public void testNullMetadataFile()
        throws InstallationException
    {
        InstallRequest request = new InstallRequest();
        request.addMetadata( metadata.setFile( null ) );

        installer.install( session, request );
    }

    @Test( expected = InstallationException.class )
    public void testNonExistentArtifactFile()
        throws InstallationException
    {
        InstallRequest request = new InstallRequest();
        request.addArtifact( artifact.setFile( new File( "missing.txt" ) ) );

        installer.install( session, request );
    }

    @Test( expected = InstallationException.class )
    public void testNonExistentMetadataFile()
        throws InstallationException
    {
        InstallRequest request = new InstallRequest();
        request.addMetadata( metadata.setFile( new File( "missing.xml" ) ) );

        installer.install( session, request );
    }

    @Test( expected = InstallationException.class )
    public void testArtifactExistsAsDir()
        throws InstallationException
    {
        String path = session.getLocalRepositoryManager().getPathForLocalArtifact( artifact );
        File file = new File( session.getLocalRepository().getBasedir(), path );
        assertFalse( file.getAbsolutePath() + " is a file, not directory", file.isFile() );
        assertFalse( file.getAbsolutePath() + " already exists", file.exists() );
        assertTrue( "failed to setup test: could not create " + file.getAbsolutePath(),
                    file.mkdirs() || file.isDirectory() );

        request.addArtifact( artifact );
        installer.install( session, request );
    }

    @Test( expected = InstallationException.class )
    public void testMetadataExistsAsDir()
        throws InstallationException
    {
        String path = session.getLocalRepositoryManager().getPathForLocalMetadata( metadata );
        assertTrue( "failed to setup test: could not create " + path,
                    new File( session.getLocalRepository().getBasedir(), path ).mkdirs() );

        request.addMetadata( metadata );
        installer.install( session, request );
    }

    @Test
    public void testSuccessfulArtifactEvents()
        throws InstallationException
    {
        InstallRequest request = new InstallRequest();
        request.addArtifact( artifact );

        installer.install( session, request );
        checkEvents( "Repository Event problem", artifact, false );
    }

    @Test
    public void testSuccessfulMetadataEvents()
        throws InstallationException
    {
        InstallRequest request = new InstallRequest();
        request.addMetadata( metadata );

        installer.install( session, request );
        checkEvents( "Repository Event problem", metadata, false );
    }

    @Test
    public void testFailingEventsNullArtifactFile()
    {
        checkFailedEvents( "null artifact file", this.artifact.setFile( null ) );
    }

    @Test
    public void testFailingEventsNullMetadataFile()
    {
        checkFailedEvents( "null metadata file", this.metadata.setFile( null ) );
    }

    @Test
    public void testFailingEventsArtifactExistsAsDir()
    {
        String path = session.getLocalRepositoryManager().getPathForLocalArtifact( artifact );
        assertTrue( "failed to setup test: could not create " + path,
                    new File( session.getLocalRepository().getBasedir(), path ).mkdirs() );
        checkFailedEvents( "target exists as dir", artifact );
    }

    @Test
    public void testFailingEventsMetadataExistsAsDir()
    {
        String path = session.getLocalRepositoryManager().getPathForLocalMetadata( metadata );
        assertTrue( "failed to setup test: could not create " + path,
                    new File( session.getLocalRepository().getBasedir(), path ).mkdirs() );
        checkFailedEvents( "target exists as dir", metadata );
    }

    private void checkFailedEvents( String msg, Metadata metadata )
    {
        InstallRequest request = new InstallRequest().addMetadata( metadata );
        msg = "Repository events problem (case: " + msg + ")";

        try
        {
            installer.install( session, request );
            fail( "expected exception" );
        }
        catch ( InstallationException e )
        {
            checkEvents( msg, metadata, true );
        }

    }

    private void checkEvents( String msg, Metadata metadata, boolean failed )
    {
        List<EventWrapper> events = listener.getEvents();
        assertEquals( msg, 2, events.size() );
        EventWrapper wrapper = events.get( 0 );
        assertEquals( msg, METADATA_INSTALLING, wrapper.getType() );

        RepositoryEvent event = wrapper.getEvent();
        assertEquals( msg, metadata, event.getMetadata() );
        assertNull( msg, event.getException() );

        wrapper = events.get( 1 );
        assertEquals( msg, METADATA_INSTALLED, wrapper.getType() );
        event = wrapper.getEvent();
        assertEquals( msg, metadata, event.getMetadata() );
        if ( failed )
        {
            assertNotNull( msg, event.getException() );
        }
        else
        {
            assertNull( msg, event.getException() );
        }
    }

    private void checkFailedEvents( String msg, Artifact artifact )
    {
        InstallRequest request = new InstallRequest().addArtifact( artifact );
        msg = "Repository events problem (case: " + msg + ")";

        try
        {
            installer.install( session, request );
            fail( "expected exception" );
        }
        catch ( InstallationException e )
        {
            checkEvents( msg, artifact, true );
        }
    }

    private void checkEvents( String msg, Artifact artifact, boolean failed )
    {
        List<EventWrapper> events = listener.getEvents();
        assertEquals( msg, 2, events.size() );
        EventWrapper wrapper = events.get( 0 );
        assertEquals( msg, ARTIFACT_INSTALLING, wrapper.getType() );
        
        RepositoryEvent event = wrapper.getEvent();
        assertEquals( msg, artifact, event.getArtifact() );
        assertNull( msg, event.getException() );
        
        wrapper = events.get( 1 );
        assertEquals( msg, ARTIFACT_INSTALLED, wrapper.getType() );
        event = wrapper.getEvent();
        assertEquals( msg, artifact, event.getArtifact() );
        if ( failed )
        {
            assertNotNull( msg + " > expected exception", event.getException() );
        }
        else
        {
            assertNull( msg + " > " + event.getException(), event.getException() );
        }
    }

    @Test
    public void testDoNotUpdateUnchangedArtifact()
        throws InstallationException
    {
        request.addArtifact( artifact );
        installer.install( session, request );

        installer.setFileProcessor( new DefaultFileProcessor()
        {
            @Override
            public long copy( File src, File target, ProgressListener listener )
                throws IOException
            {
                throw new IOException( "copy called" );
            }
        } );

        request = new InstallRequest();
        request.addArtifact( artifact );
        installer.install( session, request );
    }

    @Test
    public void testSetArtifactTimestamps()
        throws InstallationException
    {
        artifact.getFile().setLastModified( artifact.getFile().lastModified() - 60000 );

        request.addArtifact( artifact );

        installer.install( session, request );

        assertEquals( "artifact timestamp was not set to src file", artifact.getFile().lastModified(),
                      localArtifactFile.lastModified() );

        request = new InstallRequest();

        request.addArtifact( artifact );

        artifact.getFile().setLastModified( artifact.getFile().lastModified() - 60000 );

        installer.install( session, request );

        assertEquals( "artifact timestamp was not set to src file", artifact.getFile().lastModified(),
                      localArtifactFile.lastModified() );
    }
}
