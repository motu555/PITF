package util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jinyuanyuan on 2018/3/20.
 */
public class BinarySearchTest {


    public static void main(String[] args) {
       List<Double>a = new ArrayList<>();
        a.add(20.0);
        a.add( 21.0);
        a.add( 22.0);
        a.add( 23.0);



        System.out.println( 21.5 >  21);

        System.out.println(binary_search(a, 23.0));

        System.out.println(binary_search(a, 20.0));
        System.out.println(binary_search(a, 22.5));
        System.out.println(binary_search(a, 19.0));
        System.out.println(binary_search(a, 24.0));
    }

    public static int binary_search(List<Double> a, double key) {
        int low = 0;
        int high = a.size() - 1;
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (a.get(mid) > key) {
                high = mid - 1;
            } else {
                low = mid;
            }
        }


        if (a.get(low) <= key) {
            return low;
        } else {
            return -1;
        }
    }
}
