/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

/**
 *
 * @author gundram
 */
public class PropertyUtil {

    public static final String SEPERATOR = "/";

    public static boolean hasProperty(String[] props, String property) {
        return getIndexOfKey(props, property) >= 0;
    }

    public static String[] merge(String[] propsPrimary, String[] propsSecondary) {
        if (propsPrimary == null) {
            return propsSecondary;
        }
        if (propsSecondary == null) {
            return propsPrimary;
        }
        for (String key : getKeys(propsSecondary)) {
            if (!hasProperty(propsPrimary, key)) {
                propsPrimary = setProperty(propsPrimary, key, getProperty(propsSecondary, key));
            }
        }
        return propsPrimary;
    }

    private static String[] getKeys(String[] props) {
        if (props == null || props.length == 0) {
            return new String[0];
        }
        String[] res = new String[props.length / 2];
        for (int i = 0; i < res.length; i++) {
            res[i] = props[i * 2];
        }
        return res;
    }

    public static String[] getSubTreeProperty(String[] props, String prefix) {
        if (props == null) {
            return null;
        }
        if (!prefix.endsWith(SEPERATOR)) {
            prefix += SEPERATOR;
        }
        String[] res = new String[0];
        for (int i = 0; i < props.length; i += 2) {
            String prop = props[i];
            if (prop.startsWith(prefix)) {
                res = PropertyUtil.setProperty(res, prop.substring(prefix.length()), props[i + 1]);
            }
        }
        return res;
    }

    public static String[] setSubTreeProperty(String[] propsRoot, String[] propsLeave, String prefix) {
        if (propsLeave == null) {
            return propsRoot;
        }
        if (prefix == null || prefix.isEmpty()) {
            throw new RuntimeException("prefix is not set properly");
        }
        if (!prefix.endsWith(SEPERATOR)) {
            prefix += SEPERATOR;
        }
        for (int i = 0; i < propsLeave.length; i += 2) {
            propsRoot = PropertyUtil.setProperty(propsRoot, prefix + propsLeave[i], propsLeave[i + 1]);
        }
        return propsRoot;
    }

    public static String getProperty(String[] props, String property) {
        int indexOfValue = getIndexOfValue(props, property);
        return indexOfValue < 0 ? null : props[indexOfValue];
    }

    public static String getProperty(String[] props, String property, Object defaultValue) {
        int indexOfValue = getIndexOfValue(props, property);
        return indexOfValue < 0 ? defaultValue == null ? null : defaultValue.toString() : props[indexOfValue];
    }

    public static boolean isPropertyTrue(String[] props, String property) {
        String property1 = getProperty(props, property);
        return property1 != null && property1.toLowerCase().equals("true");
    }

    public static boolean isProperty(String[] props, String property, String... values) {
        String property1 = getProperty(props, property);
        if (property1 == null) {
            return false;
        }
        for (String value : values) {
            if (value.equals(property1)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPropertyFalse(String[] props, String property) {
        String property1 = getProperty(props, property);
        return property1 != null && property1.toLowerCase().equals("false");
    }

    public static String[] copy(String[] props) {
        if (props == null) {
            return null;
        }
        String[] res = new String[props.length];
        System.arraycopy(props, 0, res, 0, res.length);
        return res;
    }

    private static int getIndexOfKey(String[] props, String property) {
        if (props == null) {
            return -2;
        }
        for (int i = 0; i < props.length; i += 2) {
            if (props[i].equals(property)) {
                return i;
            };
        }
        return -2;
    }

    private static int getIndexOfValue(String[] props, String property) {
        return getIndexOfKey(props, property) + 1;
    }

    public static String[] setProperty(String[] props, String key, Object value) {
        int indexOfValue = getIndexOfKey(props, key);
        if (indexOfValue >= 0) {
            if (value != null) {//set
                props[indexOfValue + 1] = value.toString();
                return props;
            }//delete
            String[] res = new String[props.length - 2];
            System.arraycopy(props, 0, res, 0, indexOfValue);
            System.arraycopy(props, indexOfValue + 2, res, indexOfValue, res.length - indexOfValue);
            return res;
        }
        if (value != null) {//add
            if (props == null) {
                props = new String[0];
            }
            String[] res = new String[props.length + 2];
            System.arraycopy(props, 0, res, 0, props.length);
            res[props.length] = key;
            res[props.length + 1] = value.toString();
            return res;
        }
        return props;//nothing to do
    }
}
