package eu.ehri.project.indexing.source.impl;

import com.fasterxml.jackson.databind.JsonNode;
import eu.ehri.project.indexing.source.Source;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Use a file as a node source.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class FileJsonSource implements Source<JsonNode> {
    private final String fileName;
    private InputStreamJsonSource ios;
    private FileInputStream fis;
    private boolean finished = false;

    public FileJsonSource(String fileName) {
        this.fileName = fileName;
    }

    public void finish() throws SourceException {
        finished = true;
        if (ios != null) {
            ios.finish();
        }
        try {
            if (fis != null) {
                fis.close();
            }
        } catch (IOException e) {
            throw new SourceException("Unable to close file input stream", e);
        }
    }

    @Override
    public Iterable<JsonNode> getIterable() throws SourceException {
        try {
            File file = new File(fileName);
            if (!(file.exists() && file.isFile())) {
                throw new SourceException(
                        "File does not exists, or is not a plain file: " + fileName);
            }
            this.fis = new FileInputStream(new File(fileName));
            this.ios = new InputStreamJsonSource(fis);
        } catch (FileNotFoundException e) {
            throw new SourceException("File not found: " + fileName, e);
        }
        return ios.getIterable();
    }

    @Override
    public boolean isFinished() {
        return finished;
    }
}
