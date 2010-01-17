/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.maven.mercury.repository.metadata;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.maven.mercury.util.FileUtil;
import org.apache.maven.mercury.util.TimeUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class MetadataBuilderTest
    extends TestCase
{
    MetadataBuilder mb;

    File testBase = new File( "./target/test-classes/controlledRepoMd" );

    // -------------------------------------------------------------------------
    @Override
    protected void setUp()
        throws Exception
    {
        File temp = new File( testBase, "group-maven-metadata-write.xml" );
        if ( temp.exists() )
            temp.delete();
    }

    // -------------------------------------------------------------------------
    protected void tearDown()
        throws Exception
    {
    }

    protected Metadata getMetadata( File file )
        throws Exception
    {
        byte[] targetBytes = FileUtil.readRawData( file );

        return MetadataBuilder.getMetadata( targetBytes );
    }

    protected Metadata applyOpsAndGetMetadata( Metadata metadata, List<MetadataOperation> ops, File file )
        throws Exception
    {
        byte[] resBytes = MetadataBuilder.changeMetadata( metadata, ops );

        FileUtil.writeRawData( file, resBytes );

        return MetadataBuilder.read( new FileInputStream( file ) );
    }

    // -------------------------------------------------------------------------
    public void testReadGroupMd()
        throws FileNotFoundException, IOException, XmlPullParserException, MetadataException
    {
        File groupMd = new File( testBase, "group-maven-metadata.xml" );
        Metadata mmd = MetadataBuilder.read( new FileInputStream( groupMd ) );

        assertNotNull( mmd );
        assertEquals( "a", mmd.getGroupId() );
        assertEquals( "a", mmd.getArtifactId() );

        assertNotNull( mmd.getVersioning() );

        List<String> versions = mmd.getVersioning().getVersions();

        assertNotNull( versions );
        assertEquals( 4, versions.size() );
    }

    // -------------------------------------------------------------------------
    public void testWriteGroupMd()
        throws FileNotFoundException, IOException, XmlPullParserException, MetadataException
    {
        File groupMd = new File( testBase, "group-maven-metadata-write.xml" );
        Metadata md = new Metadata();
        md.setGroupId( "a" );
        md.setArtifactId( "a" );
        md.setVersion( "1.0.0" );
        Versioning v = new Versioning();
        v.addVersion( "1.0.0" );
        v.addVersion( "2.0.0" );
        md.setVersioning( v );

        MetadataBuilder.write( md, new FileOutputStream( groupMd ) );
        Metadata mmd = MetadataBuilder.read( new FileInputStream( groupMd ) );

        assertNotNull( mmd );
        assertEquals( "a", mmd.getGroupId() );
        assertEquals( "a", mmd.getArtifactId() );
        assertEquals( "1.0.0", mmd.getVersion() );

        assertNotNull( mmd.getVersioning() );

        List<String> versions = mmd.getVersioning().getVersions();

        assertNotNull( versions );
        assertEquals( 2, versions.size() );
    }

    // -------------------------------------------------------------------------
    public void testAddPluginOperation()
        throws Exception
    {
        File mdFileBefore = new File( testBase, "group-maven-metadata.xml" );
        Metadata mdBefore = getMetadata( mdFileBefore );

        Plugin pluginA = new Plugin();
        pluginA.setArtifactId( "maven-testa-plugin" );
        pluginA.setName( "Some Plugin A" );
        pluginA.setPrefix( "testa" );
        Plugin pluginB = new Plugin();
        pluginB.setArtifactId( "maven-testb-plugin" );
        pluginB.setName( "Some Plugin B" );
        pluginB.setPrefix( "testb" );
        Plugin pluginC = new Plugin();
        pluginC.setArtifactId( "maven-testc-plugin" );
        pluginC.setName( "Some Plugin C" );
        pluginC.setPrefix( "testc" );

        List<MetadataOperation> opsAdd = new ArrayList<MetadataOperation>();

        opsAdd.add( new AddPluginOperation( new PluginOperand( pluginC ) ) );
        opsAdd.add( new AddPluginOperation( new PluginOperand( pluginB ) ) );
        opsAdd.add( new AddPluginOperation( new PluginOperand( pluginA ) ) );

        File mdFileAfter = new File( testBase, "group-maven-metadata-write.xml" );
        Metadata mdAfterAdd = applyOpsAndGetMetadata( mdBefore, opsAdd, mdFileAfter );

        assertEquals( 3, mdAfterAdd.getPlugins().size() );
        assertEquals( "maven-testa-plugin", ( (Plugin) mdAfterAdd.getPlugins().get( 0 ) ).getArtifactId() );
        assertEquals( "testa", ( (Plugin) mdAfterAdd.getPlugins().get( 0 ) ).getPrefix() );
        assertEquals( "Some Plugin A", ( (Plugin) mdAfterAdd.getPlugins().get( 0 ) ).getName() );
        assertEquals( "maven-testb-plugin", ( (Plugin) mdAfterAdd.getPlugins().get( 1 ) ).getArtifactId() );
        assertEquals( "maven-testc-plugin", ( (Plugin) mdAfterAdd.getPlugins().get( 2 ) ).getArtifactId() );

        List<MetadataOperation> opsRemove = new ArrayList<MetadataOperation>();

        opsRemove.add( new RemovePluginOperation( new PluginOperand( pluginA ) ) );
        opsRemove.add( new RemovePluginOperation( new PluginOperand( pluginB ) ) );
        opsRemove.add( new RemovePluginOperation( new PluginOperand( pluginC ) ) );

        Metadata mdAfterRemove = applyOpsAndGetMetadata( mdAfterAdd, opsRemove, mdFileAfter );

        assertEquals( 0, mdAfterRemove.getPlugins().size() );
    }

    // -------------------------------------------------------------------------
    public void testMergeOperation()
        throws FileNotFoundException, IOException, XmlPullParserException, MetadataException
    {
        File groupMd = new File( testBase, "group-maven-metadata.xml" );
        byte[] targetBytes = FileUtil.readRawData( groupMd );

        Metadata source = new Metadata();
        source.setGroupId( "a" );
        source.setArtifactId( "a" );
        source.setVersion( "1.0.0" );
        Versioning v = new Versioning();
        v.addVersion( "1.0.0" );
        v.addVersion( "2.0.0" );
        source.setVersioning( v );

        byte[] resBytes =
            MetadataBuilder.changeMetadata( targetBytes, new MergeOperation( new MetadataOperand( source ) ) );

        File resFile = new File( testBase, "group-maven-metadata-write.xml" );

        FileUtil.writeRawData( resFile, resBytes );

        Metadata mmd = MetadataBuilder.read( new FileInputStream( resFile ) );

        assertNotNull( mmd );
        assertEquals( "a", mmd.getGroupId() );
        assertEquals( "a", mmd.getArtifactId() );
        assertEquals( "4", mmd.getVersion() );

        assertNotNull( mmd.getVersioning() );

        List<String> versions = mmd.getVersioning().getVersions();

        assertNotNull( versions );
        assertEquals( 6, versions.size() );
        assertTrue( versions.contains( "1" ) );
        assertTrue( versions.contains( "2" ) );
        assertTrue( versions.contains( "3" ) );
        assertTrue( versions.contains( "4" ) );
        assertTrue( versions.contains( "1.0.0" ) );
        assertTrue( versions.contains( "2.0.0" ) );
    }

    public void testAddVersionOperationOrdered()
        throws Exception
    {
        File mdFileBefore = new File( testBase, "group-maven-metadata.xml" );
        Metadata mdBefore = getMetadata( mdFileBefore );

        List<MetadataOperation> ops = new ArrayList<MetadataOperation>();
        ops.add( new AddVersionOperation( new StringOperand( "1.3.0-SNAPSHOT" ) ) );
        ops.add( new AddVersionOperation( new StringOperand( "1.2.0-SNAPSHOT" ) ) );
        ops.add( new AddVersionOperation( new StringOperand( "1.2.0.5-SNAPSHOT" ) ) );
        ops.add( new AddVersionOperation( new StringOperand( "1.0.1" ) ) );
        ops.add( new AddVersionOperation( new StringOperand( "1.0.3-SNAPSHOT" ) ) );
        ops.add( new AddVersionOperation( new StringOperand( "1.1-M1" ) ) );
        ops.add( new AddVersionOperation( new StringOperand( "1.0.0-alpha-5" ) ) );
        ops.add( new AddVersionOperation( new StringOperand( "1.2.0" ) ) );
        ops.add( new AddVersionOperation( new StringOperand( "1.2.0-beta-1" ) ) );
        ops.add( new AddVersionOperation( new StringOperand( "1.0.0.1" ) ) );
        ops.add( new AddVersionOperation( new StringOperand( "1.0.0-beta-3" ) ) );
        ops.add( new AddVersionOperation( new StringOperand( "4.1.0" ) ) );
        ops.add( new AddVersionOperation( new StringOperand( "1.0.0-beta-6-SNAPSHOT" ) ) );
        ops.add( new AddVersionOperation( new StringOperand( "5.0-SNAPSHOT" ) ) );

        List<String> orderedVersions = new ArrayList<String>();
        orderedVersions.add( "1.0.0-alpha-5" );
        orderedVersions.add( "1.0.0-beta-3" );
        orderedVersions.add( "1.0.0-beta-6-SNAPSHOT" );
        orderedVersions.add( "1" );
        orderedVersions.add( "1.0.0.1" );
        orderedVersions.add( "1.0.1" );
        orderedVersions.add( "1.0.3-SNAPSHOT" );
        orderedVersions.add( "1.1-M1" );
        orderedVersions.add( "1.2.0-beta-1" );
        orderedVersions.add( "1.2.0-SNAPSHOT" );
        orderedVersions.add( "1.2.0" );
        orderedVersions.add( "1.2.0.5-SNAPSHOT" );
        orderedVersions.add( "1.3.0-SNAPSHOT" );
        orderedVersions.add( "2" );
        orderedVersions.add( "3" );
        orderedVersions.add( "4" );
        orderedVersions.add( "4.1.0" );
        orderedVersions.add( "5.0-SNAPSHOT" );

        File mdFileAfter = new File( testBase, "group-maven-metadata-write.xml" );
        Metadata mdAfter = applyOpsAndGetMetadata( mdBefore, ops, mdFileAfter );

        Assert.assertEquals( orderedVersions, mdAfter.getVersioning().getVersions() );
        Assert.assertEquals( "5.0-SNAPSHOT", mdAfter.getVersioning().getLatest() );
        Assert.assertEquals( "4.1.0", mdAfter.getVersioning().getRelease() );

    }

    // -------------------------------------------------------------------------
    public void testAddVersionOperation()
        throws Exception
    {
        // prepare
        File groupMd = new File( testBase, "group-maven-metadata.xml" );

        Metadata md = getMetadata( groupMd );

        List<MetadataOperation> ops = new ArrayList<MetadataOperation>();

        ops.add( new AddVersionOperation( new StringOperand( "5" ) ) );

        File resFile = new File( testBase, "group-maven-metadata-write.xml" );

        // do
        Metadata mmd = applyOpsAndGetMetadata( md, ops, resFile );

        // assert
        assertNotNull( mmd );
        assertEquals( "a", mmd.getGroupId() );
        assertEquals( "a", mmd.getArtifactId() );
        assertEquals( "4", mmd.getVersion() );

        assertNotNull( mmd.getVersioning() );

        List<String> versions = mmd.getVersioning().getVersions();

        assertNotNull( versions );
        assertEquals( 5, versions.size() );
        assertTrue( versions.contains( "1" ) );
        assertTrue( versions.contains( "2" ) );
        assertTrue( versions.contains( "3" ) );
        assertTrue( versions.contains( "4" ) );
        assertTrue( versions.contains( "5" ) );
    }

    // -------------------------------------------------------------------------
    public void testAddVersionTwiceOperation()
        throws Exception
    {
        File groupMd = new File( testBase, "group-maven-metadata.xml" );

        Metadata checkMd = getMetadata( groupMd );

        assertNotNull( checkMd );
        assertEquals( "a", checkMd.getGroupId() );
        assertEquals( "a", checkMd.getArtifactId() );
        assertEquals( "4", checkMd.getVersion() );

        assertNotNull( checkMd.getVersioning() );

        List<String> checkVersions = checkMd.getVersioning().getVersions();

        assertNotNull( checkVersions );
        assertEquals( 4, checkVersions.size() );
        assertTrue( checkVersions.contains( "1" ) );
        assertTrue( checkVersions.contains( "2" ) );
        assertTrue( checkVersions.contains( "3" ) );
        assertTrue( checkVersions.contains( "4" ) );
        assertFalse( checkVersions.contains( "5" ) );

        List<MetadataOperation> ops = new ArrayList<MetadataOperation>();
        ops.add( new AddVersionOperation( new StringOperand( "5" ) ) );
        ops.add( new AddVersionOperation( new StringOperand( "5" ) ) );

        File resFile = new File( testBase, "group-maven-metadata-write.xml" );

        Metadata mmd = applyOpsAndGetMetadata( checkMd, ops, resFile );

        assertNotNull( mmd );
        assertEquals( "a", mmd.getGroupId() );
        assertEquals( "a", mmd.getArtifactId() );
        assertEquals( "4", mmd.getVersion() );

        assertNotNull( mmd.getVersioning() );

        List<String> versions = mmd.getVersioning().getVersions();

        assertNotNull( versions );
        assertEquals( 5, versions.size() );
        assertTrue( versions.contains( "1" ) );
        assertTrue( versions.contains( "2" ) );
        assertTrue( versions.contains( "3" ) );
        assertTrue( versions.contains( "4" ) );
        assertTrue( versions.contains( "5" ) );
    }

    // -------------------------------------------------------------------------
    public void testRemoveVersionOperation()
        throws FileNotFoundException, IOException, XmlPullParserException, MetadataException
    {
        File groupMd = new File( testBase, "group-maven-metadata.xml" );
        byte[] targetBytes = FileUtil.readRawData( groupMd );

        byte[] resBytes =
            MetadataBuilder.changeMetadata( targetBytes, new RemoveVersionOperation( new StringOperand( "1" ) ) );

        File resFile = new File( testBase, "group-maven-metadata-write.xml" );

        FileUtil.writeRawData( resFile, resBytes );

        Metadata mmd = MetadataBuilder.read( new FileInputStream( resFile ) );

        assertNotNull( mmd );
        assertEquals( "a", mmd.getGroupId() );
        assertEquals( "a", mmd.getArtifactId() );
        assertEquals( "4", mmd.getVersion() );

        assertNotNull( mmd.getVersioning() );

        List<String> versions = mmd.getVersioning().getVersions();

        assertNotNull( versions );
        assertEquals( 3, versions.size() );
        assertTrue( !versions.contains( "1" ) );
        assertTrue( versions.contains( "2" ) );
        assertTrue( versions.contains( "3" ) );
        assertTrue( versions.contains( "4" ) );
    }

    public void testSetSnapshotVersionOperation()
        throws Exception
    {
        File mdFileBefore = new File( testBase, "group-maven-metadata.xml" );
        Metadata mdBefore = getMetadata( mdFileBefore );

        mdBefore.setVersion( "1.3.0-SNAPSHOT" );

        List<MetadataOperation> ops = new ArrayList<MetadataOperation>();
        ops.add( new SetSnapshotOperation( new StringOperand( "a-1.3.0-20090210.041603-374.pom" ) ) );
        ops.add( new SetSnapshotOperation( new StringOperand( "a-1.3.0-20090210.030701-373.pom" ) ) );
        ops.add( new SetSnapshotOperation( new StringOperand( "a-1.3.0-20090210.090218-375.pom" ) ) );
        ops.add( new SetSnapshotOperation( new StringOperand( "a-1.3.0-20090210.095716-376.pom" ) ) );

        File mdFileAfter = new File( testBase, "group-maven-metadata-write.xml" );
        Metadata mdAfter = applyOpsAndGetMetadata( mdBefore, ops, mdFileAfter );

        Assert.assertEquals( "20090210.095716", mdAfter.getVersioning().getSnapshot().getTimestamp() );
        Assert.assertEquals( 376, mdAfter.getVersioning().getSnapshot().getBuildNumber() );
    }

    // -------------------------------------------------------------------------
    public void testSetSnapshotOperation()
        throws FileNotFoundException, IOException, XmlPullParserException, MetadataException
    {
        File groupMd = new File( testBase, "group-maven-metadata.xml" );
        byte[] targetBytes = FileUtil.readRawData( groupMd );

        Snapshot sn = new Snapshot();
        sn.setLocalCopy( false );
        sn.setBuildNumber( 35 );
        String ts = TimeUtil.getUTCTimestamp();
        sn.setTimestamp( ts );

        byte[] resBytes =
            MetadataBuilder.changeMetadata( targetBytes, new SetSnapshotOperation( new SnapshotOperand( sn ) ) );

        File resFile = new File( testBase, "group-maven-metadata-write.xml" );

        FileUtil.writeRawData( resFile, resBytes );

        Metadata mmd = MetadataBuilder.read( new FileInputStream( resFile ) );

        assertNotNull( mmd );
        assertEquals( "a", mmd.getGroupId() );
        assertEquals( "a", mmd.getArtifactId() );
        assertEquals( "4", mmd.getVersion() );

        assertNotNull( mmd.getVersioning() );
        Snapshot snapshot = mmd.getVersioning().getSnapshot();
        assertNotNull( snapshot );
        assertEquals( ts, snapshot.getTimestamp() );

        // now let's drop sn
        targetBytes = FileUtil.readRawData( resFile );
        resBytes =
            MetadataBuilder.changeMetadata( targetBytes, new SetSnapshotOperation( new SnapshotOperand( null ) ) );

        Metadata mmd2 = MetadataBuilder.read( new ByteArrayInputStream( resBytes ) );

        assertNotNull( mmd2 );
        assertEquals( "a", mmd2.getGroupId() );
        assertEquals( "a", mmd2.getArtifactId() );
        assertEquals( "4", mmd2.getVersion() );

        assertNotNull( mmd2.getVersioning() );

        snapshot = mmd2.getVersioning().getSnapshot();
        assertNull( snapshot );
    }

    // -------------------------------------------------------------------------
    public void testMultipleOperations()
        throws FileNotFoundException, IOException, XmlPullParserException, MetadataException
    {
        File groupMd = new File( testBase, "group-maven-metadata.xml" );
        byte[] targetBytes = FileUtil.readRawData( groupMd );

        ArrayList<MetadataOperation> ops = new ArrayList<MetadataOperation>( 2 );
        ops.add( new RemoveVersionOperation( new StringOperand( "1" ) ) );
        ops.add( new AddVersionOperation( new StringOperand( "8" ) ) );

        byte[] resBytes = MetadataBuilder.changeMetadata( targetBytes, ops );

        File resFile = new File( testBase, "group-maven-metadata-write.xml" );

        FileUtil.writeRawData( resFile, resBytes );

        Metadata mmd = MetadataBuilder.read( new FileInputStream( resFile ) );

        assertNotNull( mmd );
        assertEquals( "a", mmd.getGroupId() );
        assertEquals( "a", mmd.getArtifactId() );
        assertEquals( "4", mmd.getVersion() );

        assertNotNull( mmd.getVersioning() );

        List<String> versions = mmd.getVersioning().getVersions();

        assertNotNull( versions );
        assertEquals( 4, versions.size() );
        assertTrue( !versions.contains( "1" ) );
        assertTrue( versions.contains( "2" ) );
        assertTrue( versions.contains( "3" ) );
        assertTrue( versions.contains( "4" ) );
        assertTrue( versions.contains( "8" ) );
    }
    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
}
