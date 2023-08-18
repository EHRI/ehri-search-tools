package eu.ehri.project.indexing;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import eu.ehri.project.indexing.sink.impl.BufferSink;
import eu.ehri.project.indexing.sink.impl.CallbackSink;
import eu.ehri.project.indexing.source.impl.InputStreamJsonSource;
import eu.ehri.project.indexing.test.JsonToXml;
import eu.ehri.project.indexing.test.StringSource;
import eu.ehri.project.indexing.test.StringToInteger;
import eu.ehri.project.indexing.utils.Stats;
import org.junit.Test;
import org.w3c.dom.Node;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class PipelineTest {

    @Test
    public void testPipeline1() throws Exception {
        List<Number> out = Lists.newArrayList();

        new Pipeline.Builder<String, Number>()
                .addSource(new StringSource(Lists.newArrayList("1", "2", "3")))
                .addConverter(new StringToInteger())
                .addSink(new BufferSink<>(out))
                .build()
                .run();

        assertEquals(Lists.newArrayList(1, 2, 3), out);
    }

    @Test
    public void testPipeline2() throws Exception {
        String json = "[{\"foo\": \"bar\"}, {\"bar\": \"baz\"}]";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        final List<String> outText = Lists.newArrayList();
        final Stats stats = new Stats();

        new Pipeline.Builder<JsonNode, Node>()
                .addSource(new InputStreamJsonSource(stream))
                .addConverter(new JsonToXml("test"))
                .addSink(new CallbackSink<>(new CallbackSink.Callback<Node>() {
                    @Override
                    public void call(Node node) {
                        stats.incrementCount();
                        outText.add(JsonToXml.nodeToString(node));
                    }

                    @Override
                    public void finish() {
                    }
                }))
                .build()
                .run();

        assertEquals(2, stats.getCount());
        assertEquals(2, outText.size());
        assertEquals("<test>\n    <foo>bar</foo>\n</test>\n", outText.get(0));
    }

    @Test(expected = IllegalStateException.class)
    public void testPipelineMissingConverted() throws Exception {
        new Pipeline.Builder<String, Number>()
                .addSource(new StringSource(Lists.newArrayList("1", "2", "3")))
                .addSink(new BufferSink<>(Lists.<Number>newArrayList()))
                .build();
    }
}