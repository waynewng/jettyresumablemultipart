/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.media.multipart.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.BodyPartEntityPartial;
import org.glassfish.jersey.media.multipart.BodyPartPartial;
import org.glassfish.jersey.media.multipart.FormDataBodyPartPartial;
import org.glassfish.jersey.media.multipart.FormDataMultiPartPartial;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartPartial;
import org.glassfish.jersey.media.multipart.MultiPartProperties;
import org.glassfish.jersey.media.multipart.Partial;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.jvnet.mimepull.Header;
import org.jvnet.mimepull.MIMEConfigPartial;
import org.jvnet.mimepull.MIMEMessagePartial;
import org.jvnet.mimepull.MIMEParsingException;
import org.jvnet.mimepull.MIMEPartPartial;

/**
 * This code is taken from MultiPartReaderClientSide {@link MultiPartReaderClientSide}. Only difference is this uses the partial mime parser.
 */
/**
 * {@link MessageBodyReader} implementation for {@link MultiPart} entities.
 *
 * @author Craig McClanahan
 * @author Paul Sandoz
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
@Consumes("multipart/*")
@Singleton
public class MultiPartPartialReader implements MessageBodyReader<MultiPart> {

    /**
     * Injectable helper to look up appropriate {@link MessageBodyReader}s for our body parts.
     */
    @Inject
    private Provider<MessageBodyWorkers> messageBodyWorkers;

    /**
     * MIME configuration.
     */
    private final MIMEConfigPartial mimeConfig;

    /** maxMIMESize. */
    private final int maxMIMESize = 45 * 1024 * 1024;

    /**
     * Accepts constructor injection of the configuration parameters for this
     * application.
     */
    @Inject
    public MultiPartPartialReader() {
        final MultiPartProperties properties = new MultiPartProperties();
        mimeConfig = createMimeConfig(properties, true);
    }

    /**
     * Create the mime config object.
     *
     * @param properties from this the mime config values will be taken.
     * @param enablePartial boolean.
     * @return the created mime config object.
     */
    private MIMEConfigPartial createMimeConfig(final MultiPartProperties properties, final boolean enablePartial) {
        final MIMEConfigPartial mimeConfig = new MIMEConfigPartial();

        // Set values defined by user.
        mimeConfig.setMemoryThreshold(properties.getBufferThreshold());

        mimeConfig.setMaxMIMESize(maxMIMESize);

        if (enablePartial) {
            mimeConfig.setEnablePartial(true);
        } else {
            mimeConfig.setEnablePartial(false);
        }

        final String tempDir = properties.getTempDir();
        if (tempDir != null) {
            mimeConfig.setDir(tempDir);
        }

        if (properties.getBufferThreshold() != MultiPartProperties.BUFFER_THRESHOLD_MEMORY_ONLY) {
            // Validate - this checks whether it's possible to create temp files in currently set temp directory.
            try {
                //noinspection ResultOfMethodCallIgnored
                File.createTempFile("MIME", null, tempDir != null ? new File(tempDir) : null).delete();
            } catch (final IOException ioe) {
                // LOGGER.log(Level.WARNING, LocalizationMessages.TEMP_FILE_CANNOT_BE_CREATED(properties.getBufferThreshold()), ioe);
            }
        }

        return mimeConfig;
    }

    /**
     * Refer to Jersey MultiPart.
     */
    @Override
    public boolean isReadable(final Class<?> type,
                              final Type genericType,
                              final Annotation[] annotations,
                              final MediaType mediaType) {
        return MultiPart.class.isAssignableFrom(type);
    }

    /**
     * Reads the entire list of body parts from the Input stream, using the
     * appropriate provider implementation to de-serialize each body part's entity.
     *
     * @param type        the class of the object to be read (i.e. {@link MultiPart}.class).
     * @param genericType the type of object to be written.
     * @param annotations annotations on the resource method that returned this object.
     * @param mediaType   media type ({@code multipart/*}) of this entity.
     * @param headers     mutable map of HTTP headers for the entire response.
     * @param stream      output stream to which the entity should be written.
     * @throws java.io.IOException                 if an I/O error occurs.
     * @throws javax.ws.rs.WebApplicationException If an HTTP error response needs to be produced (only effective if the response
     *                                             is not
     *                                             committed yet) or if the Content-Disposition header of a {@code
     *                                             multipart/form-data} body part
     *                                             cannot be parsed.
     */
    @Override
    public MultiPart readFrom(final Class<MultiPart> type,
                              final Type genericType,
                              final Annotation[] annotations,
                              final MediaType mediaType,
                              final MultivaluedMap<String, String> headers,
                              final InputStream stream) throws IOException, WebApplicationException {
        try {
            return readMultiPart(type, genericType, annotations, mediaType, headers, stream);
        } catch (final MIMEParsingException mpe) {
            if (mpe.getCause() instanceof IOException) {
                throw (IOException) mpe.getCause();
            } else {
                throw new BadRequestException(mpe);
            }
        }
    }

    /**
     * For Partial uploads.
     *
     * @param type refer to jersey.MultiPart.
     * @param genericType refer to jersey.MultiPart.
     * @param annotations refer to jersey.MultiPart.
     * @param mediaTypeIn refer to jersey.MultiPart.
     * @param headers refer to jersey.MultiPart.
     * @param stream refer to jersey.MultiPart.
     * @return refer to jersey.MultiPart.
     * @throws IOException refer to jersey.MultiPart.
     * @throws MIMEParsingException refer to jersey.MultiPart.
     */

    protected MultiPart readMultiPart(final Class<MultiPart> type,
                                      final Type genericType,
                                      final Annotation[] annotations,
            final MediaType mediaTypeIn,
                                      final MultivaluedMap<String, String> headers,
                                      final InputStream stream) throws IOException, MIMEParsingException {
        MediaType mediaType = unquoteMediaTypeParameters(mediaTypeIn, "boundary");

        final MIMEMessagePartial mimeMessage = new MIMEMessagePartial(stream,
                mediaType.getParameters().get("boundary"),
                mimeConfig);

        if (!mimeConfig.isEnablePartial() && mimeMessage.getIsPartial()) {
            mimeMessage.close();
            throw new BadRequestException("Request is partial input.");
        }

        final boolean formData = MediaTypes.typeEqual(mediaType, MediaType.MULTIPART_FORM_DATA_TYPE);
        final MultiPart multiPart = formData ? new FormDataMultiPartPartial() : new MultiPartPartial();

        final MessageBodyWorkers workers = messageBodyWorkers.get();
        multiPart.setMessageBodyWorkers(workers);

        final MultivaluedMap<String, String> multiPartHeaders = multiPart.getHeaders();
        for (final Map.Entry<String, List<String>> entry : headers.entrySet()) {
            final List<String> values = entry.getValue();

            for (final String value : values) {
                multiPartHeaders.add(entry.getKey(), value);
            }
        }

        final boolean fileNameFix;
        if (!formData) {
            multiPart.setMediaType(mediaType);
            fileNameFix = false;
        } else {
            // see if the User-Agent header corresponds to some version of MS Internet Explorer
            // if so, need to set fileNameFix to true to handle issue http://java.net/jira/browse/JERSEY-759
            final String userAgent = headers.getFirst(HttpHeaders.USER_AGENT);
            fileNameFix = userAgent != null && userAgent.contains(" MSIE ");
        }

        for (final MIMEPartPartial mimePart : getMimePartsPartial(mimeMessage)) {
            final BodyPart bodyPart = formData ? new FormDataBodyPartPartial(fileNameFix) : new BodyPartPartial();

            final boolean isPartial = mimePart.getIsPartial();
            if (isPartial) {
                final Partial ifcBodyPartPartial = formData ? (FormDataBodyPartPartial) bodyPart : (BodyPartPartial) bodyPart;
                final Partial ifcMultiPartPartial = formData ? (FormDataMultiPartPartial) multiPart : (MultiPartPartial) multiPart;

                ifcBodyPartPartial.setIsPartial(isPartial);
                ifcMultiPartPartial.setIsPartial(isPartial);
            }

            // Configure providers.
            bodyPart.setMessageBodyWorkers(workers);

            // Copy headers.
            for (final Header header : mimePart.getAllHeaders()) {
                bodyPart.getHeaders().add(header.getName(), header.getValue());
            }

            try {
                final String contentType = bodyPart.getHeaders().getFirst("Content-Type");
                if (contentType != null) {
                    if (mimePart.getIsPartial() && contentType.equals("application/json")) {
                        throw new BadRequestException("application/json is corrupted");
                    }
                    bodyPart.setMediaType(MediaType.valueOf(contentType));
                }
                bodyPart.getContentDisposition();
            } catch (final IllegalArgumentException ex) {
                throw new BadRequestException(ex);
            }

            // Copy data into a BodyPartEntity structure.
            bodyPart.setEntity(new BodyPartEntityPartial(mimePart));

            // Add this BodyPart to our MultiPart.
            multiPart.getBodyParts().add(bodyPart);
        }

        return multiPart;
    }

    /**
     * Refer to jersey.MultiPart.
     *
     * @param message refer to jersey.MultiPart.
     * @return refer to jersey.MultiPart.
     */
    private List<MIMEPartPartial> getMimePartsPartial(final MIMEMessagePartial message) {
        try {
            return message.getAttachmentsPartial();
        } catch (final MIMEParsingException obtainPartsError) {
            message.close();

            // Re-throw the exception.
            throw obtainPartsError;
        }
    }

    /**
     * Refer to jersey.MultiPart.
     *
     * @param mediaType refer to jersey.MultiPart.
     * @param parameters refer to jersey.MultiPart.
     * @return refer to jersey.MultiPart.
     */
    protected static MediaType unquoteMediaTypeParameters(final MediaType mediaType, final String... parameters) {
        if (parameters == null || parameters.length == 0) {
            return mediaType;
        }

        final Map<String, String> unquotedParams = new HashMap<>(mediaType.getParameters());
        for (final String parameter : parameters) {
            String value = mediaType.getParameters().get(parameter);

            if (value != null && value.startsWith("\"")) {
                value = value.substring(1, value.length() - 1);
                unquotedParams.put(parameter, value);
            }
        }

        return new MediaType(mediaType.getType(), mediaType.getSubtype(), unquotedParams);
    }
}
