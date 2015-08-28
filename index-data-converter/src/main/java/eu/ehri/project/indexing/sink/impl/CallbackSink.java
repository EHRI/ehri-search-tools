package eu.ehri.project.indexing.sink.impl;

import com.google.common.collect.ImmutableList;
import eu.ehri.project.indexing.sink.Sink;

import java.util.List;

/**
 * Run a function on write and close.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class CallbackSink<T> implements Sink<T> {

    public interface Callback<T> {
        void call(T t);

        void finish();
    }

    private final List<Callback<T>> callbacks;

    public CallbackSink(Callback<T> callback) {
        this(ImmutableList.of(callback));
    }

    public CallbackSink(final List<Callback<T>> callbacks) {
        this.callbacks = callbacks;
    }

    public void write(T t) {
        for (Callback<T> cb : callbacks) {
            cb.call(t);
        }
    }

    public void close() {
        for (Callback<T> cb : callbacks) {
            cb.finish();
        }
    }
}
