package eu.ehri.project.indexing;

import com.google.common.collect.Lists;
import eu.ehri.project.indexing.converter.Converter;
import eu.ehri.project.indexing.converter.impl.MultiConverter;
import eu.ehri.project.indexing.sink.Sink;
import eu.ehri.project.indexing.sink.impl.MultiSink;
import eu.ehri.project.indexing.sink.impl.NoopSink;
import eu.ehri.project.indexing.source.Source;
import eu.ehri.project.indexing.source.impl.MultiSource;
import eu.ehri.project.indexing.source.impl.NoopSource;

import java.util.List;

/**
 * A class to orchestrate flow from sources, converters,
 * and sinks.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class Pipeline<S, E> {

    protected final Source<S> source;
    protected final Sink<E> writer;
    protected final Converter<S, E> converter;

    /**
     * Builder for an Indexer. More options to come.
     */
    public static class Builder<S, E> {
        private final List<Source<S>> sources = Lists.newArrayList();
        private final List<Sink<E>> writers = Lists.newArrayList();
        private final List<Converter<S, E>> converters = Lists.newArrayList();

        public Builder<S, E> addSink(Sink<E> writer) {
            writers.add(writer);
            return this;
        }

        private Sink<E> getSink() {
            if (writers.size() > 1) {
                return new MultiSink<>(writers);
            } else if (writers.size() == 1) {
                return writers.get(0);
            } else {
                return new NoopSink<>();
            }
        }

        public Source<S> getSource() {
            if (sources.size() > 1) {
                return new MultiSource<>(sources);
            } else if (sources.size() == 1) {
                return sources.get(0);
            } else {
                return new NoopSource<>();
            }
        }

        public Builder<S, E> addSource(Source<S> source) {
            this.sources.add(source);
            return this;
        }

        public Converter<S, E> getConverter() {
            if (converters.size() > 1) {
                return new MultiConverter<>(converters);
            } else if (converters.size() == 1) {
                return converters.get(0);
            } else {
                throw new IllegalStateException("No converters defined " +
                        "for mapping sources to sinks");
            }
        }

        public Builder<S, E> addConverter(Converter<S, E> converter) {
            this.converters.add(converter);
            return this;
        }

        public Pipeline<S, E> build() {
            return new Pipeline<>(this);
        }
    }

    protected Pipeline(Builder<S, E> builder) {
        this.writer = builder.getSink();
        this.source = builder.getSource();
        this.converter = builder.getConverter();
    }

    /**
     * Perform the actual actions.
     */
    public void run() throws Source.SourceException, Sink.SinkException, Converter.ConverterException {
        try {
            for (S item : source.getIterable()) {
                for (E out : converter.convert(item)) {
                    writer.write(out);
                }
            }
        } finally {
            source.finish();
            writer.finish();
        }
    }
}
