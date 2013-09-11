package eu.ehri.project.indexer.converter;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public interface Converter<T> {



    public Iterable<T> convert(T t);
}
