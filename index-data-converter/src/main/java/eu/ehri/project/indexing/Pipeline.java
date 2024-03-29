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
 */
public class Pipeline<S, E> {

    protected final Source<? extends S> source;
    protected final Sink<? super E> writer;
    protected final Converter< S, ? extends E> converter;

    /**
     * Pipeline builder.
     */
    public static class Builder<S, E> {
        private final List<Source<? extends S>> sources = Lists.newArrayList();
        private final List<Converter< S, ? extends E>> converters = Lists.newArrayList();
        private final List<Sink<E>> writers = Lists.newArrayList();

        public Builder<S, E> addSink(Sink<E> writer) {
            writers.add(writer);
            return this;
        }

        public Builder<S, E> addSource(Source<S> source) {
            this.sources.add(source);
            return this;
        }

        public Builder<S, E> addConverter(Converter<S, ? extends E> converter) {
            this.converters.add(converter);
            return this;
        }

        private Sink<? super E> getSink() {
            if (writers.size() > 1) {
                return new MultiSink<>(writers);
            } else if (writers.size() == 1) {
                return writers.get(0);
            } else {
                return new NoopSink<>();
            }
        }

        private Source<? extends S> getSource() {
            if (sources.size() > 1) {
                return new MultiSource<>(sources);
            } else if (sources.size() == 1) {
                return sources.get(0);
            } else {
                return new NoopSource<>();
            }
        }

        private Converter<S, ? extends E> getConverter() {
            if (converters.size() > 1) {
                return new MultiConverter<>(converters);
            } else if (converters.size() == 1) {
                return converters.get(0);
            } else {
                throw new IllegalStateException("No converters defined " +
                        "for mapping sources to sinks");
            }
        }

        /**
         * Build a pipeline with the given sources, converters,
         * and sinks.
         *
         * @return a new pipeline
         * @throws IllegalStateException if there are no converters provided
         */
        public Pipeline<S, E> build() {
            return new Pipeline<>(getSource(), getConverter(), getSink());
        }
    }

    protected Pipeline(Source<? extends S> source,
                       Converter<S, ? extends E> converter,
                       Sink<? super E> sink) {
        this.writer = sink;
        this.source = source;
        this.converter = converter;
    }

    /**
     * Initiate the pipeline.
     *
     * @throws eu.ehri.project.indexing.source.Source.SourceException when a source errors
     * @throws eu.ehri.project.indexing.sink.Sink.SinkException when a sink errors
     * @throws eu.ehri.project.indexing.converter.Converter.ConverterException when a converter errors
     */
    public void run() throws Source.SourceException, Sink.SinkException, Converter.ConverterException {
        try {
            for (S item : source.iterable()) {
                for (E out : converter.convert(item)) {
                    writer.write(out);
                }
            }
        } finally {
            source.close();
            writer.close();
        }
    }
}
