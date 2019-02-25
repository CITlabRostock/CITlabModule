/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

import eu.transkribus.interfaces.IModule;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.fail;

/**
 * @author gundram
 */
public class IModuleTest {
    public static void testUsage(IModule instance) {
        System.out.println("usage");
        String result = instance.usage();
        if (result == null || result.isEmpty()) {
            fail("usage string is '" + result + "'");
        }
    }

    public static void testGetToolName(IModule instance) {
        System.out.println("getToolName");
        String result = instance.getToolName();
        if (result == null || result.isEmpty()) {
            fail("tool name is '" + result + "'");
        }
    }

    public static void testGetVersion(IModule instance) {
        System.out.println("getVersion");
        String result = instance.getVersion();
        if (!result.matches("[0-9]+\\.[0-9]+\\.[0-9]+")) {
            fail("Version does not fit to regex \"[0-9]+\\\\.[0-9]+\\\\.[0-9]+\".");

        }
    }

    @Test
    public void testVersion() {
        String softwareVersion = MetadataUtil.getSoftwareVersion();
        File f = new File("pom.xml");
        Pattern p = Pattern.compile(".*<version>([0-9.]+)(-SNAPSHOT)?</version>.*");
        List<String> strings = FileUtil.readLines(f);
        strings.sort((o1, o2) -> Integer.compare(o1.indexOf('<'), o2.indexOf('<')));
        for (String s : strings) {
            Matcher matcher = p.matcher(s);
            if (matcher.matches()) {
                String group = matcher.group(1);
                Assert.assertEquals("saved version does not fit to version in pom.xml", group, MetadataUtil.getSoftwareVersion());
                break;
            }
        }

    }

    public static void testGetProvider(IModule instance) {
        System.out.println("getProvider");
        String result = instance.getProvider();
        if (result == null || result.isEmpty()) {
            fail("provider is '" + result + "'");
        }
        if (!result.contains("CITlab")) {
            fail("provider does not contain CITlab");
        }
    }

}
