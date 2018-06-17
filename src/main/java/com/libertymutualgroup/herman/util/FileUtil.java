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
package com.libertymutualgroup.herman.util;

import com.amazonaws.util.IOUtils;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.logging.HermanLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    private String rootPath;
    private HermanLogger buildLogger;

    public FileUtil(String rootPath, HermanLogger buildLogger) {
        this.rootPath = rootPath;
        this.buildLogger = buildLogger;
    }

    public boolean fileExists(String path) {
        boolean fileExists = new File(rootPath + File.separator + path).exists();
        if (!fileExists) {
            fileExists = findFileInZip(path) != null;
        }
        return fileExists;

    }

    public String findFile(String filename, boolean isOptional) {
        String result = findFile(filename);
        if (result == null) {
            result = findFileInZip(filename);
        }
        if (result == null && !isOptional) {
            throw new AwsExecException(
                String.format("Error finding file %s in root path or in **-config.zip", filename));
        }
        return result;
    }

    public FileInputStream findfileAsInputStream(String filename) {
        String path = rootPath + File.separator + filename;
        File file = new File(path);
        if (file.exists()) {
            try {
                return new FileInputStream(file);
            } catch (IOException e) {
                LOGGER.debug("Error finding file: " + path, e); //NOSONAR
                buildLogger.addLogEntry(String.format("Error finding file %s: %s", path, e.getMessage()));
            }
        }
        return null;
    }

    private String findFile(String filename) {
        String path = rootPath + File.separator + filename;
        File template = new File(path);
        if (template.exists()) {
            FileInputStream streamToParse;
            try {
                streamToParse = new FileInputStream(template);
                return IOUtils.toString(streamToParse);
            } catch (IOException e) {
                LOGGER.debug("Error finding file: " + path, e);
                buildLogger.addLogEntry(String.format("Error finding file %s: %s", path, e.getMessage()));
            }
        }
        return null;
    }

    private String findFileInZip(String filename) {
        File zip = findZip();
        String result;
        if (zip != null) {
            try (ZipFile zipFile = new ZipFile(zip)) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.getName().contentEquals(filename)) {
                        InputStream stream = zipFile.getInputStream(entry);
                        result = IOUtils.toString(stream);
                        return result;
                    }
                }
            } catch (IOException e) {
                LOGGER.debug("Error finding file: " + filename, e);
                buildLogger
                    .addLogEntry(String.format("Error reading file in zip %s: %s", filename, e.getMessage()));

            }
        }
        return null;

    }

    private File findZip() {
        File folder = new File(rootPath);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile() && listOfFiles[i].getName().endsWith("-config.zip")) {
                return listOfFiles[i];
            }
        }
        return null;
    }
}
