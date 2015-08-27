package eu.ehri.project.indexing;

import com.google.common.collect.Lists;
import eu.ehri.project.indexing.converter.Converter;
import eu.ehri.project.indexing.sink.Sink;
import eu.ehri.project.indexing.source.Source;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class PipelineTest {

    static class StringSource implements Source<String> {

        private final List<String> strings;

        public StringSource(List<String> strings) {
            this.strings = strings;
        }

        @Override
        public Iterable<String> getIterable() throws SourceException {
            return strings;
        }

        @Override
        public boolean isFinished() {
            return true;
        }

        @Override
        public void finish() throws SourceException {
        }
    }

    static class IntegerSink implements Sink<Integer> {
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

    static class StringToInteger implements Converter<String, Integer> {
        @Override
        public Iterable<Integer> convert(String t) throws ConverterException {
            return Lists.newArrayList(Integer.valueOf(t));
        }
    }

    @Test
    public void testPineline() throws Exception {
        List<Integer> out = Lists.newArrayList();

        Pipeline<String,Integer> pipeline = new Pipeline.Builder<String, Integer>()
                .addSource(new StringSource(Lists.newArrayList("1", "2", "3")))
                .addConverter(new StringToInteger())
                .addSink(new IntegerSink(out))
                .build();

        pipeline.run();
        assertEquals(Lists.newArrayList(1, 2, 3), out);
    }

    @Test(expected = IllegalStateException.class)
    public void testPipelineMissingConverted() throws Exception {
        new Pipeline.Builder<String, Integer>()
                .addSource(new StringSource(Lists.newArrayList("1", "2", "3")))
                .addSink(new IntegerSink(Lists.<Integer>newArrayList()))
                .build();
    }
}