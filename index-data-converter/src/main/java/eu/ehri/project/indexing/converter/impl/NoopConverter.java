package eu.ehri.project.indexing.converter.impl;

import com.google.common.collect.Lists;
import eu.ehri.project.indexing.converter.Converter;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class NoopConverter<T> implements Converter<T, T> {
    @Override
    public Iterable<T> convert(T t) throws ConverterException {
        //noinspection unchecked
        return Lists.newArrayList(t);
    }
}
