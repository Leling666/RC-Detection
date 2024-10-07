package com.mycompany.Project;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import com.alibaba.fastjson2.JSON;
import com.mycompany.genFinalGraph.Node;
import com.mycompany.mapping.LineMapping;
import com.mycompany.parsePatch.Patch;

public class UnionProject {
    private String bpath, bjpath, apath, ajpath, pPath, tPath, fixPath;
    private Project p1, p2;
    private ArrayList<Node> finalNodes;

    public UnionProject(String path) {
        bpath = path + "/before";
        bjpath = path + "/before/joern";

        apath = path + "/after";
        ajpath = path + "/after/joern";

        fixPath = path + "/fixing";
        File[] fileList = new File(fixPath).listFiles();

        pPath = fileList[0].getAbsolutePath();
        tPath = path + "/test/";

        Patch p = new Patch(pPath);
        // System.out.println("begin to deal before path");
        p1 = new Project(bpath, p, bjpath, false);
        // System.out.println("begin to deal after path");
        p2 = new Project(apath, p, ajpath, true);

        int begIndex = p1.getFinalNodes().size();
        adjustIndex(begIndex, p2.getFinalNodes());
        genMapping();
        finalNodes = new ArrayList<>();

        for (var n : p1.getFinalNodes()) {
            var n_ = n.clone();
            n_.isDel = true;
            finalNodes.add(n_);
        }

        for (var n : p2.getFinalNodes()) {
            finalNodes.add(n.clone());
        }

        adjustIndex(-begIndex, p2.getFinalNodes());
    }

    private void adjustIndex(int begIndex, ArrayList<Node> allNodes) {
        for (var node : allNodes) {
            node.index = begIndex + node.index;
            for (int i = 0; i < node.cfgEdges.size(); ++i) {
                node.cfgEdges.set(i, node.cfgEdges.get(i) + begIndex);
            }
            for (int i = 0; i < node.cfgParents.size(); ++i) {
                node.cfgParents.set(i, node.cfgParents.get(i) + begIndex);
            }
            for (int i = 0; i < node.dfgEdges.size(); ++i) {
                node.dfgEdges.set(i, node.dfgEdges.get(i) + begIndex);
            }
            for (int i = 0; i < node.dfgParents.size(); ++i) {
                node.dfgParents.set(i, node.dfgParents.get(i) + begIndex);
            }
            for (int i = 0; i < node.fieldEdges.size(); ++i) {
                node.fieldEdges.set(i, node.fieldEdges.get(i) + begIndex);
            }
            for (int i = 0; i < node.fieldParents.size(); ++i) {
                node.fieldParents.set(i, node.fieldParents.get(i) + begIndex);
            }
            for (int i = 0; i < node.methodParents.size(); ++i) {
                node.methodParents.set(i, node.methodParents.get(i) + begIndex);
            }
            for (int i = 0; i < node.methodEdges.size(); ++i) {
                node.methodEdges.set(i, node.methodEdges.get(i) + begIndex);
            }
        }
    }

    private void genMapping() {

        for (var srcf1 : p1.getSourceFiles()) {
            for (var srcf2 : p2.getSourceFiles()) {

                if (srcf1.getFileName().equals(srcf2.getFileName())) {
                    // System.out.println("begin to gen linemapping fo r
                    // "+srcf1.getSimpleFileName());
                    LineMapping lp = new LineMapping(srcf1, srcf2);
                    // lp.printMappings("IfStmt");;
                }

            }
        }
    }

    public ArrayList<Node> getFinalNodes() {
        return finalNodes;
    }

    public String toLineMappingDot() {
        StringBuffer sb = new StringBuffer();
        sb.append("digraph " + "LineMapping" + " {\n");
        for (var n : finalNodes) {
            if (n.getMappingIndex() != -1)
                sb.append(
                        String.format("  %d [label=\"%s(%d-%d)\"];\n", n.index, n.u.getCodeStr().replace("\"", "\\\""),
                                n.u.getBegPos().line,
                                n.u.getEndPos().line));
        }
        for (var n : finalNodes) {
            if (n.getMappingIndex() != -1) {
                sb.append(String.format("  %d -> %d [color=purple];\n", n.index, n.getMappingIndex()));
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    public String toDot(String name) {
        StringBuffer sb = new StringBuffer();
        sb.append("digraph " + name + " {\n");
        for (var n : finalNodes) {
            sb.append(String.format("  %d [label=\"%s(%d-%d)\"];\n", n.index, n.u.getCodeStr().replace("\"", "\\\""),
                    n.u.getBegPos().line,
                    n.u.getEndPos().line));
        }
        for (var n : finalNodes) {
            for (var e : n.cfgEdges) {
                sb.append(String.format("  %d -> %d;\n", n.index, finalNodes.get(e).index));
            }
            for (var e : n.dfgEdges) {
                sb.append(String.format("  %d -> %d [style=dotted];\n", n.index, finalNodes.get(e).index));
            }
            for (var e : n.fieldParents) {
                sb.append(String.format("  %d -> %d [color=blue];\n", n.index, finalNodes.get(e).index));
            }
            for (var e : n.methodParents) {
                sb.append(String.format("  %d -> %d [color=red];\n", n.index, finalNodes.get(e).index));
            }
            if (n.getMappingIndex() != -1) {
                sb.append(String.format("  %d -> %d [color=purple];\n", n.index, n.getMappingIndex()));
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    public void writeFile(String content, String path) {
        try {
            FileOutputStream fos = new FileOutputStream(path);
            fos.write(content.getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    public void testLineMappingG() {
        String graphName = "line_mapping.dot";
        StringBuffer sb = new StringBuffer();
        sb.append("digraph lineMapping {\n");

        for (var n : finalNodes) {
            if (n.getMappingIndex() != -1) {
                sb.append(
                        String.format("  %d [label=\"%s(%d-%d)\"];\n", n.index, n.u.getCodeStr().replace("\"", "\\\""),
                                n.u.getBegPos().line,
                                n.u.getEndPos().line));
            }
        }
        for (var n : finalNodes) {
            if (n.getMappingIndex() != -1) {
                sb.append(String.format("  %d -> %d [color=purple];\n", n.index, n.getMappingIndex()));
            }
        }
        sb.append("}\n");
        writeFile(sb.toString(), graphName);
    }

    class JsonNode {
        public String code;
        public int nodeIndex;
        public ArrayList<Integer> cfgs;
        public ArrayList<Integer> dfgs;
        public ArrayList<Integer> fieldParents;
        public ArrayList<Integer> methodParents;
        public boolean isDel;
        public String fName;
        public int lineMapIndex;
        public int lineBeg;
        public int lineEnd;

        public JsonNode(String code_,
                int nodeIndex_,
                ArrayList<Integer> cfgs_,
                ArrayList<Integer> dfgs_,
                ArrayList<Integer> fieldParents_,
                ArrayList<Integer> methodParents_, boolean isDel_, String fName_, int lineMapIndex_, int lineBeg_,
                int lineEnd_) {
            code = code_;
            nodeIndex = nodeIndex_;
            cfgs = cfgs_;
            dfgs = dfgs_;
            fieldParents = fieldParents_;
            methodParents = methodParents_;
            isDel = isDel_;
            fName = fName_;
            lineMapIndex = lineMapIndex_;
            lineBeg = lineBeg_;
            lineEnd = lineEnd_;
        }
    }

    public String toJson() {
        ArrayList<JsonNode> jnodes = new ArrayList<>();
        for (var n : finalNodes) {
            jnodes.add(new JsonNode(n.u.getCodeStr(), n.index, n.cfgEdges, n.dfgEdges, n.fieldParents, n.methodParents,
                    n.isDel, n.fName, n.getMappingIndex(), n.u.getBegPos().line, n.u.getEndPos().line));
        }
        return JSON.toJSONString(jnodes);
    }

    public void writeJson(String path) {
        writeFile(toJson(), path);
    }
}
