package eu.ehri.project.indexing;

import com.google.common.collect.Lists;
import eu.ehri.project.indexing.test.NumberSink;
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
        List<Number> out = Lists.newArrayList();

        new Pipeline.Builder<String, Number>()
                .addSource(new StringSource(Lists.newArrayList("1", "2", "3")))
                .addConverter(new StringToInteger())
                .addSink(new NumberSink(out))
                .build()
                .run();

        assertEquals(Lists.newArrayList(1, 2, 3), out);
    }

    @Test(expected = IllegalStateException.class)
    public void testPipelineMissingConverted() throws Exception {
        new Pipeline.Builder<String, Number>()
                .addSource(new StringSource(Lists.newArrayList("1", "2", "3")))
                .addSink(new NumberSink(Lists.<Number>newArrayList()))
                .build();
    }
}