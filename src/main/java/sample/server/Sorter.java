package sample.server;

import java.util.ArrayList;
import java.util.List;

public class Sorter {
    //selection sort
    public static List<Integer> sort(List<Integer> arr) {
        ArrayList<Integer> array = new ArrayList<>();
        array.addAll(arr);
        for (int min = 0; min < array.size() - 1; min++) {
            int least = min;
            for (int j = min + 1; j < array.size(); j++) {
                if (array.get(j) < array.get(least)) {
                    least = j;
                }
            }
            int tmp = array.get(min);
            array.set(min, array.get(least));
            array.set(least, tmp);
        }
        return array;

    }
}
