package eu.ehri.project.indexer.index;

import java.io.InputStream;
import java.util.List;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 *         <p/>
 *         General interface for common search engine operations.
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

    /**
     * Delete everything in the index.
     *
     * @param commit Whether or not to commit the action.
     * @throws IndexException
     */
    public void deleteAll(boolean commit) throws IndexException;

    /**
     * Delete an item with the given ID or itemId.
     *
     * @param id     The item's id or itemId.
     * @param commit Whether or not to commit the action.
     * @throws IndexException
     */
    public void deleteItem(String id, boolean commit) throws IndexException;

    /**
     * Delete all items with a given field value.
     *
     * @param field    The field name
     * @param value  The field value
     * @param commit Whether or not to commit the action.
     * @throws IndexException
     */
    public void deleteByFieldValue(String field, String value, boolean commit) throws IndexException;

    /**
     * Delete items identified by a set of ids or itemIds.
     *
     * @param ids    A set of ids matching items to delete.
     * @param commit Whether or not to commit the action.
     * @throws IndexException
     */
    public void deleteItems(List<String> ids, boolean commit) throws IndexException;

    /**
     * Delete items belong to a given type.
     *
     * @param type   The type of objects to deleteByQuery.
     * @param commit Whether or not to commit the action.
     * @throws IndexException
     */
    public void deleteType(String type, boolean commit) throws IndexException;

    /**
     * Delete items belonging to a list of types.
     *
     * @param types  The types of objects to deleteByQuery.
     * @param commit Whether or not to commit the action.
     * @throws IndexException
     */
    public void deleteTypes(List<String> types, boolean commit) throws IndexException;

    /**
     * Run a Solr update operation given arbitrary InputStream data.
     *
     * @param ios    The InputStream to upload
     * @param commit Whether or not to commit the upload immediately.
     */
    public void update(InputStream ios, boolean commit);

    /**
     * Commit outstanding Solr transactions.
     */
    public void commit();
}
