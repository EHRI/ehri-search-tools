package eu.ehri.project.indexer.index;

import java.io.InputStream;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface Index {

    public class IndexException extends RuntimeException {
        public IndexException(String message) {
            super(message);
        }

        public IndexException(String message, Exception e) {
            super(message, e);
        }
    }

    public void update(InputStream ios, boolean commit);

    public void commit();
}
