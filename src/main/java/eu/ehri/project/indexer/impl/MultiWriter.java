package eu.ehri.project.indexer.impl;

import com.google.common.collect.Lists;
import eu.ehri.project.indexer.Writer;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 *
 * Aggregate several writers together.
 */
public class MultiWriter<T, W extends Writer<T>> implements Writer<T> {

    private final List<W> writers;

    public MultiWriter(W... writers) {
        this.writers = Lists.newArrayList(writers);
    }

    public void write(T t) {
        for (W writer : writers) {
            writer.write(t);
        }
    }

    public void close() {
        for (W writer : writers) {
            writer.close();
        }
    }
}
