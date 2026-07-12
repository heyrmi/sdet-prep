package ra.hul.dsa.greedy;

/**
 * Gas Station - starting index to complete the circular route once, or -1 if impossible.
 *
 * Time: O(n), Space: O(1) - single pass
 */
public class Ques4_GasStation {

    public static int canCompleteCircuit(int[] gas, int[] cost) {
        long total = 0; // grand running total (never reset) -> feasibility
        long tank = 0;  // running tank since current candidate start
        int start = 0;
        for (int i = 0; i < gas.length; i++) {
            int diff = gas[i] - cost[i];
            total += diff;
            tank += diff;
            if (tank < 0) {     // candidate start failed by station i
                start = i + 1;  // none of start..i can work; try i+1
                tank = 0;
            }
        }
        return total >= 0 ? start : -1;
    }

    static void main() {
        System.out.println(canCompleteCircuit(new int[]{1, 2, 3, 4, 5}, new int[]{3, 4, 5, 1, 2})); // 3
        System.out.println(canCompleteCircuit(new int[]{2, 3, 4}, new int[]{3, 4, 3}));             // -1
    }
}
