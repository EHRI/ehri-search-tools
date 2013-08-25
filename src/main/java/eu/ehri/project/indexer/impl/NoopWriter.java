package eu.ehri.project.indexer.impl;

import eu.ehri.project.indexer.Indexer;
import eu.ehri.project.indexer.Writer;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class NoopWriter<T> implements Writer<T> {

    public NoopWriter() {
    }

    public void write(T t) {
    }

    public void close() {
    }
}
