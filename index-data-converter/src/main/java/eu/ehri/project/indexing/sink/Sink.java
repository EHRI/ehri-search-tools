package eu.ehri.project.indexing.sink;

/**
 * A class that consumes some data.
 */
public interface Sink<T> extends AutoCloseable {

    class SinkException extends Exception {
        public SinkException(String message, Exception e) {
            super(message, e);
        }
    }

    /**
     * Write some data to the sink.
     *
     * @param t a data item
     * @throws SinkException if there is an error writing
     */
    void write(T t) throws SinkException;

    /**
     * Close the sink and release resources
     *
     * @throws SinkException if there is an error closing the sink
     */
    void close() throws SinkException;
}
