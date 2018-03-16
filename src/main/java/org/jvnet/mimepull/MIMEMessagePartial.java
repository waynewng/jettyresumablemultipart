/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Represents MIME message. MIME message parsing is done lazily using a
 * pull parser.
 *
 * @author Jitendra Kotamraju
 */
/**
 * Customized from MIMEMessage. Methods are overridden because base class has all private variables.
 *
 * @author wayneng
 *
 */
public class MIMEMessagePartial extends MIMEMessage {

    // MIMEConfig config;

    /** No change from base class. */
    private final InputStream inPartial;
    /** No change from base class. */
    private final Iterator<MIMEEvent> itPartial;
    /** No change from base class. */
    private boolean parsedPartial; // true when entire message is parsed
    /** No change from base class. */
    private MIMEPartPartial currentPartPartial;
    /** No change from base class. */
    private int currentIndexPartial;

    /** preallocate lists and hashmap. */
    private final int initialCapacityPartial = 20;
    /** No change from base class. */
    private final List<MIMEPartPartial> partsListPartial = new ArrayList<>(initialCapacityPartial);
    /** No change from base class. */
    private final Map<String, MIMEPartPartial> partsMapPartial = new HashMap<>(initialCapacityPartial);
    /** boolean for partial message. */
    private boolean isPartial = false;

    /**
     * see MIMEMessage.MIMEMessage(InputStream, String, MIMEConfig)
     *
     * @param in InputStream.
     * @param boundary String.
     */
    public MIMEMessagePartial(final InputStream in, final String boundary) {
        this(in, boundary, new MIMEConfigPartial());
    }

    /**
     * Creates a MIME message from the content's stream. The content stream is closed when EOF is reached.
     *
     * @param in MIME message stream
     * @param boundary the separator for parts(pass it without --)
     * @param config various configuration parameters
     */
    public MIMEMessagePartial(final InputStream in, final String boundary, final MIMEConfigPartial config) {
        super(new ByteArrayInputStream(new String("").getBytes(StandardCharsets.UTF_8)), boundary, config);
        this.inPartial = in;
        this.config = config;
        MIMEParserPartial parser = new MIMEParserPartial(in, boundary, config);
        itPartial = parser.iterator();

        if (config.isParseEagerly()) {
            parseAllPartial();
        }
    }

    /**
     * No change from base class.
     *
     * Gets all the attachments by parsing the entire MIME message. Avoid this if possible since it is an expensive operation.
     *
     * @return list of attachments.
     */
    public List<MIMEPartPartial> getAttachmentsPartial() {
        if (!parsedPartial) {
            parseAllPartial();
        }
        return partsListPartial;
    }

    /**
     * No change from base class except logging removed.
     *
     * Creates nth attachment lazily. It doesn't validate if the message has so many attachments. To do the validation, the message needs to be
     * parsed. The parsing of the message is done lazily and is done while reading the bytes of the part.
     *
     * @param index sequential order of the part. starts with zero.
     * @return attachemnt part
     */
    @Override
    public MIMEPartPartial getPart(final int index) {
        MIMEPartPartial partPartial = (index < partsListPartial.size()) ? partsListPartial.get(index) : null;
        if (parsedPartial && partPartial == null) {
            throw new MIMEParsingException("There is no " + index + " attachment part ");
        }
        if (partPartial == null) {
            // Parsing will done lazily and will be driven by reading the part
            partPartial = new MIMEPartPartial(this);
            partsListPartial.add(index, partPartial);
        }
        return partPartial;
    }

    /**
     * No change from base class.
     *
     * Gets if MIMEMessage is partial payload.
     *
     * @return isPartial
     */
    public boolean getIsPartial() {
        if (!parsedPartial) {
            parseAllPartial();
        }
        return isPartial;
    }

    /**
     * No change from base class except logging removed.
     *
     * Creates a lazy attachment for a given Content-ID. It doesn't validate if the message contains an attachment with the given Content-ID. To do
     * the validation, the message needs to be parsed. The parsing of the message is done lazily and is done while reading the bytes of the part.
     *
     * @param contentId Content-ID of the part, expects Content-ID without brackets.
     * @return attachemnt part
     */
    @Override
    public MIMEPartPartial getPart(final String contentId) {
        MIMEPartPartial partPartial = getDecodedCidPart(contentId);
        if (parsedPartial && partPartial == null) {
            throw new MIMEParsingException("There is no attachment part with Content-ID = " + contentId);
        }
        if (partPartial == null) {
            // Parsing is done lazily and is driven by reading the part
            partPartial = new MIMEPartPartial(this, contentId);
            partsMapPartial.put(contentId, partPartial);
        }
        return partPartial;
    }

    /**
     * No change from base class.
     *
     * @param cid String.
     * @return MIMEPartPartial.
     */
    // this is required for Indigo interop, it writes content-id without escaping
    private MIMEPartPartial getDecodedCidPart(final String cid) {
        MIMEPartPartial partPartial = partsMapPartial.get(cid);
        if (partPartial == null) {
            if (cid.indexOf('%') != -1) {
                try {
                    String tempCid = URLDecoder.decode(cid, "utf-8");
                    partPartial = partsMapPartial.get(tempCid);
                } catch (UnsupportedEncodingException ue) {
                    // Ignore it
                }
            }
        }
        return partPartial;
    }

    /**
     * No change from base class.
     */
    public void parseAllPartial() {
        while (makeProgressPartial()) {
            // Nothing to do
        }
    }

    /**
     * Closes all parsed {@link org.jvnet.mimepull.MIMEPartPartial parts}. This method is safe to call even if parsing of message failed.
     *
     * Does not throw {@link org.jvnet.mimepull.MIMEParsingException} if an error occurred during closing a MIME part. The exception (if any) is still
     * logged.
     */
    @Override
    public void close() {
        closePartial(partsListPartial);
        closePartial(partsMapPartial.values());
    }

    /**
     * No change from base class, only removed logging.
     *
     * @param parts Collection.
     */
    private void closePartial(final Collection<MIMEPartPartial> parts) {
        for (final MIMEPartPartial part : parts) {
            try {
                part.close();
            } catch (final MIMEParsingException closeError) {
                // logging goes here.
            }
        }
    }

    /**
     * Only changed case CONTENT for partial message enabling and removed logging. No other change from base class.
     *
     * Parses the MIME message in a pull fashion.
     *
     * @return false if the parsing is completed.
     */
    //@Override
    public synchronized boolean makeProgressPartial() {
        if (!itPartial.hasNext()) {
            return false;
        }
        MIMEEvent eventPartial = itPartial.next();

        switch (eventPartial.getEventType()) {
        case START_MESSAGE:
            break;

        case START_PART:
            break;

        case HEADERS:
            MIMEEventPartial.Headers headers = (MIMEEventPartial.Headers) eventPartial;
            InternetHeaders ih = headers.getHeaders();
            List<String> cids = ih.getHeader("content-id");
            String cid = (cids != null) ? cids.get(0) : currentIndexPartial + "";
            if (cid.length() > 2 && cid.charAt(0) == '<') {
                cid = cid.substring(1, cid.length() - 1);
            }
            MIMEPartPartial listPart = (currentIndexPartial < partsListPartial.size()) ? partsListPartial.get(currentIndexPartial) : null;
            MIMEPartPartial mapPart = getDecodedCidPart(cid);
            if (listPart == null && mapPart == null) {
                currentPartPartial = getPart(cid);
                partsListPartial.add(currentIndexPartial, currentPartPartial);
            } else if (listPart == null) {
                currentPartPartial = mapPart;
                partsListPartial.add(currentIndexPartial, mapPart);
            } else if (mapPart == null) {
                currentPartPartial = listPart;
                currentPartPartial.setContentId(cid);
                partsMapPartial.put(cid, currentPartPartial);
            } else if (listPart != mapPart) {
                throw new MIMEParsingException("Created two different attachments using Content-ID and index");
            }
            currentPartPartial.setHeaders(ih);
            break;

        case CONTENT:
            MIMEEventPartial.ContentPartial content = (MIMEEventPartial.ContentPartial) eventPartial;
            ByteBuffer buf = content.getData();

            boolean partIsPartial = content.getIsPartial();
            if (partIsPartial) {
                currentPartPartial.setIsPartial(partIsPartial);
                isPartial = partIsPartial;
            }
            currentPartPartial.addBody(buf);
            break;

        case END_PART:
            currentPartPartial.doneParsing();
            ++currentIndexPartial;
            break;

        case END_MESSAGE:
            parsedPartial = true;
            try {
                inPartial.close();
            } catch (IOException ioe) {
                throw new MIMEParsingException(ioe);
            }
            break;

        default:
            throw new MIMEParsingException("Unknown Parser state = " + eventPartial.getEventType());
        }
        return true;
    }
}
