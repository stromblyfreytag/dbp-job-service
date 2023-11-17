package com.trustwave.dbpjobservice.workflow.api.util;

public class BooleanStatisticsCollector {
    private String name;
    private int successCount = 0;
    private int failCount = 0;

    BooleanStatisticsCollector(String name) {
        this.name = name;
    }

    BooleanStatisticsCollector(BooleanStatisticsCollector other) {
        this.name = other.name;
        this.successCount = other.successCount;
        this.failCount = other.failCount;
    }

    public void markSuccess() {
        ++successCount;
    }

    public void markFailure() {
        ++failCount;
    }

    public void reset() {
        successCount = failCount = 0;
    }

    public String getName() {
        return name;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public long getFailCount() {
        return failCount;
    }

    public long getTotal() {
        return successCount + failCount;
    }

    public double getSuccessRate() {
        long total = getTotal();
        return total > 0 ? ((double) successCount) / total : 0.0;
    }

    public String toString() {
        long total = getTotal();
        double percent = getSuccessRate() * 100.0;
        if (total == 0) {
            return getName() + ": never called";
        }
        return String.format("%16s:\tsuccess rate %5.1f%%,  calls: %6d",
                getName(), percent, total);

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + failCount;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + successCount;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
        BooleanStatisticsCollector other = (BooleanStatisticsCollector) obj;
        if (name == null) {
			if (other.name != null) {
				return false;
			}
        }
        else if (!name.equals(other.name)) {
			return false;
		}

        return getSuccessRate() == other.getSuccessRate();
    }

}
