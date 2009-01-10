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
package org.apache.maven.mercury.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class DefaultMonitor
    implements Monitor
{
    Writer _writer;
    boolean _timestamp = true;
    private static final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    

    public DefaultMonitor( boolean timestamp )
    {
        this( System.out );
        this._timestamp = timestamp;
    }

    public DefaultMonitor()
    {
        this( System.out );
    }

    public DefaultMonitor( OutputStream os )
    {
        _writer = new OutputStreamWriter( os );
    }

    public DefaultMonitor( Writer writer )
    {
        _writer = writer;
    }

    public void message( String msg )
    {
        try
        {
            if ( _writer != null )
            {
                if( _timestamp )
                {
                    _writer.write( fmt.format( new Date() ) );
                    _writer.write( ": " );
                }
                _writer.write( msg );
                _writer.write( "\n" );
                _writer.flush();
            }
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
    }

}
