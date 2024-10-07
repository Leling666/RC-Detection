package com.mycompany.Project;

import com.mycompany.genFinalGraph.Node;

public class Call {
    public final String cname;
    public final int pNum;
    public final String fname;
    public final Node n;

    public Call(String c, int p, String f, Node n) {
        cname = c;
        pNum = p;
        fname = f;
        this.n = n;
    }

    @Override
    public String toString() {
        return "call:" + cname + ",in class " + fname + " with " + pNum + " parameters";
    }
}
