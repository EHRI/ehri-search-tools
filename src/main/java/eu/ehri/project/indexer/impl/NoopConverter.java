package eu.ehri.project.indexer.impl;

import com.google.common.collect.Lists;
import eu.ehri.project.indexer.Converter;

import java.io.IOException;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class NoopConverter<T> implements Converter<T> {
    @Override
    public Iterable<T> convert(T t) throws IOException {
        // This is ugly, but never mind
        return Lists.newArrayList(t);
    }
}
