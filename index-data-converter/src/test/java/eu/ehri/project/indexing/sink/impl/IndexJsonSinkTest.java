package eu.ehri.project.indexing.sink.impl;

import com.fasterxml.jackson.databind.node.TextNode;
import eu.ehri.project.indexing.index.impl.StringBufferIndex;
import org.junit.Test;

import static org.junit.Assert.*;

public class IndexJsonSinkTest {

    @Test
    public void testWrite() throws Exception {
        StringBufferIndex dummy = new StringBufferIndex();
        IndexJsonSink sink = new IndexJsonSink(dummy);
        sink.write(new TextNode("foo"));
        sink.write(new TextNode("bar"));
        sink.finish();
        assertEquals("[\"foo\",\"bar\"]", dummy.getBuffer());
    }
}