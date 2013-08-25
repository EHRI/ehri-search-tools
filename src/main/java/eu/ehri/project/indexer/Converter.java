package eu.ehri.project.indexer;

import java.io.IOException;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface Converter<T> {
    public Iterable<T> convert(T t) throws IOException;
}
