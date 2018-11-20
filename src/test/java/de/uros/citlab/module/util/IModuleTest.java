/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

import eu.transkribus.interfaces.IModule;
import static org.junit.Assert.fail;

/**
 *
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
