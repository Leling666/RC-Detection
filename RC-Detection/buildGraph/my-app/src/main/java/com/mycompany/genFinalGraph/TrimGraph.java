package com.mycompany.genFinalGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.common.base.Predicates;
import com.mycompany.parsePatch.PatchLine;

public class TrimGraph {
    private ArrayList<PatchLine> patchLines;
    private ArrayList<Node> nodes;
    private ArrayList<Node> finalNodes;
    private TreeMap<Integer, Node> idMap;
    private int begIndex;
    private String fName;

    public TrimGraph(ArrayList<PatchLine> plines, ArrayList<Node> nodes, int begIndex, String fName) {
        this.patchLines = plines;
        this.nodes = nodes;
        this.begIndex = begIndex;
        this.fName = fName;
        finalNodes = new ArrayList<>();
        idMap = new TreeMap<>();

        filterNodes();

        HashSet<Integer> visited = new HashSet<>();
        for (var topNode : finalNodes) {
            HashSet<Integer> set1 = new HashSet<>();
            HashSet<Integer> set2 = new HashSet<>();
            dfs1(topNode, nodes.get(topNode.index), set1, visited);
            dfs2(topNode, nodes.get(topNode.index), set2, visited);
            visited.add(topNode.index);
        }
        compactNode();
    }

    private Node getNode(PatchLine pl) {
        int left = 0, right = nodes.size() - 1;
        int mid;
        while (left < right) {
            mid = left + (right - left) / 2;
            if (pl.lineno <= nodes.get(mid).u.getEndPos().line) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }

        if (left >= nodes.size() || nodes.get(left).u.getBegPos().line > pl.lineno)
            return null;
        return nodes.get(left);

    }

    private void filterNodes() {
        Node pre = null;
        for (var l : patchLines) {
            Node n = getNode(l);
            if (n != null && n != pre) {
                pre = n;
                Node t = new Node(n.u, n.index, fName);
                t.u.setNode(t);
                finalNodes.add(t);
                idMap.put(n.index, n);
            }
        }
    }

    private void dfs1(Node topNode, Node curNode, HashSet<Integer> set, HashSet<Integer> visited) {
        if (set.contains(curNode.index) || visited.contains(curNode.index)) {
            return;
        }
        set.add(curNode.index);

        for (var dst : curNode.cfgEdges) {
            if (idMap.containsKey(dst)) {
                if (dst != topNode.index && !topNode.cfgEdges.contains(dst) && !visited.contains(dst))
                    topNode.cfgEdges.add(dst);
            } else {
                dfs1(topNode, nodes.get(dst), set, visited);
            }
        }

    }

    private void dfs2(Node topNode, Node curNode, HashSet<Integer> set, HashSet<Integer> visited) {
        if (set.contains(curNode.index) || visited.contains(curNode.index)) {
            return;
        }
        set.add(curNode.index);
        for (var dst : curNode.dfgEdges) {
            if (idMap.containsKey(dst)) {
                if (dst != topNode.index && !topNode.dfgEdges.contains(dst) && !visited.contains(dst))
                    topNode.dfgEdges.add(dst);
            } else {
                dfs2(topNode, nodes.get(dst), set, visited);
            }
        }
    }

    private void compactNode() {
        HashMap<Integer, Integer> indexToIndex = new HashMap<>();
        HashMap<Integer, Integer> indexToPos = new HashMap<>();
        for (int i = 0; i < finalNodes.size(); ++i) {

            indexToIndex.put(finalNodes.get(i).index, begIndex + i);
            indexToPos.put(begIndex + i, i);
        }

        for (var n : finalNodes) {
            n.index = indexToIndex.get(n.index);
            for (int i = 0; i < n.cfgEdges.size(); ++i) {
                int index = indexToIndex.get(n.cfgEdges.get(i));
                int pos = indexToPos.get(index);
                n.cfgEdges.set(i, index);
                finalNodes.get(pos).cfgParents.add(n.index);
            }

            for (int i = 0; i < n.dfgEdges.size(); ++i) {
                int index = indexToIndex.get(n.dfgEdges.get(i));
                int pos = indexToPos.get(index);
                n.dfgEdges.set(i, index);
                finalNodes.get(pos).dfgParents.add(n.index);
            }
        }
    }

    public ArrayList<Node> getFinalGraph() {
        return finalNodes;
    }
}
