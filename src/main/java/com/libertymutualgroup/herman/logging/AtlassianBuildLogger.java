package com.libertymutualgroup.herman.logging;

import com.atlassian.bamboo.build.logger.BuildLogger;

public class AtlassianBuildLogger implements HermanLogger {
    private BuildLogger logger;

    public AtlassianBuildLogger(BuildLogger logger) {
        this.logger = logger;
    }

    @Override
    public void addLogEntry(String logEntry) {
        this.logger.addBuildLogEntry(logEntry);
    }

    @Override
    public void addErrorLogEntry(String errorLogEntry) {
        this.logger.addErrorLogEntry(errorLogEntry);
    }

    @Override
    public void addErrorLogEntry(String errorLogEntry, Throwable exception) {
        this.logger.addErrorLogEntry(errorLogEntry, exception);
    }
}
