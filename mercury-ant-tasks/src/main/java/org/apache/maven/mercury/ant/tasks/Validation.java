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

package org.apache.maven.mercury.ant.tasks;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class Validation
{
    public static final String TYPE_PGP = "pgp";

    public static final String TYPE_SHA1 = "sha1";

    public static final String PROP_PUBLIC_KEYRING = "keyring";

    public static final String PROP_SECRET_KEYRING = "secret-keyring";

    public static final String PROP_SECRET_KEY_ID = "secret-keyring";

    public static final String PROP_SECRET_KEY_PASS = "secret-keyring";

    protected boolean _pgpValidation = false;

    protected String _pgpPublicKeyring = Config.DEFAULT_PUBLIC_KEYRING;

    protected String _pgpSecretKeyring = Config.DEFAULT_SECRET_KEYRING;

    protected String _pgpSecretKey;

    protected String _pgpSecretKeyPass;

    protected boolean _sha1Validation = false;

    protected boolean _sha1Signature = true;
}
