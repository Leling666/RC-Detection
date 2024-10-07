package com.mycompany.mapping;

import com.github.gumtreediff.gen.TreeGenerators;

import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.mycompany.Project.SourceFile;
import com.mycompany.genFinalGraph.Node;
import com.github.gumtreediff.client.*;
import java.util.Queue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;

public class LineMapping {
    private HashSet<String> typeSet;
    private TreeContext tc1, tc2;
    private ArrayList<Tree> trees1, trees2;
    private GetPos gp1, gp2;
    private MappingStore ms;
    private SourceFile srcf1, srcf2;

    public LineMapping(SourceFile srcf1, SourceFile srcf2) {
        typeSet = new HashSet<>(Arrays.asList("ImportDeclaration",
                "PackageDeclaration",
                "ClassOrInterfaceDeclaration",
                "ConstructorDeclaration",
                "EnumConstantDeclaration",
                "EnumDeclaration",
                "FieldDeclaration",
                "InitializerDeclaration",
                "MethodDeclaration",
                "Expression",
                "AssertStmt",
                "BreakStmt",
                "CatchClause",
                "ContinueStmt",
                "DoStmt",
                "ExplicitConstructorInvocationStmt",
                "ExpressionStmt",
                "ForEachStmt",
                "ForStmt",
                "IfStmt",
                "LabeledStmt",
                "LocalClassDeclarationStmt",
                "ReturnStmt",
                "Statement",
                "SwitchEntry",
                "SwitchStmt",
                "SynchronizedStmt",
                "ThrowStmt",
                "TryStmt",
                "WhileStmt"));
        try {
            this.srcf1 = srcf1;
            this.srcf2 = srcf2;

            tc1 = new JavaParserGenerator().generateFrom().string(srcf1.getContent());
            tc2 = new JavaParserGenerator().generateFrom().string(srcf2.getContent());

            trees1 = new ArrayList<>();
            trees2 = new ArrayList<>();

            gp1 = new GetPos(srcf1.getContent());
            gp2 = new GetPos(srcf2.getContent());
            Matcher dMatcher = Matchers.getInstance().getMatcher();

            ms = dMatcher.match(tc1.getRoot(), tc2.getRoot());
            processTrees();
            genMappings();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processTrees() {
        Queue<Tree> q = new LinkedList<>();
        q.add(tc1.getRoot());
        while (!q.isEmpty()) {
            Tree t = q.poll();
            if (typeSet.contains(t.getType().name)) {   
                trees1.add(t);
            }
            for (var c : t.getChildren()) {
                q.add(c);
            }

        }
        trees1.sort(new TreeComparator(gp1));

        q.clear();
        q.add(tc2.getRoot());
        while (!q.isEmpty()) {
            Tree t = q.poll();
            if (typeSet.contains(t.getType().name)) {
                trees2.add(t);
            }
            for (var c : t.getChildren()) {
                q.add(c);
            }

        }
        trees2.sort(new TreeComparator(gp2));

    }

    private class TreeComparator implements Comparator<Tree> {
        GetPos gp;

        public TreeComparator(GetPos gp) {
            this.gp = gp;
        }

        @Override
        public int compare(Tree t1, Tree t2) {
            if (gp.getLineNum(t1.getPos()) > gp.getLineNum(t2.getPos())) {
                return 1;
            } else if (gp.getLineNum(t1.getPos()) == gp.getLineNum(t2.getPos())) {
                return 0;
            }
            return -1;
        }
    }

    // input:begin line of a node
    private int Upperbound(ArrayList<Tree> vector, int val, GetPos gp) {
        int l = 0, u = vector.size() - 1;
        while (l < u) {
            int midp = l + (u - l) / 2;
            int mid = gp.getLineNum(vector.get(midp).getPos());
            if (val < mid) {
                u = midp;
            } else {
                l = midp + 1;
            }
        }
        if (l >= vector.size()) {
            return vector.size();
        }

        return val < gp.getLineNum(vector.get(l).getPos()) ? l : vector.size();
    }

    // input:begin line of a tree
    private int Upperbound1(ArrayList<Node> vector, int val, GetPos gp) {
        int l = 0, u = vector.size() - 1;
        while (l < u) {
            int midp = l + (u - l) / 2;
            int mid = vector.get(midp).u.getBegPos().line;
            if (val < mid) {
                u = midp;
            } else {
                l = midp + 1;
            }
        }

        if (l >= vector.size()) {
            return vector.size();
        }

        return val < vector.get(l).u.getBegPos().line ? l : vector.size();
    }

    private Tree getTree(Node n, ArrayList<Tree> curTree, GetPos gp) {

        int pos = Upperbound(curTree, n.u.getBegPos().line, gp);
        // System.out.println("pos is " + pos);
        // if (pos > 0) {
        //     System.out.println("getTree output as following:");
        //     System.out.println(gp.getLineNum(curTree.get(pos - 1).getPos()));
        //     System.out.println(gp.getLineNum(curTree.get(pos - 1).getEndPos()));
        //     System.out.println(curTree.get(pos - 1).getType().name);
        // }

        int i = 1;
        for (; pos - i >= 0 && gp.getLineNum(curTree.get(pos - i).getEndPos()) >= n.u.getBegPos().line
                && gp.getLineNum(curTree.get(pos - i).getPos()) >= n.u.getBegPos().line; ++i)
            ;
        i--;
        if (pos - i < curTree.size() && pos - i >= 0
                && gp.getLineNum(curTree.get(pos - i).getEndPos()) >= n.u.getBegPos().line
                && gp.getLineNum(curTree.get(pos - i).getPos()) >= n.u.getBegPos().line) {
            return curTree.get(pos - i);
        }
        if (pos >= curTree.size()) {
            return null;
        }
        // if (pos > 0 && gp.getLineNum(curTree.get(pos - 1).getEndPos()) >=
        // n.u.getBegPos().line) {
        // return curTree.get(pos - 1);
        // } else
        if (n.u.getEndPos().line >= gp.getLineNum(curTree.get(pos).getPos())) {
            return curTree.get(pos);
        }
        return null;
    }

    private Node getNode(int begLine, int endLine, ArrayList<Node> nodes, GetPos gp) {
        int pos = Upperbound1(nodes, begLine, gp);
        // System.out.println("pos is " + pos);

        int i = 1;
        for (; pos - i >= 0 && nodes.get(pos - i).u.getEndPos().line >= begLine; ++i)
            ;
        i--;
        if (pos - i >= 0 && pos - i < nodes.size() && nodes.get(pos - i).u.getEndPos().line >= begLine) {
            return nodes.get(pos - i);
        }
        if (pos >= nodes.size()) {
            return null;
        }
        // if (pos > 0 && nodes.get(pos - 1).u.getEndPos().line >= begLine) {

        // return nodes.get(pos - 1);
        // } else
        if (endLine >= nodes.get(pos).u.getBegPos().line) {
            return nodes.get(pos);
        }
        return null;
    }

    private void genMappings() {
        for (var srcNode : srcf1.getFinalNodes()) {
            // System.out.println(srcNode.u.getCodeStr() + "(" + srcNode.u.getBegPos().line
            //         + ")");
            // System.out.println("curTree's size is " + trees1.size());
            // System.out.println("curTree's size is " + trees2.size());

            // System.out.println("begin to enter get tree");
            Tree t1 = getTree(srcNode, trees1, gp1);
            // if (t1 != null)
            //     System.out.println("result is " + t1 + ",type is " + t1.getType().name);
            // else {
            //     System.out.println("not get tree");
            // }
            if (ms.isSrcMapped(t1)) {
                // System.out.println("is mapped");
                Tree t2 = ms.getDstForSrc(t1);
                // System.out.println("result is " + t2 + ",type is " + t2.getType().name);
                Node dstNode = getNode(gp2.getLineNum(t2.getPos()), gp2.getLineNum(t2.getEndPos()),
                        srcf2.getFinalNodes(), gp2);
                // System.out.println(dstNode);
                if (dstNode != null && srcNode.u.getType().equals(dstNode.u.getType())
                        && srcNode.u.getType().equals(t1.getType().toString())) {
                    // System.out.println("mapped!");
                    srcNode.setMappingIndex(dstNode.index);
                    dstNode.setMappingIndex(srcNode.index);
                }
            }
        }
    }

    public void printSrcTrees() {
        for (var t : trees1) {
            System.out.println(t);
        }
    }

    public void printDstTrees() {
        for (var t : trees2) {
            System.out.println(t);
        }
    }

    public void printMappings(String type) {
        for (var t : trees1) {
            if (ms.isSrcMapped(t) && t.getType().name.equals(type)) {
                Tree t1 = ms.getDstForSrc(t);
                System.out.println(gp1.getLineNum(t.getPos()) + " -> " +
                        gp2.getLineNum(t1.getPos()));
                System.out.println(t.toString() + " -> " + t1.toString());
            }
        }
    }


}
