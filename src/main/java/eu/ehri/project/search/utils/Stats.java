package eu.ehri.project.search.utils;

import java.io.PrintStream;

/**
 * A class for holding interesting stats.
 */
public class Stats {
    private final long startTime = System.nanoTime();

    private int itemCount = 0;

    public void incrementCount() {
        itemCount++;
    }

    public void printReport(PrintStream pw) {
        long endTime = System.nanoTime();
        double duration = ((double) (endTime - startTime)) / 1000000000.0;

        pw.println("Indexing completed in " + duration);
        pw.println("Items indexed: " + itemCount);
        pw.println("Items per second: " + (itemCount / duration));
    }
}
