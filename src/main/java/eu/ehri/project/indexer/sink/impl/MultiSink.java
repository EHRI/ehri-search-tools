package eu.ehri.project.indexer.sink.impl;

import com.google.common.collect.Lists;
import eu.ehri.project.indexer.sink.Sink;

import java.util.List;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 *         <p/>
 *         Aggregate several sink together.
 */
public class MultiSink<T, W extends Sink<T>> implements Sink<T> {

    private final List<W> writers;

    public MultiSink(List<W> writers) {
        this.writers = Lists.newArrayList(writers);
    }

    public void write(T t) throws SinkException {
        for (W writer : writers) {
            writer.write(t);
        }
    }

    public void finish() throws SinkException {
        for (W writer : writers) {
            writer.finish();
        }
    }
}
