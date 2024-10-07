package com.mycompany.Project;

import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.mycompany.app.Unit;
import com.mycompany.genFinalGraph.Node;

public class Method {
    public final String name;
    public final String className;
    public final MethodDeclaration decl;
    public final ConstructorDeclaration cdecl;
    public final int pNum;
    public final Node firstNode;

    public Method(String name,
            String className,
            MethodDeclaration decl,
            int pNum,
            Node firstNode) {
        this.name = name;
        this.className = className;
        this.decl = decl;
        this.pNum = pNum;
        this.firstNode = firstNode;
        cdecl = null;
    }

    public Method(String name,
            String className,
            ConstructorDeclaration cd,
            int pNum,
            Node firstNode) {
        this.name = name;
        this.className = className;
        this.decl = null;
        this.pNum = pNum;
        this.firstNode = firstNode;
        cdecl = cd;
    }

    @Override
    public String toString() {
        return "method:" + name + ",in class " + className + " with " + pNum + " parameters";
    }
}
