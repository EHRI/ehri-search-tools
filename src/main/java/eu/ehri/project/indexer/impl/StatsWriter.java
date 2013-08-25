package eu.ehri.project.indexer.impl;

import eu.ehri.project.indexer.Writer;
import org.codehaus.jackson.JsonNode;

import java.io.*;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class StatsWriter implements Writer<JsonNode> {

    private final Stats stats = new Stats();
    private final PrintStream pw;
    public final boolean vv;

    public StatsWriter(OutputStream out, boolean veryVerbose) {
        this.pw = new PrintStream(new BufferedOutputStream(out));
        this.vv = veryVerbose;

    }

    public StatsWriter(PrintStream out) {
        this(out, false);
    }

    public void write(JsonNode t) {
        stats.incrementCount();
        if (vv) {
            pw.println(t.path("type").asText() + " -> "
                    + t.path("itemId").asText() + " -> "
                    + t.path("id").asText());
        }
    }

    public void close() {
        // Abusing close here!
        stats.printReport(pw);
        pw.flush();
    }
}
