package eu.ehri.project.indexing.test;

import eu.ehri.project.indexing.source.Source;

import java.util.List;


public class StringSource implements Source<String> {

    private final List<String> strings;

    public StringSource(List<String> strings) {
        this.strings = strings;
    }

    @Override
    public Iterable<String> iterable() throws SourceException {
        return strings;
    }

    @Override
    public boolean isFinished() {
        return true;
    }

    @Override
    public void close() throws SourceException {
    }
}
