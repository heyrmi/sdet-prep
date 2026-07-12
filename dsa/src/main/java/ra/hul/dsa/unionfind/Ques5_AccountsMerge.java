package ra.hul.dsa.unionfind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Accounts Merge - map emails to int ids, union emails within each account, group by root.
 * LeetCode #721 (Medium)
 *
 * Time: O(N * alpha + N log N) (N = total emails), Space: O(N)
 */
public class Ques5_AccountsMerge {

    static List<List<String>> accountsMerge(List<List<String>> accounts) {
        Map<String, Integer> emailId = new HashMap<>();   // email -> dense int id
        Map<String, String> emailOwner = new HashMap<>(); // email -> person name

        // 1. Assign ids and record owners.
        for (List<String> acct : accounts) {
            String name = acct.get(0);
            for (int i = 1; i < acct.size(); i++) {
                String email = acct.get(i);
                emailId.computeIfAbsent(email, k -> emailId.size());
                emailOwner.put(email, name);
            }
        }

        // 2. + 3. Union all emails within each account.
        UnionFind uf = new UnionFind(emailId.size());
        for (List<String> acct : accounts) {
            if (acct.size() <= 1) continue;
            int firstId = emailId.get(acct.get(1));
            for (int i = 2; i < acct.size(); i++) {
                uf.union(firstId, emailId.get(acct.get(i)));
            }
        }

        // 4. Group emails by their root id.
        Map<Integer, List<String>> groups = new HashMap<>();
        for (Map.Entry<String, Integer> e : emailId.entrySet()) {
            int root = uf.find(e.getValue());
            groups.computeIfAbsent(root, k -> new ArrayList<>()).add(e.getKey());
        }

        // 5. Sort each group's emails and prepend the owner name.
        List<List<String>> result = new ArrayList<>();
        for (List<String> emails : groups.values()) {
            Collections.sort(emails);
            List<String> entry = new ArrayList<>();
            entry.add(emailOwner.get(emails.get(0)));
            entry.addAll(emails);
            result.add(entry);
        }
        return result;
    }

    static class UnionFind {
        private final int[] parent;
        private final int[] rank;
        private int count;

        public UnionFind(int n) {
            parent = new int[n];
            rank = new int[n];
            for (int i = 0; i < n; i++) parent[i] = i;
            count = n;
        }

        public int find(int x) {
            while (parent[x] != x) {
                parent[x] = parent[parent[x]];
                x = parent[x];
            }
            return x;
        }

        public void union(int a, int b) {
            int ra = find(a), rb = find(b);
            if (ra == rb) return;
            if (rank[ra] < rank[rb]) parent[ra] = rb;
            else if (rank[ra] > rank[rb]) parent[rb] = ra;
            else { parent[rb] = ra; rank[ra]++; }
            count--;
        }
    }

    static void main() {
        List<List<String>> accounts = new ArrayList<>();
        accounts.add(List.of("John", "johnsmith@mail.com", "john00@mail.com"));
        accounts.add(List.of("John", "johnnybravo@mail.com"));
        accounts.add(List.of("John", "johnsmith@mail.com", "john_newyork@mail.com"));
        accounts.add(List.of("Mary", "mary@mail.com"));
        List<List<String>> res = accountsMerge(accounts);
        // Output order doesn't matter; sort for a stable printout.
        res.sort(Comparator.comparing(Object::toString));
        System.out.println(res);
        // [[John, john00@mail.com, john_newyork@mail.com, johnsmith@mail.com], [John, johnnybravo@mail.com], [Mary, mary@mail.com]]
    }
}
