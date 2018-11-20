/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uros.citlab.module.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.Bidi;

/**
 * @author gundram
 */
public class BidiUtil {

    public static final int SOFT_LTR = Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT;
    public static final int SOFT_RTL = Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT;
    public static final int FORCE_LTR = Bidi.DIRECTION_LEFT_TO_RIGHT;
    public static final int FORCE_RTL = Bidi.DIRECTION_RIGHT_TO_LEFT;
    private static final Logger LOG = LoggerFactory.getLogger(BidiUtil.class);

    private static int[] getLevels(Bidi bidi) {
        int length = bidi.getLength();
        int[] levels = new int[length];
        int runCount = bidi.getRunCount();
        int p = 0;
        for (int i = 0; i < runCount; ++i) {
            int rlimit = bidi.getRunLimit(i);
            int rlevel = bidi.getRunLevel(i);
            while (p < rlimit) {
                levels[p++] = rlevel;
            }
        }
        return levels;
    }

    public static String logical2visual(String logical, int... flags) {
        Bidi bidi = getBidi(logical, flags);
        int[] levels = getLevels(bidi);
        char[] chars = logical.toCharArray();
        reorderVisually(levels, chars);
        String pre = String.valueOf(chars);
        return removeDirectionalityControlCharacter(pre);
    }

    private static void reorderVisually(int[] levels, char[] objects) {
        int len = levels.length;

        int lowestOddLevel = 1023;
        int currentLevel = 0;
        // initialize mapping and levels
        for (int i = 0; i < len; i++) {
            int level = levels[i];
            if (level > 1023) {
                throw new RuntimeException("cannot handle levels larger than 1023, but is " + level);
            }
            currentLevel = Math.max(currentLevel, level);
            if ((level % 2) != 0 && level < lowestOddLevel) {
                lowestOddLevel = level;
            }
        }

        while (currentLevel >= lowestOddLevel) {
            int idx = 0;
            for (; ; ) {
                while (idx < len && levels[idx] < currentLevel) {
                    idx++;
                }
                int begin = idx++;

                if (begin == len) {
                    break; // no more runs at this level
                }

                while (idx < len && levels[idx] >= currentLevel) {
                    idx++;
                }
                int end = idx - 1;
                //reverse substring
                while (begin < end) {
                    char temp = objects[begin];
                    objects[begin] = objects[end];
                    objects[end] = temp;
                    begin++;
                    end--;
                }
            }

            --currentLevel;
        }
    }

    private static Bidi getBidi(String string, int... flags) {
        int direction = flags.length == 0 ? SOFT_LTR : flags[0];
        return new Bidi(string, direction);
    }

    /**
     * removes character which indicates a directionality and are no visual
     * signs. This method should only be applied on strings, wich are already
     * transformed from logical to visual interpretation. See
     * http://www.unicode.org/reports/tr9/ for more details.
     *
     * @param string
     * @return string without directionality control character
     */
    public static String removeDirectionalityControlCharacter(String string) {
        if (string == null) {
            return null;
        }
        //fast check, if any character is in union-range of bidi-marks
        final char[] chars = string.toCharArray();
        boolean ok = true;
        for (char aChar : chars) {
            if (aChar == '\u061C' || (aChar >= '\u200E' && aChar <= '\u2069')) {
                ok = false;
            }
        }
        if (ok) {
            return string;
        }
        //exact transformation
        StringBuilder sb = new StringBuilder();
        for (char c : chars) {
            if ((c < '\u2066' || c > '\u2069') //LRI, RLI, FSI, PDI not allowed
                    && (c < '\u202A' || c > '\u202E') // LRE, RLE, LRO, RLO, PDF not allowed
                    && c != '\u200E' && c != '\u200F' && c != '\u061C'//RLM, LRM, ALM not allowed 
                    ) {
                sb.append(c);
            } else {
                LOG.warn(String.format("delete character \\u%04X from string '%s' because it is a bidi control character.", (int) c, string));
            }
        }
        return sb.toString();
    }

    public static String visual2logical(String visual, int... flags) {
        if ((visual == null) || (visual.length() == 0)) {
            return visual;
        }
//        if (isLeft2Right(visual, flags)) {
//            return visual;
//        }

        Bidi bidi = getBidi(visual, flags);

        if (bidi.isLeftToRight()) {
            return visual;
        }

        int count = bidi.getRunCount();
        byte[] levels = new byte[count];
        Integer[] runs = new Integer[count];

        for (int i = 0; i < count; i++) {
            levels[i] = (byte) bidi.getRunLevel(i);
            runs[i] = i;
        }

        Bidi.reorderVisually(levels, 0, runs, 0, count);

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < count; i++) {
            int index = runs[i];
            int start = bidi.getRunStart(index);
            int end = bidi.getRunLimit(index);
            int level = levels[index];

            if ((level & 1) != 0) {
                for (; --end >= start; ) {
                    result.append(visual.charAt(end));
                }
            } else {
                result.append(visual, start, end);
            }
        }

        return result.toString();
    }

}
