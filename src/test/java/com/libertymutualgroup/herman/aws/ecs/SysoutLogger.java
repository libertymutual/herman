package com.libertymutualgroup.herman.aws.ecs;

import com.atlassian.bamboo.build.LogEntry;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.logger.LogInterceptorStack;
import com.atlassian.bamboo.build.logger.LogMutatorStack;
import java.util.List;

public class SysoutLogger implements BuildLogger {

    @Override
    public String addBuildLogEntry(LogEntry arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String addBuildLogEntry(String arg0) {
        System.out.println(arg0);
        return arg0;
    }

    @Override
    public String addBuildLogHeader(String arg0, boolean arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String addErrorLogEntry(LogEntry arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String addErrorLogEntry(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addErrorLogEntry(String arg0, Throwable arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void clearBuildLog() {
        // TODO Auto-generated method stub

    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public List<LogEntry> getBuildLog() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<LogEntry> getErrorLog() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LogInterceptorStack getInterceptorStack() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<LogEntry> getLastNLogEntries(int arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LogMutatorStack getMutatorStack() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getStringErrorLogs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getTimeOfLastLog() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void stopStreamingBuildLogs() {
        // TODO Auto-generated method stub

    }

}
