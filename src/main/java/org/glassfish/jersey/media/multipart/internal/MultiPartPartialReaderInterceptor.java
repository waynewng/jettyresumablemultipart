package org.glassfish.jersey.media.multipart.internal;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import org.glassfish.jersey.media.multipart.MultiPartPartialBinder;
import org.glassfish.jersey.message.internal.EntityInputStream;

/**
 * This class is same as Terminal reader interceptor {ReaderInterceptorExecutor.TerminalReaderInterceptor} of Jersey. Only difference is this
 * one is using MultiPart parser that supports partial MIME. Jersey uses a single instance of this interceptor and call with input stream of
 * individual request.
 *
 * @author nabanita
 */
@Provider
@MultiPartPartialBinder
public class MultiPartPartialReaderInterceptor implements ReaderInterceptor {
    /** Injected MultiPartPartialReader object that supports partial mime. */
    @Inject
    private MultiPartPartialReader messageBodyReader;

    /**
     * Reading the input stream and creating mime object. Refer to ReaderInterceptorExecutor.TerminalReaderInterceptor.
     *
     * @param context interceptor context.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException {
            @SuppressWarnings("rawtypes")
        MessageBodyReader bodyReader = messageBodyReader;

        final EntityInputStream input = new EntityInputStream(context.getInputStream());
        final UnCloseableInputStream stream = new UnCloseableInputStream(input, bodyReader);

        Object entity = bodyReader.readFrom(context.getType(), context.getGenericType(), context.getAnnotations(), context.getMediaType(),
                context.getHeaders(), stream);
        return entity;
    }


    /**
     * This class is taken from Jersey UnCloseableInputStream of TerminalReaderInterceptor (@link TerminalReaderInterceptor} which convert the stream
     * to mime object.
     *
     * @author nabanita
     *
     */
    private static class UnCloseableInputStream extends InputStream {

        /** Input stream. */
        private final InputStream original;
        /**
         * Constructor.
         *
         * @param original input stream.
         * @param reader object which will be used to convert input stream to Java Object.
         */
        private UnCloseableInputStream(final InputStream original, final MessageBodyReader<?> reader) {
            this.original = original;
        }

        /**
         * Refer to ReaderInterceptorExecutor.TerminalReaderInterceptor.
         */
        @Override
        public int read() throws IOException {
            return original.read();
        }

        /**
         * Refer to ReaderInterceptorExecutor.TerminalReaderInterceptor.
         */
        @Override
        public int read(final byte[] b) throws IOException {
            return original.read(b);
        }

        /**
         * Refer to ReaderInterceptorExecutor.TerminalReaderInterceptor.
         */
        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            return original.read(b, off, len);
        }

        /**
         * Refer to ReaderInterceptorExecutor.TerminalReaderInterceptor.
         */
        @Override
        public long skip(final long l) throws IOException {
            return original.skip(l);
        }

        /**
         * Refer to ReaderInterceptorExecutor.TerminalReaderInterceptor.
         */
        @Override
        public int available() throws IOException {
            return original.available();
        }

        /**
         * Refer to ReaderInterceptorExecutor.TerminalReaderInterceptor.
         */
        @Override
        public synchronized void mark(final int i) {
            original.mark(i);
        }

        /**
         * Refer to ReaderInterceptorExecutor.TerminalReaderInterceptor.
         */
        @Override
        public synchronized void reset() throws IOException {
            original.reset();
        }

        /**
         * Refer to ReaderInterceptorExecutor.TerminalReaderInterceptor.
         */
        @Override
        public boolean markSupported() {
            return original.markSupported();
        }

        /**
         * Refer to ReaderInterceptorExecutor.TerminalReaderInterceptor.
         */
        @Override
        public void close() throws IOException {
        }
    }
};
