package com.libertymutualgroup.herman.util;

import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.logging.HermanLogger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class FileUtilTest {

    @Mock
    HermanLogger logger;

    FileUtil util;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        URL url = this.getClass().getResource("/fileUtil/claimletterdocsvc-BUILD76-R32098-config.zip");
        File configZip = new File(url.getFile());
        String rootPath = configZip.getParentFile().getPath();
        util = new FileUtil(rootPath, logger);
    }

    @Test
    public void existsInZip() throws IOException {
        Assert.assertTrue(util.fileExists("template.yml"));
        Assert.assertFalse(util.fileExists("template.json"));
    }

    @Test
    public void notExistsInZip() throws IOException {
        Assert.assertFalse(util.fileExists("template.json"));
    }

    @Test
    public void readFromZip() throws IOException {
        String yml = util.findFile("template.yml", false);
        Assert.assertNotNull(yml);
    }

    @Test
    public void readFromFilesystem() throws IOException {
        String yml = util.findFile("random-env.properties", false);
        Assert.assertNotNull(yml);
    }

    @Test(expected = AwsExecException.class)
    public void failOnNotExistsAndRequired() throws IOException {
        util.findFile("template.json", false);
    }

    @Test
    public void nullOnNotExistsAndOptionalInZip() throws IOException {
        String yml = util.findFile("template.json", true);
        Assert.assertNull(yml);
    }

    @Test
    public void readFileStreamFromZip() throws IOException {
        String envProps = util.findFile("dev-us-east-1.properties", true);
        Properties props = new Properties();
        if (props != null && envProps != null) {

            InputStream propStream = new ByteArrayInputStream(envProps.getBytes());
            props.load(propStream);

        }
    }

    @Test
    public void readPropsDirectly() throws IOException {
        String envProps = util.findFile("random-env.properties", true);
        Properties props = new Properties();
        if (props != null && envProps != null) {

            InputStream propStream = new ByteArrayInputStream(envProps.getBytes());
            props.load(propStream);

        }
    }

}
