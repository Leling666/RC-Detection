package com.mycompany.parseJoernDot;

import java.util.ArrayList;

public class JoernNode {
    public final String id;
    public final String type;
    public final String code;
    public int lineNum;
    public ArrayList<JoernEdge> edges;

    public JoernNode(String id_, String type_, String code_, int lineNum_) {
        id = id_;
        type = type_;
        code = code_;
        lineNum = lineNum_;
        edges = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "(" + id + "," + type + "," + code + "," + lineNum + ")";
    }
}
