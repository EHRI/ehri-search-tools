package eu.ehri.project.indexer;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface Writer<T> {
    public void write(T t);

    public void close();
}
