package eu.ehri.project.indexer.sink.impl;

import eu.ehri.project.indexer.sink.Sink;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 *         <p/>
 *         /dev/null
 */
public class NoopSink<T> implements Sink<T> {

    public NoopSink() {
    }

    public void write(T t) {
    }

    public void close() {
    }
}
