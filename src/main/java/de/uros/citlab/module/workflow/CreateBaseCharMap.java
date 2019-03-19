package de.uros.citlab.module.workflow;

import com.achteck.misc.types.CharMap;
import com.achteck.misc.types.ConfMat;
import de.uros.citlab.errorrate.util.ObjectCounter;
import de.uros.citlab.module.util.CharMapUtil;
import de.uros.citlab.module.util.FileUtil;
import org.apache.commons.math3.util.Pair;

import java.io.File;
import java.util.List;

public class CreateBaseCharMap {
    public static void main(String[] args) {
        ObjectCounter<Character> oc = new ObjectCounter<>();
        for (File txt : FileUtil.listFiles(HomeDir.getFile("data/base"), "txt", true)) {
            String s = FileUtil.readLine(txt);
            for (char c : s.toCharArray()) {
                oc.add(c);
            }

        }
        CharMap<Integer> cm = new CharMap<>();
        cm.put(0, ConfMat.NaC);
        int sum = 0;
        int cnt = 0;
        List<Pair<Character, Long>> resultOccurrence = oc.getResultOccurrence();
//        resultOccurrence.sort((o1, o2) -> o1.getSecond().compareTo(o2.getSecond()));
        ObjectCounter oc2 = new ObjectCounter();
        for (Pair<Character, Long> characterLongPair : resultOccurrence) {
            int cntNew = characterLongPair.getSecond().intValue();
                System.out.println(cnt + " : " + cm.keySet().size() + " " + (characterLongPair.getFirst() / 379202.0));
            if (cnt != cntNew) {
                cnt = cntNew;
            }
            sum += cntNew;

            if (cntNew >= 4) {
                oc2.add(characterLongPair.getFirst(), characterLongPair.getSecond());
                cm.put(cm.keySet().size(), characterLongPair.getFirst());
            }
        }

        System.out.println(resultOccurrence);
        System.out.println(sum);
        CharMapUtil.saveCharMap(oc2, HomeDir.getFile("cm_chinese_base.txt"));
    }
}
