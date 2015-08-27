package eu.ehri.project.indexing.test;

import eu.ehri.project.indexing.sink.Sink;

import java.util.List;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class NumberSink implements Sink<Number> {
    private final List<? super Number> buffer;

    public NumberSink(List<? super Number> buffer) {
        this.buffer = buffer;
    }

    @Override
    public void write(Number number) throws SinkException {
        buffer.add(number);
    }

    @Override
    public void finish() throws SinkException {
    }
}
