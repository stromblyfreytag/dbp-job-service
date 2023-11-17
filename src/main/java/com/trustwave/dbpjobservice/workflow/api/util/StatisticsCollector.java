package com.trustwave.dbpjobservice.workflow.api.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StatisticsCollector {
    private static Logger logger = LogManager.getLogger(StatisticsCollector.class);
    private static StatisticsCollector instance = new StatisticsCollector();
    private List<CollectorEntry> collectors = new ArrayList<>();
    private long outputIntervalSeconds = 0;
    private Timer timer = null;
    private int resetAfterOutputCount = 10;
    private int outputCount = 0;

    public static StatisticsCollector getInstance() {
        return instance;
    }

    public static StatisticsCollector createInstance() {
        return instance;
    }

    public void init() {
        if (outputIntervalSeconds > 0 && timer == null) {
            timer = new Timer(true);
            timer.schedule(new TimerTask() {
                public void run() {
                    output();
                }
            }, outputIntervalSeconds * 1000, outputIntervalSeconds * 1000);
        }
    }

    public BooleanStatisticsCollector createBooleanCollector(String name) {
        BooleanStatisticsCollector collector =
                new BooleanStatisticsCollector(name);
        collectors.add(new CollectorEntry(collector));
        return collector;
    }

    public void output() {
        Logger llogger = logger;
        if (llogger != null && llogger.isDebugEnabled()) {
            StringBuffer sb = new StringBuffer();
            for (CollectorEntry ce : collectors) {
                if (ce.hasChanged()) {
                    ce.output(sb);
                }
            }
            if (sb.length() > 0) {
                llogger.debug(sb.toString());
            }
        }

        if (++outputCount >= resetAfterOutputCount) {
            reset();
            outputCount = 0;
        }
    }

    public void reset() {
        for (CollectorEntry ce : collectors) {
            ce.collector.reset();
            if (ce.lastOutput != null) {
                ce.lastOutput.reset();
            }
        }
    }

    public void setOutputIntervalSeconds(long outputInterval) {
        this.outputIntervalSeconds = outputInterval;
    }

    private static class CollectorEntry {
        BooleanStatisticsCollector collector;
        BooleanStatisticsCollector lastOutput = null;

        public CollectorEntry(BooleanStatisticsCollector collector) {
            this.collector = collector;
        }

        public boolean hasChanged() {
            return !collector.equals(lastOutput);
        }

        public void output(StringBuffer sb) {
            sb.append("\r\n");
            sb.append(collector.toString());
            lastOutput = new BooleanStatisticsCollector(collector);
        }
    }

}
