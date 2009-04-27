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

package org.apache.maven.mercury.util;

import java.util.Date;

import junit.framework.TestCase;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class TimeUtilTest
    extends TestCase
{
    long expected = 1234550304000L;

    public void testSnTsWithDash()
        throws Exception
    {
        long ts = TimeUtil.snTstoMillis( "20090213.183824-29" );

        assertEquals( expected, ts );
    }

    public void testSnTsWithoutDash()
        throws Exception
    {
        long ts = TimeUtil.snTstoMillis( "20090213.183824" );

        assertEquals( expected, ts );
    }

    public void testNow()
        throws Exception
    {
        long now = System.currentTimeMillis();

        System.out.println( new Date( now ) );
        System.out.println( TimeUtil.defaultToSnTs( now ) );

        String name = "a-1.0-20090213.183824-90.jar";

        int lastDash = name.lastIndexOf( '-' );
        int firstDash = name.lastIndexOf( '-', lastDash - 1 );
        String fTS = name.substring( firstDash + 1, lastDash );

        System.out.println( fTS );

    }
}
