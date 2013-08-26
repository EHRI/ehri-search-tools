package eu.ehri.project.indexer.sink.impl;

import eu.ehri.project.indexer.sink.Sink;
import org.codehaus.jackson.JsonNode;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class StatsSink implements Sink<JsonNode> {

    private final Stats stats = new Stats();
    private final PrintStream pw;
    private final boolean vv;

    public StatsSink(OutputStream out, boolean veryVerbose) {
        this.pw = new PrintStream(new BufferedOutputStream(out));
        this.vv = veryVerbose;

    }

    public StatsSink(PrintStream out) {
        this(out, false);
    }

    public void write(JsonNode t) {
        stats.incrementCount();
        if (vv) {
            pw.println(t.path("type").asText() + " : " + t.path("id").asText());
        }
    }

    public void close() {
        // Abusing finish here!
        stats.printReport(pw);
        pw.flush();
    }
}
