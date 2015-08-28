package eu.ehri.project.indexing.sink.impl;

import com.google.common.collect.Lists;
import eu.ehri.project.indexing.sink.Sink;

import java.util.List;

/**
 * Aggregate several sink together.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class MultiSink<T, W extends Sink<? super T>> implements Sink<T> {

    private final List<W> writers;

    public MultiSink(List<W> writers) {
        this.writers = Lists.newArrayList(writers);
    }

    public void write(T t) throws SinkException {
        for (W writer : writers) {
            writer.write(t);
        }
    }

    public void close() throws SinkException {
        for (W writer : writers) {
            writer.close();
        }
    }
}
