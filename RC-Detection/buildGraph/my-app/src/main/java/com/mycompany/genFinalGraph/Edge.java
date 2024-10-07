package com.mycompany.genFinalGraph;

public class Edge {
    public final Node src, dst;
    public final String type;

    public Edge(Node s, Node d, String type) {
        src = s;
        dst = d;
        this.type = type;
    }

    @Override
    public String toString() {
        return "(" + src.u.getCodeStr() + "," + dst.u.getCodeStr() + "," + this.type + ")";
    }}

