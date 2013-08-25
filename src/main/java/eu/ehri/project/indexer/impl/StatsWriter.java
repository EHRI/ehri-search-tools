package eu.ehri.project.indexer.impl;

import com.google.common.collect.Lists;
import eu.ehri.project.indexer.Indexer;
import eu.ehri.project.indexer.Writer;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class StatsWriter<T> implements Writer<T> {

    private final Indexer.Stats stats = new Indexer.Stats();
    private final PrintWriter pw;

    public StatsWriter(PrintStream out) {
        this.pw = new PrintWriter(out);
    }

    public void write(T t) {
        stats.itemCount++;
    }

    public void close() {
        // Abusing close here!
        stats.printReport(pw);
        pw.flush();
    }
}
