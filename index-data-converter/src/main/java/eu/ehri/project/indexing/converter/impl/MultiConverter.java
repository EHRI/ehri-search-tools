package eu.ehri.project.indexing.converter.impl;

import com.google.common.collect.Lists;
import eu.ehri.project.indexing.converter.Converter;

import java.util.List;

/**
 * Adapter to aggregate several converters.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class MultiConverter<S, E> implements Converter<S, E> {
    private final List<Converter<S, E>> converters;

    public MultiConverter(List<Converter<S, E>> converters) {
        this.converters = Lists.newArrayList(converters);
    }

    @Override
    public Iterable<E> convert(S t) throws ConverterException {
        List<E> temp = Lists.newArrayList();
        for (Converter<S, E> converter : converters) {
            for (E out : converter.convert(t)) {
                temp.add(out);
            }
        }
        return temp;
    }
}
