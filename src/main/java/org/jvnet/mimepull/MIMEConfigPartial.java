/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.jvnet.mimepull;

/**
 * Configuration for MIME message parsing and storing.
 *
 * This is a customized extension for constructor overload that passes in sizing info. Everything else is overridden because of private vars.
 *
 */
/**
 * Configuration for MIME message parsing and storing.
 *
 * This is a customized extension for constructor overload that passes in sizing info. Everything else is overridden because of private vars.
 *
 * @author Jitendra Kotamraju
 */
public class MIMEConfigPartial extends MIMEConfig {

    /** DEFAULT CHUNK SIZE for buffer sizing. No change from base class. */
    private static final int DEFAULT_CHUNK_SIZE = 8192;
    /** DEFAULT_MEMORY_THRESHOLD for max buffer sizing. No change from base class. */
    private static final long DEFAULT_MEMORY_THRESHOLD = 1048576L;
    /** DEFAULT_FILE_PREFIX for MIME. No change from base class. */
    private static final String DEFAULT_FILE_PREFIX = "MIME";
    /** DEFAULT_MAX_MIME_SIZE for max size. */
    private static final int DEFAULT_MAX_MIME_SIZE = 45 * 1024 * 1024;

    /** New field to support partial message. */
    boolean enablePartial;

    /** Max size to parse. */
    int maxMIMESize;

    /**
     * Private Constructor. Added only enablePartial, and no other change from base class.
     *
     * @param parseEagerly boolean.
     * @param chunkSize int.
     * @param inMemoryThreshold long.
     * @param dir String.
     * @param prefix String.
     * @param suffix String.
     */
    private MIMEConfigPartial(boolean parseEagerly, int chunkSize, long inMemoryThreshold, String dir, String prefix, String suffix,
            final boolean enablePartial, final int maxMIMESize) {
        this.parseEagerly = parseEagerly;
        this.chunkSize = chunkSize;
        this.memoryThreshold = inMemoryThreshold;
        this.prefix = prefix;
        this.suffix = suffix;
        this.enablePartial = enablePartial;
        this.maxMIMESize = maxMIMESize;
        setDir(dir);
    }

    /**
     * Constructor overload for YM. This is different from base class. Used in MIMEParserPartial.
     *
     * @param parseEagerly boolean.
     * @param chunkSize int.
     * @param inMemoryThreshold long.
     * @param enablePartial boolean.
     */
    public MIMEConfigPartial(final boolean parseEagerly, final int chunkSize, final long inMemoryThreshold, final boolean enablePartial) {
        this(parseEagerly, chunkSize, inMemoryThreshold, null, DEFAULT_FILE_PREFIX, null, enablePartial, DEFAULT_MAX_MIME_SIZE);
    }

    /**
     * Default constructor. Added only enablePartial, and no other change from base class. Used in customized Jersey MultiPartPartial.
     */
    public MIMEConfigPartial() {
        this(false, DEFAULT_CHUNK_SIZE, DEFAULT_MEMORY_THRESHOLD, null, DEFAULT_FILE_PREFIX, null, true, DEFAULT_MAX_MIME_SIZE);
    }

    /**
     * Get boolean enablePartial.
     *
     * @return boolean.
     */
    public boolean isEnablePartial() {
        return this.enablePartial;
    }

    /**
     * Set boolean enablePartial.
     *
     * @param enablePartial boolean.
     */
    public void setEnablePartial(final boolean enablePartial) {
        this.enablePartial = enablePartial;
    }

    /**
     * Get max MIME size.
     *
     * @return int.
     */
    public int getMaxMIMESize() {
        return this.maxMIMESize;
    }

    /**
     * Set max MIME size.
     *
     * @param maxMIMESize int.
     */
    public void setMaxMIMESize(final int maxMIMESize) {
        this.maxMIMESize = maxMIMESize;
    }
}
