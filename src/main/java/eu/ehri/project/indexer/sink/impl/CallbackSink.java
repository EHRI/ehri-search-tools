package eu.ehri.project.indexer.sink.impl;

import com.google.common.collect.Lists;
import eu.ehri.project.indexer.sink.Sink;

import java.util.List;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 *         <p/>
 *         Run a function on write and close.
 */
public class CallbackSink<T> implements Sink<T> {

    public static interface Callback<T> {
        public void call(T t);

        public void finish();
    }

    private final List<Callback<T>> callbacks;

    public CallbackSink(Callback<T>... callback) {
        callbacks = Lists.newArrayList(callback);
    }

    public CallbackSink(List<Callback<T>> callbacks) {
        this.callbacks = Lists.newArrayList(callbacks);
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
