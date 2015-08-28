package eu.ehri.project.indexing.source;

/**
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

    Iterable<T> iterable() throws SourceException;

    boolean isFinished();

    void close() throws SourceException;
}
