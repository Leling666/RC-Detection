package com.mycompany.mapping;

import java.util.ArrayList;

public class GetPos {
    private ArrayList<Integer> lineBegs;

    public GetPos(String fContent) {
        lineBegs = new ArrayList<>();

        String[] lines = fContent.split("\n");
        int p1 = 0, p2 = 0;
        for (int i = 0; i < lines.length; ++i) {
            p1 = p2;
            lineBegs.add(p1);
            p2 = p1 + lines[i].length() + 1;

        }
    }

    public static int Upperbound(ArrayList<Integer> vector, int val) {
        int l = 0, u = vector.size() - 1;
        while (l < u) {
            int midp = l + (u - l) / 2;
            int mid = vector.get(midp);
            if (val < mid) {
                u = midp;
            } else {
                l = midp + 1;
            }
        }

        return val < vector.get(l) ? l : vector.size();
    }

    
    public int getLineNum(int offset) {
        return Upperbound(lineBegs, offset);
    }
}
