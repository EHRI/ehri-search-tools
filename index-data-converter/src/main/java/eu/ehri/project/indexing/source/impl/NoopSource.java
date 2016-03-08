package eu.ehri.project.indexing.source.impl;

import com.google.common.collect.ImmutableSet;
import eu.ehri.project.indexing.source.Source;

import java.util.Iterator;

/**
 * Source which does Nada. Mainly here for symmetry.
 */
public class NoopSource<T> implements Source<T> {
    @Override
    public void close() throws SourceException {
    }

    @Override
    public Iterable<T> iterable() throws SourceException {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return ImmutableSet.<T>of().iterator();
            }
        };
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
