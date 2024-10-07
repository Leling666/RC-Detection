package com.mycompany.Project;

import java.io.File;
import java.util.ArrayList;

import com.mycompany.genFinalGraph.Node;
import com.mycompany.parsePatch.Patch;
import com.mycompany.parsePatch.PatchLine;

public class Project {
    private ArrayList<SourceFile> sourceFiles;
    private ArrayList<Method> methods;
    private ArrayList<Call> allCalls;

    private ArrayList<Node> finalNodes;
    private boolean isAdd;

    public Project(String path, Patch p, String joernPath, boolean isAdd) {
        sourceFiles = new ArrayList<SourceFile>();
        finalNodes = new ArrayList<>();
        methods = new ArrayList<>();
        allCalls = new ArrayList<>();
        this.isAdd = isAdd;
        File file = new File(path);
        File[] fileList = file.listFiles();
        int index = 0;
        for (int i = 0; i < fileList.length; ++i) {
            ArrayList<PatchLine> ps = null;
            if (fileList[i].isFile()) {
                if (fileList[i].getAbsolutePath().indexOf(".java") == -1) {
                    continue;
                }
                if (isAdd) {
                    System.out.println("add:" + fileList[i].getName());
                    if (p.isSpecial(fileList[i].getName())) {
                        continue;
                    }
                    ps = p.getAddLines(fileList[i].getName());
                    assert ps != null;
                } else {
                    System.out.println("del:" + fileList[i].getName());
                    if (p.isSpecial(fileList[i].getName())) {
                        continue;
                    }
                    ps = p.getDelLine(fileList[i].getName());
                    assert ps != null;
                }
                SourceFile curSf = new SourceFile(fileList[i].getAbsolutePath(), joernPath, index, ps,
                        fileList[i].getName());
                sourceFiles.add(curSf);
                index = index + curSf.getFinalNodes().size();
                finalNodes.addAll(curSf.getFinalNodes());

                methods.addAll(curSf.getMethods());
                allCalls.addAll(curSf.getCalls());
            }
        }
        genCallRef();
    }

    private void genCallRef() {
        // System.out.println("begin to print all calls");

        for (var call : allCalls) {
            // System.out.println(call.n.u.getCodeStr() + " " + call.fname);
            for (var m : methods) {
                if (call.cname.equals(m.name) && call.pNum == m.pNum) {
                    if (call.n.methodParents.isEmpty()) {
                        call.n.methodParents.add(m.firstNode.index);
                    }
                    if (!m.firstNode.methodEdges.contains(call.n.index))
                        m.firstNode.methodEdges.add(call.n.index);

                }
            }
        }
    }

    public ArrayList<Node> getFinalNodes() {
        return finalNodes;
    }

    public ArrayList<SourceFile> getSourceFiles() {
        return sourceFiles;
    }

}
