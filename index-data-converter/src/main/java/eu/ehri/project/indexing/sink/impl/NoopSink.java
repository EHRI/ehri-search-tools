package eu.ehri.project.indexing.sink.impl;

import eu.ehri.project.indexing.sink.Sink;

/**
 * Basically /dev/null
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class NoopSink<T> implements Sink<T> {

    public NoopSink() {
    }

    public void write(T t) {
    }

    public void finish() {
    }
}
