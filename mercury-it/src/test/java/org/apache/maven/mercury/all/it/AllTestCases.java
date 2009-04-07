/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.maven.mercury.all.it;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.maven.mercury.dependency.tests.DependencyTreeBuilderTest;
import org.apache.maven.mercury.plexus.DefaultPlexusMercuryTest;
import org.apache.maven.mercury.repository.tests.ComprehensiveRepositoryTest;
import org.apache.maven.mercury.repository.tests.DavServerTest;
import org.apache.maven.mercury.repository.tests.LocalRepositoryReaderM2Test;
import org.apache.maven.mercury.repository.tests.LocalRepositoryWriterM2Test;
import org.apache.maven.mercury.repository.tests.ReadWriteTest;
import org.apache.maven.mercury.repository.tests.RemoteRepositoryCachingReaderM2Test;
import org.apache.maven.mercury.repository.tests.RemoteRepositoryReaderM2Test;
import org.apache.maven.mercury.repository.tests.RemoteRepositoryWriterM2Test;
import org.apache.maven.mercury.repository.tests.VirtualRepositoryReaderIntegratedTest;

/**
 * adopted from Maven ITs structure
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public class AllTestCases
    extends AbstractTestCase
{
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
    }
    
    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
    }
    
    public void testConfig()
    {
        
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite();

        /*
         * This must be the first one to ensure the local repository is properly setup.
         */
        suite.addTestSuite( DavServerTest.class );

        /*
         * Add tests in reverse alpha order by number below. This makes testing new
         * ITs quicker and since it counts down to zero, it's easier to judge how close
         * the tests are to finishing. Newer tests are also more likely to fail, so this is
         * a fail fast technique as well.
         */
        suite.addTestSuite( LocalRepositoryReaderM2Test.class );
        suite.addTestSuite( LocalRepositoryWriterM2Test.class );
        suite.addTestSuite( ReadWriteTest.class );
        suite.addTestSuite( RemoteRepositoryCachingReaderM2Test.class );
        suite.addTestSuite( RemoteRepositoryReaderM2Test.class );
        suite.addTestSuite( RemoteRepositoryWriterM2Test.class );
        suite.addTestSuite( DependencyTreeBuilderTest.class );
        suite.addTestSuite( VirtualRepositoryReaderIntegratedTest.class );
        suite.addTestSuite( DefaultPlexusMercuryTest.class );
        suite.addTestSuite( ComprehensiveRepositoryTest.class );


        /*
         * Add tests in reverse alpha order above.
         */

        return suite;
    }
}
