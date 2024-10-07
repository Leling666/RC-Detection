package com.mycompany.parseJoernDot;

public class JoernEdge {
    public final String type;
    public final String content;
    public final String src;
    public final String dst;

    public JoernEdge(String type_, String content_, String src_, String dst_) {
        type = type_;
        content = content_;
        src = src_;
        dst = dst_;
    }

    @Override
    public String toString() {
        return "(" + type + "," + content + "," + src + "," + dst + ")";
    }
}
