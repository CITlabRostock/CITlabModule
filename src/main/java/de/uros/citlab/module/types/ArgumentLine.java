/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.types;

import de.uros.citlab.module.util.PropertyUtil;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gundram
 */
public class ArgumentLine {

    HashMap<String, String> map = new HashMap<>();
    private boolean help = false;

    public void addArgument(Object key, Object value) {
        if (key == null) {
            throw new RuntimeException("key have to be != null.");
        }
        String sStr = key.toString();
        if (!sStr.startsWith("-")) {
            sStr = "-" + sStr;
        }
        if (value == null) {
            map.remove(sStr);
        } else {
            map.put(sStr, value.toString());
        }
    }

    private void addArgumentPair(StringBuilder sb, String key, String value) {
        if (!key.startsWith("-")) {
            sb.append('-');
        }
        sb.append(key);
        sb.append(' ');
        sb.append(value);
        sb.append(' ');
    }

    public String[] getArgs() {
        return getString().split(" ");
    }

    public void setHelp() {
        help = true;
    }

    private String getString() {
        StringBuilder sb = new StringBuilder();
        for (String key : map.keySet()) {
            addArgumentPair(sb, key, map.get(key));
        }
        if (help) {
            sb.append("--help ");
        }
        return sb.toString().trim();
    }

    @Override
    public String toString() {
        return getString();
    }

    public static Set<String> getKeys() {
        HashSet<String> res = new HashSet<>();
        Field[] declaredFields = Key.class.getFields();
        for (Field declaredField : declaredFields) {
            if (declaredField.getType().equals(String.class)) {
                try {
                    res.add(declaredField.get(declaredField).toString());
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    Logger.getLogger(ArgumentLine.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return res;
    }

    public static String[] getPropertiesFromArgs(String[] remainingArgumentList) {
        return getPropertiesFromArgs(remainingArgumentList, null);
    }

    public static String[] getPropertiesFromArgs(String[] remainingArgumentList, String[] props) {
        Set<String> keys = getKeys();
        for (int i = 1; i < remainingArgumentList.length; i += 2) {
            String key = remainingArgumentList[i - 1];
            String value = remainingArgumentList[i];
            if (key.startsWith("-")) {
                key = key.substring(1);
            }
            if (keys.contains(key)) {
                props = PropertyUtil.setProperty(props, key, value);
            } else {
                throw new RuntimeException("key " + key + " is unknown, possible values are " + keys);
            }
        }
        return props;

    }

}
