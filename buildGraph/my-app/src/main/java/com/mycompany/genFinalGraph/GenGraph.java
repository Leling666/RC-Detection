package com.mycompany.genFinalGraph;

import java.util.ArrayList;
import java.util.HashMap;

import com.github.javaparser.ast.stmt.IfStmt;
import com.mycompany.app.Unit;
import com.mycompany.parseJoernDot.JoernEdge;
import com.mycompany.parseJoernDot.JoernGraph;
import com.mycompany.parseJoernDot.JoernNode;

public class GenGraph {
    private HashMap<String, Node> nodeMap;
    private ArrayList<JoernNode> jNodes;
    private ArrayList<JoernEdge> jEdges;
    private ArrayList<Node> nodes;
    private ArrayList<Unit> units;
    private ArrayList<Edge> edges;
    private ArrayList<Node> specialNodes;

    public GenGraph(ArrayList<JoernNode> jns, ArrayList<JoernEdge> jes, ArrayList<Unit> u, String fName) {
        units = u;

        jNodes = jns;
        jEdges = jes;
        edges = new ArrayList<>();
        nodes = new ArrayList<>();
        specialNodes = new ArrayList<>();

        for (int i = 0; i < units.size(); ++i) {
            Node n = new Node(units.get(i), i, fName);
            nodes.add(n);
            if (units.get(i).getCodeStr().indexOf("try") != -1 || units.get(i).getCodeStr().indexOf("catch") != -1
                    || units.get(i).getCodeStr().indexOf("else") != -1 || units.get(i).getType().equals("do")
                    || units.get(i).getType().equals("while")
                    || units.get(i).getType().equals(IfStmt.class.getSimpleName())) {
                specialNodes.add(n);
            }
        }

        nodeMap = new HashMap<>();
        mapNode();
        genGraph();
        addControlEdges();
        collectEdges();

        // System.out.println(toDot("test"));
    }

    private void mapNode() {
        for (var jn : jNodes) {
            for (var n : nodes) {
                if (n.u.getBegPos().line <= jn.lineNum && n.u.getEndPos().line >= jn.lineNum) {
                    nodeMap.put(jn.id, n);

                }
            }
        }

    }

    private void genGraph() {
        ArrayList<JoernEdge> tedges = new ArrayList<>();
        for (var e : jEdges) {

            if (!e.type.equals("CFG") && !e.type.equals("DDG")) {
                continue;
            }
            String src = e.src;
            String dst = e.dst;
            Node n1 = nodeMap.get(src);
            Node n2 = nodeMap.get(dst);

            if (n1 == n2 || n1 == null || n2 == null) {
                continue;
            }
            tedges.add(e);
        }

        for (var e : tedges) {

            Node n1 = nodeMap.get(e.src);
            Node n2 = nodeMap.get(e.dst);
            assert n1 != n2;
            assert e.type.equals("CFG") || e.type.equals("DDG");
            assert e.src != e.dst;
            if (e.type.equals("CFG")) {
                if (!n1.cfgEdges.contains(n2.index)) {
                    n1.cfgEdges.add(n2.index);
                    n2.cfgParents.add(n1.index);
                }
                edges.add(new Edge(n1, n2, "CFG"));

            } else {
                if (!n1.dfgEdges.contains(n2.index)) {
                    n1.dfgEdges.add(n2.index);
                    n2.dfgParents.add(n1.index);
                }
                edges.add(new Edge(n1, n2, "DDG"));
            }
        }

    }

    private void addControlEdges() {
        for (Node n : specialNodes) {
            if (n.index + 1 >= nodes.size()) {
                continue;
            }
            Node next = nodes.get(n.index + 1);
            if (n.cfgEdges.contains(n.index + 1)) {
                continue;
            }
            for (var pre : next.cfgParents) {
                Node preNode = nodes.get(pre);
                preNode.cfgEdges.removeIf((index) -> {
                    return index == next.index;
                });
                if (!preNode.cfgEdges.contains(n.index))
                    preNode.cfgEdges.add(n.index);
                if (!n.cfgParents.contains(preNode.index))
                    n.cfgParents.add(preNode.index);
                if (!n.cfgEdges.contains(next.index))
                    n.cfgEdges.add(next.index);
            }
            next.cfgParents.clear();
            next.cfgParents.add(n.index);
        }
    }

    private void collectEdges() {
        for (Node n : nodes) {
            Node src = n;
            for (var dIndex : n.cfgEdges) {
                Node dst = nodes.get(dIndex);
                edges.add(new Edge(src, dst, "CFG"));
            }

            for (var dIndex : n.dfgEdges) {
                Node dst = nodes.get(dIndex);
                edges.add(new Edge(src, dst, "DDG"));
            }
        }
    }

    public ArrayList<Node> getNodes() {

        return nodes;
    }

    public ArrayList<Edge> getEdges() {
        return edges;
    }

    public String toDot(String name) {
        StringBuffer sb = new StringBuffer();
        sb.append("digraph " + name + " {\n");
        for (var n : nodes) {
            sb.append(String.format("  %d [label=\"%s(%d-%d)\"];\n", n.index, n.u.getCodeStr().replace("\"", "\\\""),
                    n.u.getBegPos().line,
                    n.u.getEndPos().line));
        }
        for (var n : nodes) {
            for (var e : n.cfgEdges) {
                sb.append(String.format("  %d -> %d;\n", n.index, nodes.get(e).index));
            }
            // for (var e : n.dfgEdges) {
            // sb.append(String.format(" %d -> %d [style=dotted];\n", n.index,
            // nodes.get(e).index));
            // }
        }
        sb.append("}\n");
        return sb.toString();
    }
}
