package eu.ehri.project.indexer.source.impl;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.ehri.project.indexer.source.Source;

import java.util.List;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 *         <p/>
 *         Aggregate multiple sources into one.
 */
public class MultiSource<T> implements Source<T> {

    private final List<Source<T>> sources;

    public MultiSource(List<Source<T>> sources) {
        this.sources = sources;
    }

    @Override
    public void finish() throws SourceException {
        for (Source<T> reader : sources) {
            reader.finish();
        }
    }

    @Override
    public Iterable<T> getIterable() throws SourceException {
        return Iterables.concat(getReaders());
    }

    private Iterable<Iterable<T>> getReaders() throws SourceException {
        List<Iterable<T>> readers = Lists.newArrayList();
        for (Source<T> source: sources) {
            readers.add(source.getIterable());
        }
        return readers;
    }
}
