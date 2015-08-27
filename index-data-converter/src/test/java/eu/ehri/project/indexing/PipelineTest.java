package eu.ehri.project.indexing;

import com.google.common.collect.Lists;
import eu.ehri.project.indexing.test.IntegerSink;
import eu.ehri.project.indexing.test.StringSource;
import eu.ehri.project.indexing.test.StringToInteger;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class PipelineTest {

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