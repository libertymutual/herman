/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.libertymutualgroup.herman.cli;

import com.amazonaws.regions.Regions;
import com.libertymutualgroup.herman.cli.command.ECRLoginCommand;
import com.libertymutualgroup.herman.cli.command.ECRRepoCreateCommand;
import com.libertymutualgroup.herman.cli.command.ECRRepoTrimCommand;
import com.libertymutualgroup.herman.cli.command.ECSPushCommand;
import com.libertymutualgroup.herman.logging.SysoutLogger;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

import static picocli.CommandLine.Help;
import static picocli.CommandLine.IVersionProvider;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.RunAll;

@Command(description = "Runs Herman the AWS Task Helper", name = "herman", mixinStandardHelpOptions = true, versionProvider = Cli.ManifestVersionProvider.class,
    subcommands = {
        ECSPushCommand.class,
        ECRRepoCreateCommand.class,
        ECRLoginCommand.class,
        ECRRepoTrimCommand.class
})
public class Cli implements Callable<Void> {
    private static final String CONFIG_BUCKET_TEMPLATE= "herman-configuration-<aws account #>-lts";

    private SysoutLogger logger = new SysoutLogger();

    public SysoutLogger getLogger() {
        return logger;
    }

    @Option(names = {"-r", "--region"}, description = "AWS Region to perform tasks", showDefaultValue = Help.Visibility.ALWAYS, arity = "1")
    private Regions region = Regions.US_EAST_1;

    @Option(names = {"-c", "--config"}, description = "Configuration S3 bucket name", showDefaultValue = Help.Visibility.ALWAYS)
    private String configurationBucket = CONFIG_BUCKET_TEMPLATE;

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new Cli());
        cmd.parseWithHandler(new RunAll(), args);
    }

    @Override public Void call() {
        return null;
    }

    public Regions getRegion() {
        return this.region;
    }

    public String getCustomConfigurationBucket() {
        String customConfigurationBucket = null;
        if (configurationBucket != null && !configurationBucket.equals(CONFIG_BUCKET_TEMPLATE)) {
            customConfigurationBucket = configurationBucket;
        }
        return customConfigurationBucket;
    }

    static class ManifestVersionProvider implements IVersionProvider {

        @Override public String[] getVersion() {
            String version = getClass().getPackage().getImplementationVersion();
            if (version != null) {
                return new String[] {getClass().getPackage().getImplementationVersion()};
            }
            return new String[] { "No version information found in manifest!"};
        }
    }
}
