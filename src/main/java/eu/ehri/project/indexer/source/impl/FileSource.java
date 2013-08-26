package eu.ehri.project.indexer.source.impl;

import eu.ehri.project.indexer.source.Source;
import org.codehaus.jackson.JsonNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 *         <p/>
 *         Use a file as a node source.
 */
public class FileSource implements Source<JsonNode> {
    private final InputStreamSource ios;
    private final FileInputStream fis;

    public FileSource(String fileName) {
        try {
            File file = new File(fileName);
            if (!(file.exists() || file.isFile())) {
                throw new SourceException(
                        "File does not exists, or is not a plain file: " + fileName);
            }
            this.fis = new FileInputStream(new File(fileName));
            this.ios = new InputStreamSource(fis);
        } catch (FileNotFoundException e) {
            throw new SourceException("File not found: " + fileName, e);
        }
    }

    public void finish() {
        ios.finish();
        try {
            fis.close();
        } catch (IOException e) {
            throw new SourceException("Unable to close file input stream", e);
        }
    }

    @Override
    public Iterator<JsonNode> iterator() {
        return ios.iterator();
    }
}
