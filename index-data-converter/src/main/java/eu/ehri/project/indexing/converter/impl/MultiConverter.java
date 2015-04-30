package eu.ehri.project.indexing.converter.impl;

import com.google.common.collect.Lists;
import eu.ehri.project.indexing.converter.Converter;

import java.util.List;

/**
 * Adapter to aggregate several converters.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class MultiConverter<T> implements Converter<T> {
    private final List<Converter<T>> converters;

    public MultiConverter(List<Converter<T>> converters) {
        this.converters = Lists.newArrayList(converters);
    }

    @Override
    public Iterable<T> convert(T t) throws ConverterException {
        List<T> temp = Lists.newArrayList();
        for (Converter<T> converter : converters) {
            for (T out : converter.convert(t)) {
                temp.add(out);
            }
        }
        return temp;
    }
}
