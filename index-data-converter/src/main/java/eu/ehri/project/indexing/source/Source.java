package eu.ehri.project.indexing.source;

/**
 * A class that produces items of a given type T.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface Source<T> extends AutoCloseable {
    class SourceException extends Exception {
        public SourceException(String message) {
            super(message);
        }

        public SourceException(String message, Exception e) {
            super(message, e);
        }
    }

    /**
     * An iterable of the source's items.
     *
     * @return a set of data
     * @throws SourceException if there is an iteration error
     */
    Iterable<T> iterable() throws SourceException;

    /**
     * Determine if the source is finished producing items.
     *
     * @return true|false
     */
    boolean isFinished();

    /**
     * Close the source and release resources
     *
     * @throws SourceException if there is an error on close.
     */
    void close() throws SourceException;
}
