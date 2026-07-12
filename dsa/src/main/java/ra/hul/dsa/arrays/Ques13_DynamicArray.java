package ra.hul.dsa.arrays;

import java.util.Arrays;

/**
 * Dynamic Array - build a resizable int[] backed by a raw array with capacity doubling.
 *
 * Time: add O(1) amortized; get/set/size/removeLast O(1). Space: O(n)
 */
public class Ques13_DynamicArray {

    // The deliverable is the resizable array itself, declared as a nested static class.
    static class DynArray {
        private int[] data;
        private int size;

        DynArray() {
            this.data = new int[4]; // small positive starting capacity so doubling works
            this.size = 0;
        }

        void add(int x) {
            if (size == data.length) {
                data = Arrays.copyOf(data, data.length * 2); // grow, then add
            }
            data[size++] = x;
        }

        int get(int i) {
            checkIndex(i);
            return data[i];
        }

        void set(int i, int x) {
            checkIndex(i);
            data[i] = x;
        }

        int size() {
            return size;
        }

        int removeLast() {
            if (size == 0) {
                throw new IndexOutOfBoundsException("removeLast on empty DynArray");
            }
            return data[--size]; // logically gone; slot reused on a future add
        }

        private void checkIndex(int i) {
            if (i < 0 || i >= size) {
                throw new IndexOutOfBoundsException("Index " + i + " out of bounds for size " + size);
            }
        }
    }

    static void main() {
        DynArray a = new DynArray();
        a.add(10);
        a.add(20);
        a.add(30);
        System.out.println(a.size());       // 3
        System.out.println(a.get(1));        // 20
        a.set(1, 99);
        System.out.println(a.get(1));        // 99
        System.out.println(a.removeLast());  // 30
        System.out.println(a.size());        // 2
    }
}
