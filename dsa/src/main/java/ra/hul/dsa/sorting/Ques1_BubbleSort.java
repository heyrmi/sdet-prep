package ra.hul.dsa.sorting;

import java.util.Arrays;

/**
 * Bubble Sort - Sort array by repeatedly swapping adjacent elements.
 * (Easy - fundamental sorting algorithm)
 *
 * Time: O(n^2) worst/average, O(n) best (already sorted with early exit)
 * Space: O(1) - in-place
 */
public class Ques1_BubbleSort {

    public static void bubbleSort(int[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - 1 - i; j++) {
                if (arr[j] > arr[j + 1]) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                    swapped = true;
                }
            }
            if (!swapped) break; // already sorted
        }
    }

    public static void main(String[] args) {
        int[] arr1 = {64, 34, 25, 12, 22, 11, 90};
        bubbleSort(arr1);
        System.out.println(Arrays.toString(arr1)); // [11, 12, 22, 25, 34, 64, 90]

        int[] arr2 = {5, 1, 4, 2, 8};
        bubbleSort(arr2);
        System.out.println(Arrays.toString(arr2)); // [1, 2, 4, 5, 8]
    }
}
