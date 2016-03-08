package eu.ehri.project.indexing.converter.impl;

import com.google.common.collect.Lists;
import eu.ehri.project.indexing.converter.Converter;

/**
 * A converter that does nothing, returning a single-item
 * iterable of it's input item.
 */
public class NoopConverter<T> implements Converter<T, T> {
    @Override
    public Iterable<T> convert(T t) throws ConverterException {
        //noinspection unchecked
        return Lists.newArrayList(t);
    }
}
