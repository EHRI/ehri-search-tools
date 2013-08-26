package eu.ehri.project.indexer.source.impl;

import com.google.common.collect.Iterables;
import eu.ehri.project.indexer.source.Source;

import java.util.Iterator;
import java.util.List;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 *         <p/>
 *         Aggregate multiple sources into one.
 */
public class MultiSource<T> implements Source<T> {

    private final List<Source<T>> readers;

    public MultiSource(List<Source<T>> readers) {
        this.readers = readers;
    }

    @Override
    public void finish() {
        for (Source<T> reader : readers) {
            reader.finish();
        }
    }

    @Override
    public Iterator iterator() {
        return Iterables.concat(
                readers.toArray(new Source[readers.size()])).iterator();
    }
}
