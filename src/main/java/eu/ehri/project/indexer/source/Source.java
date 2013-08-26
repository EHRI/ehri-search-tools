package eu.ehri.project.indexer.source;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface Source<T> extends Iterable<T> {
    public class SourceException extends RuntimeException {
        public SourceException(String message) {
            super(message);
        }

        public SourceException(String message, Exception e) {
            super(message, e);
        }
    }

    public void finish() throws SourceException;
}
