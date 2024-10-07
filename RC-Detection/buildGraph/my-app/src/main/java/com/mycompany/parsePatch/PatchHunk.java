package com.mycompany.parsePatch;

import java.util.ArrayList;

public class PatchHunk {
    private ArrayList<PatchLine> patchLines;

    public PatchHunk() {
        patchLines = new ArrayList<>();
    }

    public void addLine(PatchLine l) {
        patchLines.add(l);
    }

    public ArrayList<PatchLine> getLines() {
        return patchLines;
    }

    public String toString() {
        String ret = "";
        for (int i = 0; i < patchLines.size(); ++i) {
            ret = ret + "\n" + patchLines.get(i);
        }
        return ret;
    }
}
