package com.mycompany.parseJoernDot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JoernGraph {
    private Map<String, JoernNode> nodeMap;
    private String fcontent;
    private String[] lines;
    private ArrayList<JoernNode> nodes;
    private ArrayList<JoernEdge> edges;
    private String root;
    private boolean hasVoidRet;
    private String voidRetId;

    private JoernNode begNode;

    public JoernGraph(String path) {

        begNode = null;
        nodeMap = new HashMap<>();
        try {
            fcontent = new String(Files.readAllBytes(Paths.get(path)));
            lines = fcontent.split("\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        nodes = new ArrayList<>();
        edges = new ArrayList<>();
        root = null;
        try {
            parseGraph();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addNode(JoernNode n) {
        nodeMap.put(n.id, n);
        nodes.add(n);
    }

    public void addEdge(JoernEdge edge) {
        String src = edge.src;
        assert nodeMap.containsKey(src);

        JoernNode n = nodeMap.get(src);
        n.edges.add(edge);
        edges.add(edge);
    }

    public String getRoot() {
        if (root != null) {
            return root;
        }
        Set<String> s1 = new HashSet<>();
        Set<String> s2 = new HashSet<>();
        for (var key : nodeMap.keySet()) {
            s1.add(key);
        }

        for (var key : edges) {
            s2.add(key.dst);
        }

        ArrayList<String> candidates = new ArrayList<String>();
        for (var k1 : s1) {
            if (!s2.contains(k1)) {
                candidates.add(k1);
            }
        }
        assert candidates.size() == 1;
        root = candidates.get(0);
        return root;
    }

    // parse graph name
    private String re1 = "digraph\\s+\"(.+)\"\\s+\\{\\s+";
    // parse node info in graph
    private String re2 = "\"(.+)\"\\s+\\[label\\s+=\\s+<(.+)>\\s+]";
    // parse edge info in graph
    private String re3 = "\\s+\"(.+)\"\\s+->\\s+\"(.+)\"\\s+\\[\\s+label\\s+=\\s\"(.+)\"\\]\\s*";

    private String re4 = "\\}";

    private Matcher getMatcher(String rex, String line) {
        Pattern p = Pattern.compile(rex);
        Matcher m = p.matcher(line);
        assert m.find();
        return m;
    }

    private String parseGraphName(String line) {

        Matcher m = getMatcher(re1, line);
        return m.group(1);
    }

    private JoernNode parseNode(String line) {

        Matcher m1 = getMatcher(re2, line);
        assert m1 != null;
        String nodeId = m1.group(1);
        String nodeContent = m1.group(2);
        int lineNum = -1;

        if (nodeContent.indexOf("<SUB>") != -1) {
            Matcher m2 = getMatcher("\\((.+)\\)<SUB>(.+)</SUB>", nodeContent);
            nodeContent = m2.group(1);
            lineNum = Integer.parseInt(m2.group(2));
        } else {
            Matcher m2 = getMatcher("\\((.+)\\)", nodeContent);
            nodeContent = m2.group(1);
        }

        nodeContent = nodeContent.replace(";", "");
        nodeContent = nodeContent.replace("&lt", "<");
        nodeContent = nodeContent.replace("&gt", ">");
        nodeContent = nodeContent.replace("&quot", "\"");

        var tmpNodes = nodeContent.split(",");
        if (tmpNodes.length == 2) {
            String type = nodeContent.split(",")[0];
            nodeContent = nodeContent.split(",")[1];
            return new JoernNode(nodeId, type, nodeContent, lineNum);
        } else {
            String type = nodeContent.split(",")[0];
            return new JoernNode(nodeId, type, null, lineNum);
        }
    }

    private JoernEdge parseEdge(String line) {
        Matcher m1 = getMatcher(re3, line);
        assert m1 != null;
        String src = m1.group(1);
        String dst = m1.group(2);
        if (hasVoidRet && (src.equals(voidRetId) || dst.equals(voidRetId))) {
            return null;
        }
        String edgeContent = m1.group(3);

        Matcher m2 = getMatcher("(.+)\\:\\s+(.*)", edgeContent);
        String type = m2.group(1);
        String content = m2.group(2);

        return new JoernEdge(type, content, src, dst);
    }

    private void dealRet(String line) {
        JoernNode jn = parseNode(line);
        if (line.indexOf("(METHOD_RETURN,void)") == -1) {
            begNode.lineNum = Integer.valueOf(jn.lineNum);
        }
        hasVoidRet = true;
        voidRetId = jn.id;
    }

    public JoernNode getGraphName() {
        return nodeMap.get(parseGraphName(lines[0]));
    }

    private void parseGraph() throws Exception {
        for (var line : lines) {
            if (line.indexOf("METHOD_RETURN") != -1) {
                dealRet(line);
            }
            if (Pattern.matches(re1, line)) {
                continue;
            } else if (Pattern.matches(re2, line)) {

                JoernNode n = parseNode(line);
                if (line.indexOf("(METHOD,") != -1) {
                    begNode = n;
                }
                addNode(n);
            } else if (Pattern.matches(re3, line)) {
                JoernEdge e = parseEdge(line);
                if (e != null) {
                    addEdge(e);
                }
            } else if (Pattern.matches(re4, line)) {
                continue;
            } else if (line == "") {
                continue;
            } else {
                throw new Exception(line + " does not follow format rule\n");
            }

        }

    }

    public ArrayList<JoernNode> getNodes() {
        return nodes;
    }

    public ArrayList<JoernEdge> getEdges() {
        return edges;
    }
}
