package eu.ehri.project.indexer.sink;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface Sink<T> {
    public void write(T t);

    public void finish();
}
