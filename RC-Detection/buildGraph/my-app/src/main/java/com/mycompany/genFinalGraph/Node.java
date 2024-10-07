package com.mycompany.genFinalGraph;

import java.util.ArrayList;

import com.mycompany.app.Unit;

public class Node {
    public Unit u;
    public int index;
    public ArrayList<Integer> cfgEdges;
    public ArrayList<Integer> dfgEdges;
    public ArrayList<Integer> cfgParents;
    public ArrayList<Integer> dfgParents;

    public ArrayList<Integer> fieldEdges;
    public ArrayList<Integer> fieldParents;

    public ArrayList<Integer> methodEdges;
    public ArrayList<Integer> methodParents;

    private int mappingIndex;
    public String fName;
    public boolean isDel;


    public Node(Unit u_, int index_, String fName_) {
        u = u_;
        index = index_;
        cfgEdges = new ArrayList<>();
        dfgEdges = new ArrayList<>();

        cfgParents = new ArrayList<>();
        dfgParents = new ArrayList<>();

        fieldEdges = new ArrayList<>();
        fieldParents = new ArrayList<>();

        methodEdges = new ArrayList<>();
        methodParents = new ArrayList<>();
        fName = fName_;

        mappingIndex = -1;
        isDel=false;
    }

    @Override
    public String toString() {
        return u.getCodeStr();
    }

    public int getMappingIndex() {
        return mappingIndex;
    }

    public void setMappingIndex(int i) {
        mappingIndex = i;
    }

    @Override
    public Node clone() {
        Node n = new Node(u, index, fName);
        n.mappingIndex = mappingIndex;
        cfgEdges.forEach((e) -> {
            n.cfgEdges.add(e);
        });
        dfgEdges.forEach((e) -> {
            n.dfgEdges.add(e);
        });
        cfgParents.forEach((e) -> {
            n.cfgParents.add(e);
        });
        dfgParents.forEach((e) -> {
            n.dfgParents.add(e);
        });
        fieldEdges.forEach((e) -> {
            n.fieldEdges.add(e);
        });
        fieldParents.forEach((e) -> {
            n.fieldParents.add(e);
        });
        methodEdges.forEach((e) -> {
            n.methodEdges.add(e);
        });
        methodParents.forEach((e) -> {
            n.methodParents.add(e);
        });
        return n;
    }
}
