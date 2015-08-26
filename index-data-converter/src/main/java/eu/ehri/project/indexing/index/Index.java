package eu.ehri.project.indexing.index;

import java.io.InputStream;
import java.util.List;

/**
 * General interface for common search engine operations.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface Index {

    class IndexException extends RuntimeException {
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
    void deleteAll(boolean commit) throws IndexException;

    /**
     * Delete an item with the given ID or itemId.
     *
     * @param id     The item's id or itemId.
     * @param commit Whether or not to commit the action.
     * @throws IndexException
     */
    void deleteItem(String id, boolean commit) throws IndexException;

    /**
     * Delete all items with a given field value.
     *
     * @param field    The field name
     * @param value  The field value
     * @param commit Whether or not to commit the action.
     * @throws IndexException
     */
    void deleteByFieldValue(String field, String value, boolean commit) throws IndexException;

    /**
     * Delete items identified by a set of ids or itemIds.
     *
     * @param ids    A set of ids matching items to delete.
     * @param commit Whether or not to commit the action.
     * @throws IndexException
     */
    void deleteItems(List<String> ids, boolean commit) throws IndexException;

    /**
     * Delete items belong to a given type.
     *
     * @param type   The type of objects to deleteByQuery.
     * @param commit Whether or not to commit the action.
     * @throws IndexException
     */
    void deleteType(String type, boolean commit) throws IndexException;

    /**
     * Delete items belonging to a list of types.
     *
     * @param types  The types of objects to deleteByQuery.
     * @param commit Whether or not to commit the action.
     * @throws IndexException
     */
    void deleteTypes(List<String> types, boolean commit) throws IndexException;

    /**
     * Run a Solr update operation given arbitrary InputStream data.
     *
     * @param ios    The InputStream to upload
     * @param commit Whether or not to commit the upload immediately.
     */
    void update(InputStream ios, boolean commit);

    /**
     * Commit outstanding Solr transactions.
     */
    void commit();
}
