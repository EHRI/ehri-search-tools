package eu.ehri.project.indexing.source.impl;

import com.google.common.collect.Lists;
import eu.ehri.project.indexing.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 *         <p/>
 *         Aggregate multiple sources into one.
 */
public class MultiSource<T> implements Source<T> {

    private final static Logger logger = LoggerFactory.getLogger(MultiSource.class);

    private final List<Source<T>> sources;

    private boolean finished = false;
    private Source<T> currentSource = null;
    private Iterator<T> currentSourceIterator = null;

    public MultiSource(Source<T> ... sources) {
        this.sources = Lists.newArrayList(sources);
    }

    public MultiSource(List<Source<T>> sources) {
        this.sources = sources;
    }

    @Override
    public void finish() throws SourceException {
        finished = true;
        logger.trace("Finish");
    }

    @Override
    public Iterable<T> getIterable() throws SourceException {
        final Queue<Source<T>> sourceQueue = new ArrayDeque<Source<T>>(sources);
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
