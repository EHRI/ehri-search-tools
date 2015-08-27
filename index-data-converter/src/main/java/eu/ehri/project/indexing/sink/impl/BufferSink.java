package eu.ehri.project.indexing.sink.impl;

import eu.ehri.project.indexing.sink.Sink;

import java.util.Collection;
import java.util.List;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class BufferSink<T> implements Sink<T> {
    private final Collection<? super T> buffer;

    public BufferSink(Collection<? super T> buffer) {
        this.buffer = buffer;
    }

    @Override
    public void write(T t) throws SinkException {
        buffer.add(t);
    }

    @Override
    public void finish() throws SinkException {
    }
}
