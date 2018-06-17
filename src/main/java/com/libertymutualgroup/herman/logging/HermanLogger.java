package com.libertymutualgroup.herman.logging;

public interface HermanLogger {
    public void addLogEntry(String logEntry);
    public void addErrorLogEntry(String errorLogEntry);
    public void addErrorLogEntry(String errorLogEntry, Throwable exception);
}
