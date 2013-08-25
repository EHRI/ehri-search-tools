package eu.ehri.project.indexer;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface CloseableIterable<T> extends Iterable<T> {
    public void close();
}
