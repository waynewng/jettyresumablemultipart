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

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

/** 
 * Represents an attachment part in a MIME message. MIME message parsing is done
 * lazily using a pull parser, so the part may not have all the data. {@link #read}
 * and {@link #readOnce} may trigger the actual parsing the message. In fact,
 * parsing of an attachment part may be triggered by calling {@link #read} methods
 * on some other attachment parts. All this happens behind the scenes so the 
 * application developer need not worry about these details.
 *
 * @author Jitendra Kotamraju, Martin Grebac
 */
/**
 * Customized class from MIMEPart to support partial message. Everything overridden because of base class private variables.
 *
 * @author wayneng
 *
 */
public class MIMEPartPartial extends MIMEPart {

    /** No change from base class. */
    private volatile boolean closedPartial;
    /** No change from base class. */
    private volatile InternetHeaders headersPartial;
    /** No change from base class. */
    private volatile String contentIdPartial;
    /** No change from base class. */
    private String contentTypePartial;
    /** No change from base class. */
    private String contentTransferEncodingPartial;
    /** No change from base class. */
    private volatile boolean isPartial;

    // volatile boolean parsed; // part is parsed or not
    /** No change from base class. */
    final MIMEMessagePartial msgPartial;
    /** No change from base class. */
    private final DataHead dataHeadPartial;

    /** No change from base class. */
    private final Object lockPartial = new Object();

    /**
     * No change from base class.
     *
     * @param msg MIMEMessagePartial.
     */
    MIMEPartPartial(final MIMEMessagePartial msg) {
        super(msg);
        this.msgPartial = msg;
        this.dataHeadPartial = new DataHead(this);
    }

    /**
     * No change from base class.
     *
     * @param msg MIMEMessagePartial.
     * @param contentId String.
     */
    MIMEPartPartial(final MIMEMessagePartial msg, final String contentId) {
        this(msg);
        this.contentIdPartial = contentId;
    }

    /**
     * No change from base class.
     *
     * Can get the attachment part's content multiple times. That means the full content needs to be there in memory or on the file system. Calling
     * this method would trigger parsing for the part's data. So do not call this unless it is required(otherwise, just wrap MIMEPart into a object
     * that returns InputStream for e.g DataHandler)
     *
     * @return data for the part's content
     */
    @Override
    public InputStream read() {
        InputStream is = null;
        try {
            is = MimeUtility.decode(dataHeadPartial.read(), contentTransferEncodingPartial);
        } catch (DecodingException ex) { // ignore
            // logging goes here
        }
        return is;
    }

    /**
     * No change from base class.
     *
     * Cleans up any resources that are held by this part (for e.g. deletes the temp file that is used to serve this part's content). After calling
     * this, one shouldn't call {@link #read()} or {@link #readOnce()}
     */
    @Override
    public void close() {
        if (!closedPartial) {
            synchronized (lockPartial) {
                if (!closedPartial) {
                    dataHeadPartial.close();
                    closedPartial = true;
                }
            }
        }
    }

    /**
     * No change from base class.
     *
     * Can get the attachment part's content only once. The content will be lost after the method. Content data is not be stored on the file system or
     * is not kept in the memory for the following case: - Attachement parts contents are accessed sequentially
     *
     * In general, take advantage of this when the data is used only once.
     *
     * @return data for the part's content
     */
    @Override
    public InputStream readOnce() {
        InputStream is = null;
        try {
            is = MimeUtility.decode(dataHeadPartial.readOnce(), contentTransferEncodingPartial);
        } catch (DecodingException ex) { // ignore
            // logging goes here
        }
        return is;
    }

    /**
     * No change from base class.
     */
    @Override
    public void moveTo(final File f) {
        dataHeadPartial.moveTo(f);
    }

    /**
     * No change from base class.
     *
     * Returns Content-ID MIME header for this attachment part
     *
     * @return Content-ID of the part
     */
    @Override
    public String getContentId() {
        if (contentIdPartial == null) {
            getHeaders();
        }
        return contentIdPartial;
    }

    /**
     * No change from base class.
     *
     * Returns Content-Transfer-Encoding MIME header for this attachment part
     *
     * @return Content-Transfer-Encoding of the part
     */
    @Override
    public String getContentTransferEncoding() {
        if (contentTransferEncodingPartial == null) {
            getHeaders();
        }
        return contentTransferEncodingPartial;
    }

    /**
     * No change from base class.
     *
     * Returns Content-Type MIME header for this attachment part
     *
     * @return Content-Type of the part
     */
    @Override
    public String getContentType() {
        if (contentTypePartial == null) {
            getHeaders();
        }
        return contentTypePartial;
    }

    /**
     * No change from base class.
     *
     */
    private void getHeaders() {
        // Trigger parsing for the part headers
        while (headersPartial == null) {
            if (!msgPartial.makeProgressPartial()) {
                if (headersPartial == null) {
                    throw new IllegalStateException("Internal Error. Didn't get Headers even after complete parsing.");
                }
            }
        }
    }

    /**
     * No change from base class.
     *
     * Return all the values for the specified header. Returns <code>null</code> if no headers with the specified name exist.
     *
     * @param name header name
     * @return list of header values, or null if none
     */
    @Override
    public List<String> getHeader(final String name) {
        getHeaders();
        assert headersPartial != null;
        return headersPartial.getHeader(name);
    }

    /**
     * No change from base class.
     *
     * Return all the headers
     *
     * @return list of Header objects
     */
    @Override
    public List<? extends Header> getAllHeaders() {
        getHeaders();
        assert headersPartial != null;
        return headersPartial.getAllHeaders();
    }

    /**
     * No change from base class.
     *
     * Callback to set headers
     *
     * @param headers MIME headers for the part
     */
    @Override
    void setHeaders(final InternetHeaders headers) {
        this.headersPartial = headers;
        List<String> ct = getHeader("Content-Type");
        this.contentTypePartial = (ct == null) ? "application/octet-stream" : ct.get(0);
        List<String> cte = getHeader("Content-Transfer-Encoding");
        this.contentTransferEncodingPartial = (cte == null) ? "binary" : cte.get(0);
    }

    /**
     * Callback to set contentPartial.
     *
     * I don't think this needs to be synchronized.
     *
     * @param isPartial boolean.
     */
    void setIsPartial(final boolean isPartial) {
        this.isPartial = isPartial;
    }

    /**
     * Gets if MIMEPart is partial content.
     *
     * @return isPartial boolean.
     */
    public boolean getIsPartial() {
        return isPartial;
    }

    /**
     * No change from base class.
     *
     * Callback to notify that there is a partial content for the part
     *
     * @param buf content data for the part
     */
    @Override
    void addBody(final ByteBuffer buf) {
        dataHeadPartial.addBody(buf);
    }

    /**
     * No change from base class.
     *
     * Callback to indicate that parsing is done for this part (no more update events for this part)
     */
    @Override
    void doneParsing() {
        parsed = true;
        dataHeadPartial.doneParsing();
    }

    /**
     * No change from base class.
     *
     * Callback to set Content-ID for this part
     *
     * @param cid Content-ID of the part
     */
    @Override
    void setContentId(final String cid) {
        this.contentIdPartial = cid;
    }

    /**
     * No change from base class.
     *
     * Callback to set Content-Transfer-Encoding for this part
     *
     * @param cte Content-Transfer-Encoding of the part
     */
    @Override
    void setContentTransferEncoding(final String cte) {
        this.contentTransferEncodingPartial = cte;
    }

    /**
     * Return {@code true} if this part has already been closed, {@code false} otherwise.
     *
     * @return {@code true} if this part has already been closed, {@code false} otherwise.
     */
    @Override
    public boolean isClosed() {
        return closedPartial;
    }

    /**
     * No change from base class.
     */
    @Override
    public String toString() {
        return "Part=" + contentIdPartial + ":" + contentTransferEncodingPartial;
    }

}
