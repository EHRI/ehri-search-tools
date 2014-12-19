package eu.ehri.project.indexing.source;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface Source<T> {
    public class SourceException extends Exception {
        public SourceException(String message) {
            super(message);
        }

        public SourceException(String message, Exception e) {
            super(message, e);
        }
    }

    public Iterable<T> getIterable() throws SourceException;

    public boolean isFinished();

    public void finish() throws SourceException;
}
