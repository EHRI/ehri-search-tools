package eu.ehri.project.indexing.test;

import eu.ehri.project.indexing.sink.Sink;

import java.util.List;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class IntegerSink implements Sink<Integer> {
    private final List<? super Integer> buffer;

    public IntegerSink(List<? super Integer> buffer) {
        this.buffer = buffer;
    }

    @Override
    public void write(Integer Integer) throws SinkException {
        buffer.add(Integer);
    }

    @Override
    public void finish() throws SinkException {
    }
}
