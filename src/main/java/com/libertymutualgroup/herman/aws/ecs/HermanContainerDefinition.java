package com.libertymutualgroup.herman.aws.ecs;

import com.amazonaws.internal.SdkInternalList;
import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.HealthCheck;
import com.amazonaws.services.ecs.model.HostEntry;
import com.amazonaws.services.ecs.model.KeyValuePair;
import com.amazonaws.services.ecs.model.LinuxParameters;
import com.amazonaws.services.ecs.model.LogConfiguration;
import com.amazonaws.services.ecs.model.MountPoint;
import com.amazonaws.services.ecs.model.PortMapping;
import com.amazonaws.services.ecs.model.RepositoryCredentials;
import com.amazonaws.services.ecs.model.Ulimit;
import com.amazonaws.services.ecs.model.VolumeFrom;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HermanContainerDefinition {

    private String name;
    private String image;
    private RepositoryCredentials repositoryCredentials;
    private Integer cpu;
    private Integer memory;
    private Integer memoryReservation;
    private SdkInternalList<String> links;
    private SdkInternalList<PortMapping> portMappings;
    private Boolean essential;
    private SdkInternalList<String> entryPoint;
    private SdkInternalList<String> command;
    private List<EnvironmentVariableDefinition> environment;
    private SdkInternalList<MountPoint> mountPoints;
    private SdkInternalList<VolumeFrom> volumesFrom;
    private LinuxParameters linuxParameters;
    private String hostname;
    private String user;
    private String workingDirectory;
    private Boolean disableNetworking;
    private Boolean privileged;
    private Boolean readonlyRootFilesystem;
    private SdkInternalList<String> dnsServers;
    private SdkInternalList<String> dnsSearchDomains;
    private SdkInternalList<HostEntry> extraHosts;
    private SdkInternalList<String> dockerSecurityOptions;
    private Map<String, String> dockerLabels;
    private SdkInternalList<Ulimit> ulimits;
    private LogConfiguration logConfiguration;
    private HealthCheck healthCheck;

    public ContainerDefinition toEcsContainerDefinition() {
        return new ContainerDefinition()
                .withCommand(this.command)
                .withCpu(this.cpu)
                .withDisableNetworking(this.disableNetworking)
                .withDnsSearchDomains(this.dnsSearchDomains)
                .withDnsServers(this.dnsServers)
                .withDockerLabels(this.dockerLabels)
                .withDockerSecurityOptions(this.dockerSecurityOptions)
                .withEntryPoint(this.entryPoint)
                .withEnvironment(this.environment.stream().map(
                        envVar -> new KeyValuePair().withName(envVar.getName()).withValue(envVar.getValue())
                ).collect(Collectors.toList()))
                .withEssential(this.essential)
                .withExtraHosts(this.extraHosts)
                .withHealthCheck(this.healthCheck)
                .withHostname(this.hostname)
                .withImage(this.image)
                .withLinks(this.links)
                .withLinuxParameters(this.linuxParameters)
                .withLogConfiguration(this.logConfiguration)
                .withMemory(this.memory)
                .withMemoryReservation(this.memoryReservation)
                .withMountPoints(this.mountPoints)
                .withName(this.name)
                .withPortMappings(this.portMappings)
                .withPrivileged(this.privileged)
                .withReadonlyRootFilesystem(this.readonlyRootFilesystem)
                .withRepositoryCredentials(this.repositoryCredentials)
                .withUlimits(this.ulimits)
                .withUser(this.user)
                .withVolumesFrom(this.volumesFrom)
                .withWorkingDirectory(this.workingDirectory);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public RepositoryCredentials getRepositoryCredentials() {
        return repositoryCredentials;
    }

    public void setRepositoryCredentials(RepositoryCredentials repositoryCredentials) {
        this.repositoryCredentials = repositoryCredentials;
    }

    public Integer getCpu() {
        return cpu;
    }

    public void setCpu(Integer cpu) {
        this.cpu = cpu;
    }

    public Integer getMemory() {
        return memory;
    }

    public void setMemory(Integer memory) {
        this.memory = memory;
    }

    public Integer getMemoryReservation() {
        return memoryReservation;
    }

    public void setMemoryReservation(Integer memoryReservation) {
        this.memoryReservation = memoryReservation;
    }

    public SdkInternalList<String> getLinks() {
        return links;
    }

    public void setLinks(SdkInternalList<String> links) {
        this.links = links;
    }

    public SdkInternalList<PortMapping> getPortMappings() {
        return portMappings;
    }

    public void setPortMappings(SdkInternalList<PortMapping> portMappings) {
        this.portMappings = portMappings;
    }

    public Boolean isEssential() {
        return essential;
    }

    public void setEssential(Boolean essential) {
        this.essential = essential;
    }

    public SdkInternalList<String> getEntryPoint() {
        return entryPoint;
    }

    public void setEntryPoint(SdkInternalList<String> entryPoint) {
        this.entryPoint = entryPoint;
    }

    public SdkInternalList<String> getCommand() {
        return command;
    }

    public void setCommand(SdkInternalList<String> command) {
        this.command = command;
    }

    public List<EnvironmentVariableDefinition> getEnvironment() {
        return environment;
    }

    public void setEnvironment(List<EnvironmentVariableDefinition> environment) {
        this.environment = environment;
    }

    public SdkInternalList<MountPoint> getMountPoints() {
        return mountPoints;
    }

    public void setMountPoints(SdkInternalList<MountPoint> mountPoints) {
        this.mountPoints = mountPoints;
    }

    public SdkInternalList<VolumeFrom> getVolumesFrom() {
        return volumesFrom;
    }

    public void setVolumesFrom(SdkInternalList<VolumeFrom> volumesFrom) {
        this.volumesFrom = volumesFrom;
    }

    public LinuxParameters getLinuxParameters() {
        return linuxParameters;
    }

    public void setLinuxParameters(LinuxParameters linuxParameters) {
        this.linuxParameters = linuxParameters;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public Boolean isDisableNetworking() {
        return disableNetworking;
    }

    public void setDisableNetworking(Boolean disableNetworking) {
        this.disableNetworking = disableNetworking;
    }

    public Boolean isPrivileged() {
        return privileged;
    }

    public void setPrivileged(Boolean privileged) {
        this.privileged = privileged;
    }

    public Boolean isReadonlyRootFilesystem() {
        return readonlyRootFilesystem;
    }

    public void setReadonlyRootFilesystem(Boolean readonlyRootFilesystem) {
        this.readonlyRootFilesystem = readonlyRootFilesystem;
    }

    public SdkInternalList<String> getDnsServers() {
        return dnsServers;
    }

    public void setDnsServers(SdkInternalList<String> dnsServers) {
        this.dnsServers = dnsServers;
    }

    public SdkInternalList<String> getDnsSearchDomains() {
        return dnsSearchDomains;
    }

    public void setDnsSearchDomains(SdkInternalList<String> dnsSearchDomains) {
        this.dnsSearchDomains = dnsSearchDomains;
    }

    public SdkInternalList<HostEntry> getExtraHosts() {
        return extraHosts;
    }

    public void setExtraHosts(SdkInternalList<HostEntry> extraHosts) {
        this.extraHosts = extraHosts;
    }

    public SdkInternalList<String> getDockerSecurityOptions() {
        return dockerSecurityOptions;
    }

    public void setDockerSecurityOptions(SdkInternalList<String> dockerSecurityOptions) {
        this.dockerSecurityOptions = dockerSecurityOptions;
    }

    public Map<String, String> getDockerLabels() {
        return dockerLabels;
    }

    public void setDockerLabels(Map<String, String> dockerLabels) {
        this.dockerLabels = dockerLabels;
    }

    public SdkInternalList<Ulimit> getUlimits() {
        return ulimits;
    }

    public void setUlimits(SdkInternalList<Ulimit> ulimits) {
        this.ulimits = ulimits;
    }

    public LogConfiguration getLogConfiguration() {
        return logConfiguration;
    }

    public void setLogConfiguration(LogConfiguration logConfiguration) {
        this.logConfiguration = logConfiguration;
    }

    public HealthCheck getHealthCheck() {
        return healthCheck;
    }

    public void setHealthCheck(HealthCheck healthCheck) {
        this.healthCheck = healthCheck;
    }
}
