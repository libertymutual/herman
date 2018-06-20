package com.libertymutualgroup.herman.logging;

public class SysoutLogger implements HermanLogger {

    @Override
    public void addLogEntry(String logEntry) {
        System.out.println(logEntry);
    }

    @Override
    public void addErrorLogEntry(String errorLogEntry) {
        System.out.println(errorLogEntry);
    }

    @Override
    public void addErrorLogEntry(String errorLogEntry, Throwable exception) {
        System.out.println(errorLogEntry);
        System.out.println(exception.getMessage());
    }

}
