package org.jvnet.mimepull;

import java.nio.ByteBuffer;

/**
 * Customized class for partial message enabling. Nothing else.
 *
 * @author wayneng
 *
 */
abstract class MIMEEventPartial extends MIMEEvent {
    @Override
    abstract EVENT_TYPE getEventType();


    /**
     * Customized class for partial message enabling. Nothing else.
     *
     * @author wayneng
     *
     */
    static final class ContentPartial extends MIMEEventPartial {
        /** No change from base class. */
        private final ByteBuffer bufPartial;
        /** For partial message enabling. */
        private final boolean isPartial;

        /**
         * No change from base class except partial enabling.
         *
         * @param buf ByteBuffer.
         */
        ContentPartial(ByteBuffer buf) {
            this(buf, false);
        }

        /**
         * No change from base class except partial enabling.
         *
         * @param buf ByteBuffer.
         * @param isPartial boolean.
         */
        ContentPartial(ByteBuffer buf, boolean isPartial) {
            this.bufPartial = buf;
            this.isPartial = isPartial;
        }

        /**
         * No change from base class.
         */
        @Override
        EVENT_TYPE getEventType() {
            return EVENT_TYPE.CONTENT;
        }

        /**
         * No change from base class.
         */
        ByteBuffer getData() {
            return bufPartial;
        }

        /**
         * getter for isPartial.
         *
         * @return boolean.
         */
        boolean getIsPartial() {
            return isPartial;
        }
    }
}
