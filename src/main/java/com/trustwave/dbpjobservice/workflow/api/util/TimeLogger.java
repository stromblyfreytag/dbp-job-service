package com.trustwave.dbpjobservice.workflow.api.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.Logger;

/**
 * <p>TimeLogger class provides simple way to measure and log execution time
 * for some piece of code. It provide both last elapsed time measurement
 * and average time over specified number of cycles.</p>
 * <p>Time logging is performed only when logger has DEBUG level.
 * </p>
 * <br/>
 * </p>
 * <p>Typical usage:</p>
 * <pre>
 *    TimeLogger tlog = new TimeLogger(logger);
 *    ...
 *    tlog.log("elapsed time for %s", obj.getName()) ;
 *    ...
 *    tlog.log("elapsed time after finish for %s", obj.getName()) ;
 * </pre>
 * <p>Log output:<br/>
 * elapsed time for asset1: 23ms<br/>
 * elapsed time after finish for asset1: 69ms
 * </p>
 * <p>The <code>log()</code> call does not stop TimeLogger timer;
 * so the second log call will output accumulated time since tlog creation.
 * </p>
 *
 * <pre>
 *    // for getting average time you need to provide unique name
 *    // and maximum number of cycles:
 *    TimeLogger tlog = new TimeLogger(logger, "getTaskInfo", 20, false);
 *    ...
 *    tlog.restartTimer(); // re-start timer, needed when we are in a loop
 *    ...
 *    tlog.log( "getTaskInfo()" ) ;
 *    ...
 *    tlog.log( "getTaskInfo()+saveTaskInfo()" ) ;
 * </pre>
 * <p>Log output:<br/>
 * getTaskInfo(): 154, 112ms<br/>
 * getTaskInfo()+saveTaskInfo(): 254, 182ms<br/>
 * </p>
 * <p>The last figure is average time over last 20 cycles (or less)</p>
 * <br/>
 *
 * <p>The class is not thread-safe;
 * TimeLogger object is supposed to be used on stack only</p>
 *
 * @author vlad
 */
public class TimeLogger {
    //    Averages should be kept in some static area - to gather info
    // from multiple instances of TymeLogger objects
    //    TimeLogger objects may be created by different threads;
    // creation and updating of average must be synchronized.
    //    Average from different threads is OK - taking into account
    // nature and goal of TimeLogger
    private static HashMap<String, AverageTime> averages =
            new HashMap<String, AverageTime>();
    private Logger logger;
    private long tStart = 0;
    private long tElapsed = 0;
    private String nameForAverage;
    private int maxNumberOfCycles;

    public TimeLogger(Logger logger, String name, int numberOfCyclesInAverage) {
        this.logger = logger;
        this.nameForAverage = name;
        this.maxNumberOfCycles = numberOfCyclesInAverage;
        restartTimer();
    }

    public TimeLogger(Logger logger) {
        this(logger, null, 0);
    }

    //--------------------------------------------------------------------------
    //--------------------------------------------------------------------------

    private static synchronized AverageTime findOrCreateAverage(
            String name, int maxNumberOfCycles) {
        AverageTime at = averages.get(name);
        if (at == null) {
            at = new AverageTime(maxNumberOfCycles);
            averages.put(name, at);
        }
        return at;
    }

    public void restartTimer() {
        tStart = getCurrentTime();
        tElapsed = 0;
    }

    public boolean isRunning() {
        return tStart != 0;
    }

    /**
     * This call should be used if you want to log times later,
     * not at the point of time measurement.
     */
    public void stopTimer() {
        if (isRunning()) {
            tElapsed = getCurrentTime() - tStart;
            tStart = 0;
        }
    }

    public long getElapsedTime() {
        if (isRunning()) {
            return getCurrentTime() - tStart;
        }
        return tElapsed;
    }

    public void log(String format, Object... args) {
        if (logger.isDebugEnabled()) {
            long elapsedTime = getElapsedTime();
            AverageTime averageTime = getAverageTime(format);
            if (averageTime != null && isRunning()) {
                averageTime.add(elapsedTime);
            }
            String msg = (args.length > 0 ? String.format(format, args) : format);
            logger.debug(msg + ": " + elapsedTime
                    + (averageTime != null ? ", " + averageTime.getAverage() : "")
                    + "ms");
        }
    }

    private AverageTime getAverageTime(String format) {
        AverageTime averageTime = null;
        if (nameForAverage != null && maxNumberOfCycles > 1) {
            // We assume that every log call for the same time logger object
            // will specify different message (format), and use this fact
            // to distinguish between different averages for the same time logger:
            averageTime = findOrCreateAverage(
                    nameForAverage + format, maxNumberOfCycles);
        }
        return averageTime;
    }

    private long getCurrentTime() {
        if (logger.isDebugEnabled() || isRunning()) {
            return getCurrentTimeMillis();
        }
        return 0;
    }

    long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    static class AverageTime {
        private long totalTime = 0;
        private LinkedList<Long> times = new LinkedList<Long>();
        private int numberOfCycles = 0;
        private int maxNumberOfCycles;
        private long average = 0;

        public AverageTime(int maxNumberOfCycles) {
            this.maxNumberOfCycles = maxNumberOfCycles;
        }

        public synchronized void add(long tElapsed) {
            if (numberOfCycles < maxNumberOfCycles) {
                numberOfCycles++;
            }
            else {
                long oldestTime = times.removeFirst();
                totalTime -= oldestTime;
            }
            times.add(tElapsed);
            totalTime += tElapsed;
            average = totalTime / numberOfCycles;
        }

        public long getAverage() {
            return average;
        }

        // these calls are used in unit tests only
        long getTotalTime() {
            return totalTime;
        }

        List<Long> getTimes() {
            return new ArrayList<Long>(times);
        }

        int getNumberOfCycles() {
            return numberOfCycles;
        }
    }
}
