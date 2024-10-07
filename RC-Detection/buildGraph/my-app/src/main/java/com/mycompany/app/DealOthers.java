package com.mycompany.app;

import java.util.List;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

public class DealOthers {
    public static void dealWithImport(ImportDeclaration id, List<Unit> allPairs) {
        Unit u = new Unit(id.getBegin().get(), id.getEnd().get(), ImportDeclaration.class.getSimpleName(), ';');

        allPairs.add(u);
    }

    public static void dealWithFieldDecl(FieldDeclaration fd, List<Unit> allPairs) {

        Unit u = new Unit(fd.getBegin().get(), fd.getEnd().get(), FieldDeclaration.class.getSimpleName(), ';');

        if (!fd.getAnnotations().isEmpty()) {
            u.setHasAnnotation(true);
            u.setBegPos(fd.getAnnotations().getLast().get().getEnd().get());
        }
        allPairs.add(u);
    }

    public static void dealWithPackageDecl(PackageDeclaration pd, List<Unit> allPairs) {
        Unit u = new Unit(pd.getBegin().get(), pd.getEnd().get(), PackageDeclaration.class.getSimpleName(), ';');

        allPairs.add(u);
    }

    public static void dealWithMethodDecl(MethodDeclaration md, List<Unit> allPairs) {
        if (md.getBody().isPresent()) {
            Unit u = new Unit(md.getBegin().get(), md.getBody().get().getBegin().get(),
                    MethodDeclaration.class.getSimpleName(), '{');
            u.setSkipAhead(false);

            if (!md.getAnnotations().isEmpty()) {
                u.setHasAnnotation(true);
                u.setBegPos(md.getAnnotations().getLast().get().getEnd().get());
            }

            allPairs.add(u);

            DealStatement.dealWithStmt(md.getBody().get(), allPairs);
            return;
        }
        Unit u = new Unit(md.getBegin().get(), md.getEnd().get(), MethodDeclaration.class.getSimpleName(), ';');

        if (!md.getAnnotations().isEmpty()) {
            u.setHasAnnotation(true);
            u.setBegPos(md.getAnnotations().getLast().get().getEnd().get());
        }

        allPairs.add(u);
    }

    public static void dealWithConstructorDecl(ConstructorDeclaration cd, List<Unit> allPairs) {
        Unit u = new Unit(cd.getBegin().get(), cd.getBody().getBegin().get(),
                ConstructorDeclaration.class.getSimpleName(), '{');
        u.setSkipAhead(false);
        allPairs.add(u);
    }

    public static void dealWithClassOrInterfaceDecl(ClassOrInterfaceDeclaration cd, List<Unit> allPairs) {

        Unit u = new Unit(cd.getBegin().get(), null, ClassOrInterfaceDeclaration.class.getSimpleName(), '{');
        u.setSkipAhead(true);

        if (!cd.getAnnotations().isEmpty()) {
            u.setHasAnnotation(true);
            u.setBegPos(cd.getAnnotations().getLast().get().getEnd().get());
        }

        allPairs.add(u);
    }

    public static void dealWithEnumDecl(EnumDeclaration ed, List<Unit> allPairs) {
        Unit u = new Unit(ed.getBegin().get(), null, EnumDeclaration.class.getSimpleName(), '{');
        u.setSkipAhead(true);
        allPairs.add(u);

        List<FieldDeclaration> l = ed.getFields();
        for (var e : l) {
            dealWithFieldDecl(e, allPairs);
        }
    }

    public static void dealWithEnumConstDecl(EnumConstantDeclaration ecd, List<Unit> allPairs) {
        Unit u = new Unit(ecd.getBegin().get(), ecd.getEnd().get(), EnumConstantDeclaration.class.getSimpleName(),
                '}');
        u.setNotSkip(true);
        allPairs.add(u);
    }

    public static void dealWithInitializerDecl(InitializerDeclaration id, List<Unit> allPairs) {
        DealStatement.dealWithStmt(id.getBody(), allPairs);
    }
}
