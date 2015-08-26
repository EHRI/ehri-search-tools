package eu.ehri.project.indexing.sink;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface Sink<T> {

    class SinkException extends Exception {
        public SinkException(String message, Exception e) {
            super(message, e);
        }
    }

    void write(T t) throws SinkException;

    void finish() throws SinkException;
}
