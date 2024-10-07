package com.mycompany.parsePatch;

import java.util.ArrayList;

public class PatchFile {
    private ArrayList<PatchHunk> hunks;
    public String fname;
    public boolean isAdd, isDel, isMod, isRename;

    public PatchFile(String fname, boolean isAdd, boolean isDel, boolean isMod, boolean isRename) {
        hunks = new ArrayList<>();
        this.fname = fname;
        this.isAdd = isAdd;
        this.isDel = isDel;
        this.isMod = isMod;
        this.isRename = isRename;
    }

    public void addHunk(PatchHunk h) {
        hunks.add(h);
    }

    public ArrayList<PatchHunk> getHunk() {
        return hunks;
    }

    @Override
    public String toString() {
        if (isAdd) {
            return "add:" + fname;
        } else if (isDel) {
            return "del:" + fname;
        } else if (isMod) {
            return "mod:" + fname;
        }
        return "rename:" + fname;
    }
}
