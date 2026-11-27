package moa.core;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class SizeOfReportingTest {
    @Test
    public void reportsSameSizeAsUnboundedTraversalForSharedObjects() {
        assumeTrue(SizeOf.isPresent());
        Object[] graph = new Object[4];
        double[] shared = new double[16];
        graph[0] = shared;
        graph[1] = shared;
        graph[2] = graph;
        graph[3] = new double[16];

        long expected = SizeOf.fullSizeOf(graph);
        assertTrue(expected > 0);
        assertEquals(expected, SizeOf.measureForReporting(() -> SizeOf.fullSizeOf(graph)));
    }

    @Test
    public void expiredDeadlineIsSharedAndClearedAfterReporting() {
        AtomicInteger measurements = new AtomicInteger();
        long budget = TimeUnit.MILLISECONDS.toNanos(1);
        long reported = SizeOf.withReportingBudget(budget, () -> {
            long start = System.nanoTime();
            while (System.nanoTime() - start < budget) {
                Thread.yield();
            }
            return SizeOf.withReportingBudget(TimeUnit.SECONDS.toNanos(10),
                    () -> SizeOf.measureForReporting(measurements::incrementAndGet));
        });
        assertEquals(-1L, reported);
        assertEquals(0, measurements.get());
        assertEquals(1L, SizeOf.measureForReporting(measurements::incrementAndGet));
    }

    @Test(timeout = 5000)
    public void stopsMeasuringALargeGraphAtTheReportingDeadline() {
        assumeTrue(SizeOf.isPresent());
        // Warm up the agent, then build a graph which cannot fit in a 5 ms traversal.
        assertTrue(SizeOf.fullSizeOf(new Object[] {new Object()}) > 0);
        Object graph = new Object();
        for (int i = 0; i < 1_000_000; i++) {
            graph = new Object[] {graph};
        }
        Object root = graph;
        assertEquals(-1L, (long) SizeOf.withReportingBudget(TimeUnit.MILLISECONDS.toNanos(5),
                () -> SizeOf.measureForReporting(() -> SizeOf.fullSizeOf(root))));
    }

    @Test
    public void doesNotReturnAPartialCompositeSizeAfterCancellation() {
        assumeTrue(SizeOf.isPresent());
        long reported = SizeOf.withReportingBudget(TimeUnit.SECONDS.toNanos(10),
                () -> SizeOf.measureForReporting(() -> {
                    long initialBytes = SizeOf.fullSizeOf(new Object());
                    Thread.currentThread().interrupt();
                    try {
                        return initialBytes + SizeOf.fullSizeOf(new int[32]);
                    } finally {
                        Thread.interrupted();
                    }
                }));
        assertEquals(-1L, reported);
        assertTrue(SizeOf.fullSizeOf(new Object()) > 0);
    }
}
