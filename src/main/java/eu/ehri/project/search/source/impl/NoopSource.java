package eu.ehri.project.search.source.impl;

import com.google.common.collect.Iterators;
import eu.ehri.project.search.source.Source;

import java.util.Iterator;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 *         <p/>
 *         Souce which does Nada. Mainly here for symmetry.
 */
public class NoopSource<T> implements Source<T> {
    @Override
    public void finish() throws SourceException {
    }

    @Override
    public Iterable<T> getIterable() throws SourceException {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return Iterators.emptyIterator();
            }
        };
    }
}
