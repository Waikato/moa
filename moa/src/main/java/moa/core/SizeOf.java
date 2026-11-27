/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */

/*
 * SizeOf.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */
package moa.core;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import sizeof.agent.Filter;
import sizeof.agent.SizeOfAgent;

/**
 * Helper class for <a href="http://www.jroller.com/maxim/entry/again_about_determining_size_of" target="_blank">Maxim Zakharenkov's SizeOf agent</a>.
 *
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class SizeOf {

    private static final String TIMEOUT_PROPERTY = "moa.modelSizeTimeoutSeconds";
    private static final long DEFAULT_TIMEOUT_SECONDS = 30;
    private static final ThreadLocal<ReportingBudget> REPORTING_BUDGET = new ThreadLocal<>();
    private static final MeasurementAborted ABORTED = new MeasurementAborted();

    /** whether the agent is present. */
    protected static Boolean m_Present;

    /**
     * Checks whteher the agent is present.
     *
     * @return true if the agent is present, false otherwise
     */
    protected static synchronized boolean isPresent() {
        if (m_Present == null) {
            try {
                // An instrumented Java object has a positive shallow size.
                m_Present = SizeOfAgent.sizeOf(new Object()) > 0;
            } catch (Throwable t) {
                m_Present = false;
            }
        }

        return m_Present;
    }

    /**
     * Returns the size of the object.
     *
     * @param o	the object to get the size for
     * @return the size of the object, or if the agent isn't present -1
     */
    public static long sizeOf(Object o) {
        if (isPresent()) {
            return SizeOfAgent.sizeOf(o);
        } else {
            return -1;
        }
    }

    /**
     * Returns the full size of the object.
     *
     * @param o	the object to get the size for
     * @return the size of the object, or if the agent isn't present -1
     */
    public static long fullSizeOf(Object o) {
        if (!isPresent()) {
            return -1;
        }
        ReportingBudget budget = REPORTING_BUDGET.get();
        if (budget == null) {
            return SizeOfAgent.fullSizeOf(o);
        }
        budget.check();
        long bytes;
        try {
            bytes = SizeOfAgent.fullSizeOf(o, budget);
        } catch (OutOfMemoryError exhausted) {
            // The agent's temporary visited set can be larger than the live model.
            throw budget.abort();
        }
        budget.check();
        return bytes;
    }

    /**
     * Shares one cooperative size deadline across a reporting snapshot, including
     * calls for sublearners. Direct sizing during training keeps its existing behavior.
     * The default is 30 seconds. Set {@code -Dmoa.modelSizeTimeoutSeconds=0}
     * for unlimited reporting.
     */
    public static <T> T withReportingBudget(Supplier<T> operation) {
        long seconds = Long.getLong(TIMEOUT_PROPERTY, DEFAULT_TIMEOUT_SECONDS);
        return withReportingBudget(TimeUnit.SECONDS.toNanos(seconds), operation);
    }

    static <T> T withReportingBudget(long timeoutNanos, Supplier<T> operation) {
        if (timeoutNanos <= 0 || REPORTING_BUDGET.get() != null) {
            return operation.get();
        }
        REPORTING_BUDGET.set(new ReportingBudget(timeoutNanos));
        try {
            return operation.get();
        } finally {
            REPORTING_BUDGET.remove();
        }
    }

    /** Returns the full reported size, or -1 if the measurement was aborted. */
    public static long measureForReporting(LongSupplier measurement) {
        return withReportingBudget(() -> {
            ReportingBudget budget = REPORTING_BUDGET.get();
            if (budget == null) {
                return measurement.getAsLong();
            }
            try {
                budget.check();
                long bytes = measurement.getAsLong();
                budget.check();
                return bytes;
            } catch (MeasurementAborted aborted) {
                return -1L;
            }
        });
    }

    private static final class ReportingBudget implements Filter {
        private final long started = System.nanoTime();
        private final long timeoutNanos;
        private int visitedReferences;
        private boolean aborted;

        ReportingBudget(long timeoutNanos) {
            this.timeoutNanos = timeoutNanos;
        }

        void check() {
            if (aborted || System.nanoTime() - started >= timeoutNanos
                    || Thread.currentThread().isInterrupted()) {
                throw abort();
            }
        }

        MeasurementAborted abort() {
            aborted = true;
            return ABORTED;
        }

        @Override
        public boolean skipObject(Object object) {
            if ((++visitedReferences & 255) == 0) {
                check();
            }
            return false;
        }

        @Override public boolean skipSuperClass(Class type) { return false; }
        @Override public boolean skipField(Field field) { return false; }
    }

    private static final class MeasurementAborted extends RuntimeException {
        private static final long serialVersionUID = 1L;

        MeasurementAborted() {
            super(null, null, false, false);
        }
    }
}
