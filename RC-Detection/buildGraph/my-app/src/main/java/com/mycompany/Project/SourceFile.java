package com.mycompany.Project;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.mycompany.app.GenUnits;
import com.mycompany.app.Unit;
import com.mycompany.genFinalGraph.GenGraph;
import com.mycompany.genFinalGraph.Node;
import com.mycompany.genFinalGraph.TrimGraph;
import com.mycompany.parseJoernDot.JoernEdge;
import com.mycompany.parseJoernDot.JoernGraph;
import com.mycompany.parseJoernDot.JoernNode;
import com.mycompany.parsePatch.PatchLine;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class SourceFile {
    private ArrayList<MethodDeclaration> methodDecls;
    private ArrayList<FieldDeclaration> fieldDecls;
    private ArrayList<ConstructorDeclaration> consDecls;
    private ArrayList<Unit> units;
    private String fileName;

    private String content;
    private String[] fContent;
    private CompilationUnit cu;
    private ArrayList<JoernNode> nodes;
    private ArrayList<JoernEdge> edges;
    private ArrayList<Node> finalNodes;
    private ArrayList<Method> finalMethods;
    private ArrayList<Call> finalCalls;

    public SourceFile(String path, String jPath, int nodeIndex, ArrayList<PatchLine> plines, String fName) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(path));

            this.content = new String(bytes, StandardCharsets.UTF_8);
            fContent = content.split("\n");

            cu = StaticJavaParser.parse(new File(path));
            units = new ArrayList<>();
            methodDecls = new ArrayList<>();
            fieldDecls = new ArrayList<>();
            consDecls = new ArrayList<>();
            nodes = new ArrayList<>();
            edges = new ArrayList<>();
            finalNodes = new ArrayList<>();
            finalMethods = new ArrayList<>();
            finalCalls = new ArrayList<>();

            fileName = fName;

            GenUnits g = new GenUnits(fContent, units, cu);
            units = g.getAllUnits();

            getAllFieldDeclaration();
            getAllMethodDeclaration();
            getAllConstructorDeclaration();

            parseJoernDot(jPath);
            trimGraph(plines, nodeIndex);

            genFieldRef();
            filterMethod();
            genCalls();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class MethodDeclarationComparator implements Comparator<MethodDeclaration> {
        @Override
        public int compare(MethodDeclaration m1, MethodDeclaration m2) {
            if (m1.getBegin().get().line < m2.getBegin().get().line) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    private static class FieldDeclarationComparator implements Comparator<FieldDeclaration> {
        @Override
        public int compare(FieldDeclaration f1, FieldDeclaration f2) {
            if (f1.getBegin().get().line < f2.getBegin().get().line) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    private static class ConstructorDeclarationComparator implements Comparator<ConstructorDeclaration> {
        @Override
        public int compare(ConstructorDeclaration c1, ConstructorDeclaration c2) {
            if (c1.getBegin().get().line < c2.getBegin().get().line) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    private void getAllMethodDeclaration() {
        cu.findAll(MethodDeclaration.class).forEach((md) -> {
            methodDecls.add(md);
        });
        ;
        methodDecls.sort(new MethodDeclarationComparator());
    }

    private void getAllFieldDeclaration() {
        cu.findAll(FieldDeclaration.class).forEach((fd) -> {
            fieldDecls.add(fd);
        });

        fieldDecls.sort(new FieldDeclarationComparator());
    }

    private void getAllConstructorDeclaration() {
        cu.findAll(ConstructorDeclaration.class).forEach((cd) -> {
            consDecls.add(cd);
        });

        consDecls.sort(new ConstructorDeclarationComparator());
    }

    private void parseJoernDot(String jPath) {
        File file = new File(jPath);
        File[] fileList = file.listFiles();

        for (int i = 0; i < fileList.length; ++i) {
            if (fileList[i].isFile()) {

                String fName = fileList[i].getName();
                fName = fName.replaceAll("\\.", "_");
                int index = fName.lastIndexOf("_");
                if (index == -1) {
                    continue;
                }
                fName = fName.substring(0, index);
                // if(fName.split("$").length>1){
                // System.out.println("begin to deal subclass");
                // System.out.println(fName);
                // }
                fName = fName.split("\\$")[0];
                String sFileName = fName + ".java";
                // System.out.println(sFileName);
                // System.out.println(fileName);
                if (fileName.indexOf(sFileName) != -1 || sFileName.indexOf(fileName) != -1) {
                    // System.out.println("begin to parse");
                    JoernGraph jg = new JoernGraph(fileList[i].getAbsolutePath());
                    assert nodes.addAll(jg.getNodes());
                    assert edges.addAll(jg.getEdges());
                }

            }
        }
    }

    private void trimGraph(ArrayList<PatchLine> plines, int begIndex) {
        var nodes1 = new GenGraph(nodes, edges, units, fileName).getNodes();
        finalNodes = new TrimGraph(plines, nodes1, begIndex, fileName).getFinalGraph();
    }

    public ArrayList<Node> getFinalNodes() {
        return finalNodes;
    }

    private Node getNode(int begLine, int endLine) {
        int l = 0, r = finalNodes.size() - 1;

        while (l < r) {
            int mid = (l + r) / 2;
            if (finalNodes.get(mid).u.getBegPos().line >= begLine) {
                r = mid;
            } else {
                l = mid + 1;
            }
        }

        if (l == finalNodes.size()) {
            return null;
        }

        if (finalNodes.get(l).u.getBegPos().line >= begLine && finalNodes.get(l).u.getBegPos().line <= endLine) {
            return finalNodes.get(l);
        }
        return null;
    }

    private void filterMethod() {
        for (var md : methodDecls) {
            Node n = getNode(md.getBegin().get().line, md.getEnd().get().line);
            if (n != null) {
                finalMethods.add(new Method(md.getName().asString(), fileName, md, md.getParameters().size(), n));
            }
        }

        for (var cd : consDecls) {
            Node n = getNode(cd.getBegin().get().line, cd.getEnd().get().line);
            if (n != null) {
                finalMethods.add(new Method(cd.getNameAsString(), fileName, cd, cd.getParameters().size(), n));
            }
        }
    }

    private void genFieldRef() {
        // System.out.println("begin to print all nodes");
        // for (var n : finalNodes) {
        // System.out.println(n.u.getCodeStr());
        // }
        // System.out.println("finish printing all nodes");

        // System.out.println("begin to gen field ref");
        for (var fd : fieldDecls) {
            Node n = getNode(fd.getBegin().get().line, fd.getEnd().get().line);
            if (n != null) {
                // System.out.println(n.u.getCodeStr());
                for (var n_ : finalNodes) {
                    if (n_ != n && n_.u.getType().contains("Stmt")) {
                        for (var v : fd.getVariables()) {

                            if (n_.u.getCodeStr().contains(v.getName().asString())) {
                                if (n_.fieldParents.isEmpty()) {
                                    n_.fieldParents.add(n.index);
                                    n.fieldEdges.add(n_.index);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void genCalls() {
        // MethodCallExpr

        // ObjectCreationExpr

        var nodes1 = cu.findAll(MethodCallExpr.class);

        var nodes2 = cu.findAll(ObjectCreationExpr.class);

        for (var n : nodes1) {
            var n_ = getNode(n.getBegin().get().line, n.getEnd().get().line);
            if (n_ != null) {
                finalCalls.add(new Call(n.getNameAsString(), n.getArguments().size(), fileName, n_));
            }
        }

        for (var n : nodes2) {
            var n_ = getNode(n.getBegin().get().line, n.getEnd().get().line);
            if (n_ != null) {
                finalCalls.add(new Call(n.getTypeAsString(), n.getArguments().size(), fileName, n_));
            }
        }

    }

    public ArrayList<Method> getMethods() {
        return finalMethods;
    }

    public ArrayList<Node> getNodes() {
        return finalNodes;
    }

    public ArrayList<Call> getCalls() {
        return finalCalls;
    }

    public String getContent() {
        return content;
    }

    public String getFileName() {
        return fileName;
    }

}
