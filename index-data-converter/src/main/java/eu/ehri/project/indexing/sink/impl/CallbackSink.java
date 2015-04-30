package eu.ehri.project.indexing.sink.impl;

import com.google.common.collect.ImmutableList;
import eu.ehri.project.indexing.sink.Sink;

import java.util.List;

/**
 * Run a function on write and finish.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class CallbackSink<T> implements Sink<T> {

    public static interface Callback<T> {
        public void call(T t);

        public void finish();
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

    public void finish() {
        for (Callback<T> cb : callbacks) {
            cb.finish();
        }
    }
}
