package eu.ehri.project.indexing.source.impl;

import com.google.common.collect.Lists;
import eu.ehri.project.indexing.source.Source;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class MultiSourceTest {

    public static class TestSource implements Source<String> {

        private final List<String> data;
        private boolean finished = false;

        public TestSource(String... strings) {
            data = Lists.newArrayList(strings);
        }

        @Override
        public Iterable<String> getIterable() throws SourceException {
            return data;
        }

        @Override
        public boolean isFinished() {
            return finished;
        }

        @Override
        public void finish() throws SourceException {
            finished = true;
        }
    }

    @Test
    public void testMultiIteration() throws Exception {
        TestSource src1 = new TestSource("a", "b", "c");
        TestSource src2 = new TestSource("d", "e", "f");
        TestSource src3 = new TestSource("g", "h", "i");

        MultiSource<String> multiSource = new MultiSource<>(
                Lists.<Source<? extends String>>newArrayList(src1, src2, src3));
        Iterable<String> iterable = multiSource.getIterable();
        Iterator<String> iterator = iterable.iterator();
        assertEquals("a", iterator.next());
        assertEquals("b", iterator.next());
        assertEquals("c", iterator.next());
        assertTrue(iterator.hasNext());
        assertTrue(src1.isFinished());

        assertEquals("d", iterator.next());
        assertEquals("e", iterator.next());
        assertEquals("f", iterator.next());
        assertTrue(iterator.hasNext());
        assertTrue(src2.isFinished());

        assertEquals("g", iterator.next());
        assertEquals("h", iterator.next());
        assertEquals("i", iterator.next());
        assertFalse(iterator.hasNext());
        assertTrue(src3.isFinished());
    }
}
