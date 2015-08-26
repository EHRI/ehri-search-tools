package eu.ehri.project.indexing.index.impl;

import com.google.common.io.ByteStreams;
import eu.ehri.project.indexing.index.Index;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A dummy index that buffers updates to a string.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class StringBufferIndex implements Index {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    @Override
    public void deleteAll(boolean commit) throws IndexException {}

    @Override
    public void deleteItem(String id, boolean commit) throws IndexException {}

    @Override
    public void deleteByFieldValue(String field, String value, boolean commit) throws IndexException {}

    @Override
    public void deleteItems(List<String> ids, boolean commit) throws IndexException {}

    @Override
    public void deleteType(String type, boolean commit) throws IndexException {}

    @Override
    public void deleteTypes(List<String> types, boolean commit) throws IndexException {}

    @Override
    public void update(InputStream ios, boolean commit) {
        try {
            ByteStreams.copy(ios, buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void commit() {}

    public String getBuffer() {
        try {
            return buffer.toString(StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
