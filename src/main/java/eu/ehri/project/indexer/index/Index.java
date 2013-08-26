package eu.ehri.project.indexer.index;

import java.io.InputStream;
import java.util.List;

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

    public void deleteAll(boolean commit) throws IndexException;

    public void deleteItem(String id, boolean commit) throws IndexException;

    public void deleteItems(List<String> ids, boolean commit) throws IndexException;

    public void deleteType(String type, boolean commit) throws IndexException;

    public void deleteTypes(List<String> types, boolean commit) throws IndexException;

    public void update(InputStream ios, boolean commit);

    public void commit();
}
