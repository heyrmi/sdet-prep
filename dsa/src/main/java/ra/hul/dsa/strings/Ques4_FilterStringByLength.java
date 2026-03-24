package ra.hul.dsa.strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Ques4_FilterStringByLength {

    public static List<String> filterByLength(String[] arr, int minLength) {
        return Arrays.stream(arr)
                .filter(str -> str != null && str.length() > minLength)
                .collect(Collectors.toList());
    }

    public static List<String> filterTraditional(String[] arr, int minLength) {
        List<String> result = new ArrayList<>();

        for (String str : arr) {
            if (str != null && str.length() > minLength) {
                result.add(str);
            }
        }
        return result;
    }

    // FollowUp: Filter By descending length:
    public static List<String> filterByDescendingLength(String[] arr, int minLength) {
        return Arrays.stream(arr)
                .filter(str -> str != null && str.length() > minLength)
                .sorted(Comparator.comparingInt(String::length).reversed())
                .collect(Collectors.toList());
    }

    static void main() {
        String[] input = {"hi", "apple", "go", "banana", "cat", "watermelon", null, ""};
        System.out.println(filterByLength(input, 3));
        System.out.println(filterTraditional(input, 3));
        System.out.println(filterByDescendingLength(input, 3));
        // [apple, banana, watermelon]
    }
}
