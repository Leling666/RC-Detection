package com.mycompany.app;

import com.github.javaparser.Position;
import com.mycompany.genFinalGraph.Node;

public class Unit {
    private Position p1;
    private Position p2;
    private String type;
    private char endChar;
    private char beginChar;
    private String code;
    private boolean skipAhead;
    private boolean hasAnnotation;
    private boolean skipTwoSides;
    private boolean notSkip;
    private Node n;

    public Unit(Position p1, Position p2, String type, char endChar) {
        this.p1 = p1;
        this.p2 = p2;
        this.type = type;
        this.endChar = endChar;
        hasAnnotation = false;
        notSkip = false;
        n = null;
    }

    public boolean getSkipTwoSides() {
        return skipTwoSides;
    }

    public void setSkipTwoSides(boolean s) {
        skipTwoSides = s;
    }

    public void setBeginChar(char c) {
        beginChar = c;
    }

    public char getBeginChar() {
        return beginChar;
    }

    public boolean getSkipAhead() {
        return skipAhead;
    }

    public void setSkipAhead(boolean sp) {
        skipAhead = sp;
    }

    public Position getBegPos() {
        return p1;
    }

    public Position getEndPos() {
        return p2;
    }

    public void setBegPos(Position p) {
        p1 = p;
    }

    public void setEndPos(Position p) {
        p2 = p;
    }

    public String getType() {
        return type;
    }

    public char GetEndChar() {
        return endChar;
    }

    public void setCode(String codeStr) {
        code = codeStr;
    }

    public String getCodeStr() {
        return code;
    }

    @Override
    public boolean equals(Object obj) {
        Unit u = Unit.class.cast(obj);
        boolean cond1 = u.getBegPos().line == getBegPos().line;
        boolean cond2 = u.getEndPos().line == getEndPos().line;
        boolean cond3 = u.getBegPos().column == getBegPos().column;
        boolean cond4 = u.getEndPos().column == getEndPos().column;
        if (cond1 && cond2 && cond3 && cond4) {
            return true;
        }
        return false;
    }

    public void setHasAnnotation(boolean f) {
        hasAnnotation = f;
    }

    public boolean getHasAnnotation() {
        return hasAnnotation;
    }

    public void setNode(Node n) {
        this.n = n;
    }

    public Node getNode() {
        return n;
    }

    public void setNotSkip(boolean b) {
        notSkip = b;
    }

    public boolean getNotSkip() {
        return notSkip;
    }
}
