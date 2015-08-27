package eu.ehri.project.indexing.source.impl;

import eu.ehri.project.indexing.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Aggregate multiple sources into one.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class MultiSource<T, S extends Source<? extends T>> implements Source<T> {

    private final static Logger logger = LoggerFactory.getLogger(MultiSource.class);

    private final List<S> sources;

    private boolean finished = false;
    private S currentSource = null;
    private Iterator<? extends T> currentSourceIterator = null;

    public MultiSource(List<S> sources) {
        this.sources = sources;
    }

    @Override
    public void finish() throws SourceException {
        finished = true;
        logger.trace("Finish");
    }

    @Override
    public Iterable<T> getIterable() throws SourceException {
        final Queue<S> sourceQueue = new ArrayDeque<>(sources);
        return new Iterable<T>() {

            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    private boolean getNextValidIterator() {
                        try {
                            while (!sourceQueue.isEmpty()) {
                                if (currentSource != null) {
                                    currentSource.finish();
                                }
                                currentSource = sourceQueue.remove();
                                currentSourceIterator = currentSource.getIterable().iterator();
                                if (currentSourceIterator.hasNext()) {
                                    return true;
                                }
                            }
                            if (currentSource != null) {
                                currentSource.finish();
                            }
                            return false;
                        } catch (SourceException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public boolean hasNext() {
                        if (currentSourceIterator != null) {
                            return currentSourceIterator.hasNext() || getNextValidIterator();
                        } else {
                            return getNextValidIterator();
                        }
                    }

                    @Override
                    public T next() {
                        if (!hasNext()) throw new NoSuchElementException();
                        return currentSourceIterator.next();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @Override
    public boolean isFinished() {
        return finished;
    }
}
