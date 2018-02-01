package com.hansoolabs.and.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vulpes on 2017. 3. 6..
 */

public class LongestCommonSubSequence {

    public interface AreObjectEquals<T, K> {
        boolean areEquals(T obj1, K obj2);
    }

    private enum Origin {
        TOP, LEFT, DIAGONAL, END
    }

    public static <T, K> List<T> find(List<T> a, List<K> b, AreObjectEquals<T, K> comparator) {
        int sizeOfA = a.size();
        int sizeOfB = b.size();
        int[][] LCS = new int[sizeOfA + 1][sizeOfB + 1];
        Origin[][] solution = new Origin[sizeOfA + 1][sizeOfB + 1];

        for (int i = 0; i <= sizeOfB; i++) {
            LCS[0][i] = 0;
            solution[0][i] = Origin.END;
        }

        for (int i = 0; i < sizeOfA; i++) {
            LCS[i][0] = 0;
            solution[i][0] = Origin.END;
        }
        for (int i = 1; i <= sizeOfA; i++) {
            for (int j = 1; j <= sizeOfB; j++) {
                if (comparator.areEquals(a.get(i - 1), b.get(j - 1))) {
                    LCS[i][j] = LCS[i - 1][j - 1] + 1;
                    solution[i][j] = Origin.DIAGONAL;
                } else {
                    LCS[i][j] = Math.max(LCS[i - 1][j], LCS[i][j - 1]);
                    if (LCS[i][j] == LCS[i - 1][j]) {
                        solution[i][j] = Origin.TOP;
                    } else {
                        solution[i][j] = Origin.LEFT;
                    }
                }
            }
        }
        Origin origin;
        List<T> answer = new ArrayList<>();
        int idxOfA = sizeOfA;
        int idxOfB = sizeOfB;

        while ((origin = solution[idxOfA][idxOfB]) != Origin.END) {
            if (origin == Origin.DIAGONAL) {
                answer.add(0, a.get(idxOfA - 1));
                idxOfA--;
                idxOfB--;
            } else if (origin == Origin.LEFT) {
                idxOfB--;
            } else {
                idxOfA--;
            }
        }
        return answer;
    }
}
