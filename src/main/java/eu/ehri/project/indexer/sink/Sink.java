package eu.ehri.project.indexer.sink;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface Sink<T> {

    public class SinkException extends Exception {
        public SinkException(String message) {
            super(message);
        }

        public SinkException(String message, Exception e) {
            super(message, e);
        }
    }

    public void write(T t) throws SinkException;

    public void finish() throws SinkException;
}
