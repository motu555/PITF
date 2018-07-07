package util;

import java.util.*;

/**
 * Created by jinyuanyuan on 2018/4/1.
 */
public class MapSortTest {
    public static void main(String[] args) {
        Map<Integer, Map<Integer, List<Long>>> userTagTimeList = new HashMap<>();
        Map<Integer, List<Long>>tagTimeList = new HashMap<>();
        List<Long>list = new ArrayList<>();
        list.add(1000L);
        list.add(900L);
        tagTimeList.put(5, list);
        userTagTimeList.put(0, tagTimeList);
        System.out.println(userTagTimeList.get(0).get(5));

        for (int user = 0; user < 1; user++) {
            Map<Integer, List<Long>> tempTagsTimeMap = userTagTimeList.get(user);
            for (Map.Entry<Integer, List<Long>> tempTagsTime : tempTagsTimeMap.entrySet()) {
                List<Long> tempTimeList = tempTagsTime.getValue();
                Collections.sort(tempTimeList);
            }
        }
        System.out.println(userTagTimeList.get(0).get(5));

        System.out.println(2*-9);
        long test =1238536800000L;
        double timeUnit = 3600000.0  * 24;
        System.out.println(test/timeUnit);
    }


}
