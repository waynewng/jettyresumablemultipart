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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Represents MIME message. MIME message parsing is done lazily using a
 * pull parser.
 *
 * @author Jitendra Kotamraju
 */
/**
 * Customized MIMEParser. All methods overridden because base class has privates.
 *
 * @author wayneng
 *
 */
class MIMEParserPartial extends MIMEParser {

    /** No change from base class. */
    private static final String HEADER_ENCODING = "ISO8859-1";

    // Actually, the grammar doesn't support whitespace characters
    // after boundary. But the mail implementation checks for it.
    // We will only check for these many whitespace characters after boundary
    /** No change from base class. */
    private static final int NO_LWSP = 1000;

    /** No change from base class. */
    private enum STATE {
        /** No change from base class. */
        START_MESSAGE,
        /** No change from base class. */
        SKIP_PREAMBLE,
        /** No change from base class. */
        START_PART,
        /** No change from base class. */
        HEADERS, BODY,
        /** No change from base class. */
        END_PART,
        /** No change from base class. */
        END_MESSAGE
    }

    /** No change from base class. */
    private STATE statePartial = STATE.START_MESSAGE;

    /** No change from base class. */
    private final InputStream inPartial;
    /** No change from base class. */
    private final byte[] bndbytesPartial;
    /** No change from base class. */
    private final int blPartial;
    /** This is derived from base class. */
    private final MIMEConfigPartial configPartial;
    /** No change from base class. */
    private final int[] bcsPartial = new int[128]; // BnM algo: Bad Character Shift table
    /** No change from base class. */
    private final int[] gssPartial; // BnM algo : Good Suffix Shift table

    /**
     * Have we parsed the data from our InputStream yet?
     */
    private boolean parsedPartial;

    /*
     * Read and process body partsList until we see the terminating boundary line (or EOF).
     */
    /** No change from base class. */
    private boolean donePartial = false;

    /** No change from base class. */
    private boolean eofPartial;
    /** No change from base class. */
    private final int capacityPartial;
    /** No change from base class. */
    private byte[] bufPartial;
    /** No change from base class. */
    private int lenPartial;
    /** No change from base class. */
    private boolean bolPartial; // beginning of the line
    /** boolean for partial messages. */
    protected boolean isPartial = false;
    /** maxInputStreamSizePartial limit. */
    protected final int maxInputStreamSizePartial;
    /** counter for number of bytes read from InputStream. */
    private int totalBytesReadPartial;
    /** truncate size for partial message. */
    protected final int moduloTruncateSizePartial = 4;


    /**
     * Parses the MIME content. At the EOF, it also closes input stream.
     *
     * This calls super with min memory alloc, adds totalBytes behavior. Made params final. No other changes from base class.
     *
     * @param in InputStream.
     * @param boundary String.
     * @param config MIMEConfig.
     */
    MIMEParserPartial(final InputStream in, final String boundary, final MIMEConfigPartial config) {
        super(new ByteArrayInputStream(new String("").getBytes(StandardCharsets.UTF_8)), boundary, new MIMEConfigPartial(false, 1, 2, false));
        this.inPartial = in;
        this.bndbytesPartial = getBytes("--" + boundary);
        blPartial = bndbytesPartial.length;
        this.configPartial = config;
        gssPartial = new int[blPartial];
        this.totalBytesReadPartial = 0;
        this.maxInputStreamSizePartial = configPartial.getMaxMIMESize();
        compileBoundaryPattern();

        // \r\n + boundary + "--\r\n" + lots of LWSP
        capacityPartial = configPartial.chunkSize + 2 + blPartial + 4 + NO_LWSP;
        createBuf(capacityPartial);
    }

    /**
     * No change from base class. Returns iterator for the parsing events. Use the iterator to advance the parsing.
     *
     * @return iterator for parsing events
     */
    @Override
    public Iterator<MIMEEvent> iterator() {
        return new MIMEEventIterator();
    }

    /**
     * Customized from base class for partial message enabling. Mostly no change from base class.
     *
     * @author wayneng
     *
     */
    class MIMEEventIterator implements Iterator<MIMEEvent> {

        /**
         * No change from base class.
         */
        @Override
        public boolean hasNext() {
            return !parsedPartial;
        }

        /**
         * This is the next implementation for iterator.
         *
         * Only BODY case is changed to support partial, and logging removed. No change from base class.
         */
        @Override
        public MIMEEvent next() {
            switch (statePartial) {
            case START_MESSAGE:
                statePartial = STATE.SKIP_PREAMBLE;
                return MIMEEvent.START_MESSAGE;

            case SKIP_PREAMBLE:
                skipPreamble();
                // fall through
            case START_PART:
                statePartial = STATE.HEADERS;
                return MIMEEvent.START_PART;

            case HEADERS:
                InternetHeaders ih = readHeaders();
                statePartial = STATE.BODY;
                bolPartial = true;
                return new MIMEEvent.Headers(ih);

            case BODY:
                ByteBuffer bufPartial = readBody();
                bolPartial = false;
                if (isPartial) {
                    return new MIMEEventPartial.ContentPartial(bufPartial, isPartial);
                }
                return new MIMEEventPartial.ContentPartial(bufPartial);

            case END_PART:
                if (donePartial) {
                    statePartial = STATE.END_MESSAGE;
                } else {
                    statePartial = STATE.START_PART;
                }
                return MIMEEventPartial.END_PART;

            case END_MESSAGE:
                parsedPartial = true;
                return MIMEEvent.END_MESSAGE;

            default:
                throw new MIMEParsingException("Unknown Parser state = " + statePartial);
            }
        }

        /**
         * No change from base class.
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * No change from base class. Collects the headers for the current part by parsing mesage stream.
     *
     * @return headers for the current part
     */
    private InternetHeaders readHeaders() {
        if (!eofPartial) {
            fillBuf();
        }
        return new InternetHeaders(new LineInputStream());
    }

    /**
     * Only logging is removed and partial message enabling added. No other change from base class.
     *
     * Reads and saves the part of the current attachment part's content. At the end of this method, bufPartial should have the remaining data at index 0.
     *
     * @return a chunk of the part's content
     *
     */
    private ByteBuffer readBody() {
        if (!eofPartial) {
            fillBuf();
        }
        int start = match(bufPartial, 0, lenPartial); // matches boundary
        if (start == -1) {
            // No boundary is found
            assert eofPartial || lenPartial >= configPartial.chunkSize;
            int chunkSize = eofPartial ? lenPartial : configPartial.chunkSize;
            if (eofPartial) {
                donePartial = true;
                if (configPartial.isEnablePartial()) {
                    // This is for partial message parsing.
                    isPartial = true;
                    statePartial = STATE.END_PART;
                    // This is for truncating at 4 byte boundaries for base64 encoding, and don't care for other encoding formats.
                    final int remaining = lenPartial - chunkSize;
                    final int chunkSizeMod4 = chunkSize % moduloTruncateSizePartial;
                    final int chunkSizeTruncated = chunkSize - chunkSizeMod4;
                    return adjustBuf(chunkSizeTruncated, remaining);
                } else {
                    throw new MIMEParsingException("Reached EOF, but there is no closing MIME boundary.");
                }
            }
            return adjustBuf(chunkSize, lenPartial - chunkSize);
        }
        // Found boundary.
        // Is it at the start of a line ?
        int chunkLen = start;
        if (bolPartial && start == 0) {
            // nothing to do
        } else if (start > 0 && (bufPartial[start - 1] == '\n' || bufPartial[start - 1] == '\r')) {
            --chunkLen;
            if (bufPartial[start - 1] == '\n' && start > 1 && bufPartial[start - 2] == '\r') {
                --chunkLen;
            }
        } else {
            return adjustBuf(start + 1, lenPartial - start - 1); // boundary is not at beginning of a line
        }

        if (start + blPartial + 1 < lenPartial && bufPartial[start + blPartial] == '-' && bufPartial[start + blPartial + 1] == '-') {
            statePartial = STATE.END_PART;
            donePartial = true;
            return adjustBuf(chunkLen, 0);
        }

        // Consider all the whitespace in boundary+whitespace+"\r\n"
        int lwsp = 0;
        for (int i = start + blPartial; i < lenPartial && (bufPartial[i] == ' ' || bufPartial[i] == '\t'); i++) {
            ++lwsp;
        }

        // Check for \n or \r\n in boundary+whitespace+"\n" or boundary+whitespace+"\r\n"
        if (start + blPartial + lwsp < lenPartial && bufPartial[start + blPartial + lwsp] == '\n') {
            statePartial = STATE.END_PART;
            return adjustBuf(chunkLen, lenPartial - start - blPartial - lwsp - 1);
        } else if (start + blPartial + lwsp + 1 < lenPartial && bufPartial[start + blPartial + lwsp] == '\r' && bufPartial[start + blPartial + lwsp + 1] == '\n') {
            statePartial = STATE.END_PART;
            return adjustBuf(chunkLen, lenPartial - start - blPartial - lwsp - 2);
        } else if (start + blPartial + lwsp + 1 < lenPartial) {
            return adjustBuf(chunkLen + 1, lenPartial - chunkLen - 1); // boundary string in a part data
        } else if (eofPartial) {
            donePartial = true;
            throw new MIMEParsingException("Reached EOF, but there is no closing MIME boundary.");
        }

        // Some more data needed to determine if it is indeed a proper boundary
        return adjustBuf(chunkLen, lenPartial - chunkLen);
    }

    /**
     * No change from base class.
     *
     * Returns a chunk from the original buffer. A new buffer is created with the remaining bytes.
     *
     * @param chunkSize create a chunk with these many bytes
     * @param remaining bytes from the end of the buffer that need to be copied to the beginning of the new buffer
     * @return chunk
     */
    private ByteBuffer adjustBuf(final int chunkSize, final int remaining) {
        assert bufPartial != null;
        assert chunkSize >= 0;
        assert remaining >= 0;

        byte[] temp = bufPartial;
        // create a new bufPartial and adjust it without this chunk
        createBuf(remaining);
        System.arraycopy(temp, lenPartial - remaining, bufPartial, 0, remaining);
        lenPartial = remaining;

        return ByteBuffer.wrap(temp, 0, chunkSize);
    }

    /**
     * No change from base class.
     *
     * @param min int.
     */
    private void createBuf(final int min) {
        bufPartial = new byte[min < capacityPartial ? capacityPartial : min];
    }

    /**
     * Skips the preamble to find the first attachment part.
     *
     * No change from base class except logging removed.
     *
     */
    private void skipPreamble() {

        while (true) {
            if (!eofPartial) {
                fillBuf();
            }
            int start = match(bufPartial, 0, lenPartial); // matches boundary
            if (start == -1) {
                // No boundary is found
                if (eofPartial) {
                    throw new MIMEParsingException("Missing start boundary");
                } else {
                    adjustBuf(lenPartial - blPartial + 1, blPartial - 1);
                    continue;
                }
            }

            if (start > configPartial.chunkSize) {
                adjustBuf(start, lenPartial - start);
                continue;
            }
            // Consider all the whitespace boundary+whitespace+"\r\n"
            int lwsp = 0;
            for (int i = start + blPartial; i < lenPartial && (bufPartial[i] == ' ' || bufPartial[i] == '\t'); i++) {
                ++lwsp;
            }
            // Check for \n or \r\n
            if (start + blPartial + lwsp < lenPartial && (bufPartial[start + blPartial + lwsp] == '\n' || bufPartial[start + blPartial + lwsp] == '\r')) {
                if (bufPartial[start + blPartial + lwsp] == '\n') {
                    adjustBuf(start + blPartial + lwsp + 1, lenPartial - start - blPartial - lwsp - 1);
                    break;
                } else if (start + blPartial + lwsp + 1 < lenPartial && bufPartial[start + blPartial + lwsp + 1] == '\n') {
                    adjustBuf(start + blPartial + lwsp + 2, lenPartial - start - blPartial - lwsp - 2);
                    break;
                }
            }
            adjustBuf(start + 1, lenPartial - start - 1);
        }
    }

    /**
     * No change from base class.
     *
     * @param s String.
     * @return byte array.
     */
    private static byte[] getBytes(final String s) {
        char[] chars = s.toCharArray();
        int size = chars.length;
        byte[] bytes = new byte[size];

        for (int i = 0; i < size;) {
            bytes[i] = (byte) chars[i++];
        }
        return bytes;
    }

    /**
     * No change from base class. Boyer-Moore search method. Copied from java.util.regex.Pattern.java
     *
     * Pre calculates arrays needed to generate the bad character shift and the good suffix shift. Only the last seven bits are used to see if chars
     * match; This keeps the tables small and covers the heavily used ASCII range, but occasionally results in an aliased match for the bad character
     * shift.
     */
    private void compileBoundaryPattern() {
        int i, j;

        // Precalculate part of the bad character shift
        // It is a table for where in the pattern each
        // lower 7-bit value occurs
        for (i = 0; i < bndbytesPartial.length; i++) {
            bcsPartial[bndbytesPartial[i] & 0x7F] = i + 1;
        }

        // Precalculate the good suffix shift
        // i is the shift amount being considered
        NEXT: for (i = bndbytesPartial.length; i > 0; i--) {
            // j is the beginning index of suffix being considered
            for (j = bndbytesPartial.length - 1; j >= i; j--) {
                // Testing for good suffix
                if (bndbytesPartial[j] == bndbytesPartial[j - i]) {
                    // src[j..lenPartial] is a good suffix
                    gssPartial[j - 1] = i;
                } else {
                    // No match. The array has already been
                    // filled up with correct values before.
                    continue NEXT;
                }
            }
            // This fills up the remaining of optoSft
            // any suffix can not have larger shift amount
            // then its sub-suffix. Why???
            while (j > 0) {
                gssPartial[--j] = i;
            }
        }
        // Set the guard value because of unicode compression
        gssPartial[bndbytesPartial.length - 1] = 1;
    }

    /**
     * Finds the boundary in the given buffer using Boyer-Moore algo. Copied from java.util.regex.Pattern.java Params changed to final and added
     * copied variable because of final variables. No other change from base class.
     *
     * @param mybuf boundary to be searched in this mybuf
     * @param off start index in mybuf
     * @param lenPartial number of bytes in mybuf
     *
     * @return -1 if there is no match or index where the match starts
     */
    private int match(final byte[] mybuf, final int off, final int lenPartial) {
        int last = lenPartial - bndbytesPartial.length;
        int offset = off;

        // Loop over all possible match positions in text
        NEXT: while (offset <= last) {
            // Loop over pattern from right to left
            for (int j = bndbytesPartial.length - 1; j >= 0; j--) {
                byte ch = mybuf[offset + j];
                if (ch != bndbytesPartial[j]) {
                    // Shift search to the right by the maximum of the
                    // bad character shift and the good suffix shift
                    offset += Math.max(j + 1 - bcsPartial[ch & 0x7F], gssPartial[j]);
                    continue NEXT;
                }
            }
            // Entire pattern matched starting at off
            return offset;
        }
        return -1;
    }

    /**
     * Adds max bytes read from InputStream behavior. No change from base class.
     *
     * Fills the remaining bufPartial to the full capacity
     */
    private void fillBuf() {
        assert !eofPartial;
        while (lenPartial < bufPartial.length) {
            int read;
            try {
                read = inPartial.read(bufPartial, lenPartial, bufPartial.length - lenPartial);
            } catch (IOException ioe) {
                final String msg = ioe.getMessage();
                if (msg != null) {
                    read = -1;
                } else {
                    throw new MIMEParsingException(ioe);
                }
            }
            if (read == -1) {
                eofPartial = true;
                try {
                    inPartial.close();
                } catch (IOException ioe) {
                    throw new MIMEParsingException(ioe);
                }
                break;
            } else {
                lenPartial += read;
                this.totalBytesReadPartial += read;
                if (this.totalBytesReadPartial > maxInputStreamSizePartial) {
                    throw new MIMEParsingException("MIMEParserYM total bytes read exceeds limit of 45 MB.");
                }
            }
        }
    }

    /**
     * No change from base class.
     */
    private void doubleBuf() {
        byte[] temp = new byte[2 * lenPartial];
        System.arraycopy(bufPartial, 0, temp, 0, lenPartial);
        bufPartial = temp;
        if (!eofPartial) {
            fillBuf();
        }
    }

    /**
     * No change from base class.
     *
     * @author wayneng
     *
     */
    class LineInputStream extends MIMEParser.LineInputStream {
        /** No change from base class. */
        private int offset;

        /**
         * No change from base class.
         *
         * Read a line containing only ASCII characters from the input stream. A line is terminated by a CR or NL or CR-NL sequence. A common error is
         * a CR-CR-NL sequence, which will also terminate a line. The line terminator is not returned as part of the returned String. Returns null if
         * no data is available.
         * <p>
         *
         * This class is similar to the deprecated <code>DataInputStream.readLine()</code>
         */
        @Override
        public String readLine() throws IOException {

            int hdrLen = 0;
            int lwsp = 0;
            while (offset + hdrLen < lenPartial) {
                if (bufPartial[offset + hdrLen] == '\n') {
                    lwsp = 1;
                    break;
                }
                if (offset + hdrLen + 1 == lenPartial) {
                    doubleBuf();
                }
                if (offset + hdrLen + 1 >= lenPartial) { // No more data in the stream
                    assert eofPartial;
                    return null;
                }
                if (bufPartial[offset + hdrLen] == '\r' && bufPartial[offset + hdrLen + 1] == '\n') {
                    lwsp = 2;
                    break;
                }
                ++hdrLen;
            }
            if (hdrLen == 0) {
                adjustBuf(offset + lwsp, lenPartial - offset - lwsp);
                return null;
            }

            String hdr = new String(bufPartial, offset, hdrLen, HEADER_ENCODING);
            offset += hdrLen + lwsp;
            return hdr;
        }

    }
}
