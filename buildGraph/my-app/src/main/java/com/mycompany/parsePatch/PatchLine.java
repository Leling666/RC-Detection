package com.mycompany.parsePatch;

public class PatchLine {
    public String line;
    public Boolean isDel;
    public Boolean isAdd;
    public Boolean isBg;
    public int lineno;

    public PatchLine(String line, Boolean isDel, Boolean isAdd, Boolean isBg, int lineno) {
        this.line = line;
        this.isDel = isDel;
        this.isAdd = isAdd;
        this.isBg = isBg;
        this.lineno = lineno;
    }

    @Override
    public String toString() {
        if (isDel) {
            return "-" + String.valueOf(this.lineno) + ":" + this.line;
        } else if (isAdd) {
            return "+" + String.valueOf(this.lineno) + ":" + this.line;
        }
        return String.valueOf(this.lineno) + ":" + this.line;
    }

}
