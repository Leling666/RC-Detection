package com.mycompany.app;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;

public class Extract {
    private static class assertStatementGetter extends VoidVisitorAdapter<List<AssertStmt>> {
        @Override
        public void visit(AssertStmt st, List<AssertStmt> collector) {
            super.visit(st, collector);
            collector.add(st);
        }
    }

    public static List<AssertStmt> getAssert(CompilationUnit cu) {
        List<AssertStmt> l = new ArrayList<>();
        VoidVisitorAdapter<List<AssertStmt>> v = new assertStatementGetter();
        v.visit(cu, l);
        return l;
    }

    private static class breakStmtGetter extends VoidVisitorAdapter<List<BreakStmt>> {
        @Override
        public void visit(BreakStmt bst, List<BreakStmt> collector) {
            super.visit(bst, collector);
            collector.add(bst);
        }
    }

    public static List<BreakStmt> getBreak(CompilationUnit cu) {
        List<BreakStmt> l = new ArrayList<>();
        VoidVisitorAdapter<List<BreakStmt>> v = new breakStmtGetter();
        v.visit(cu, l);
        return l;
    }

    private static class classOrInterfaceDeclarationGetter
            extends VoidVisitorAdapter<List<ClassOrInterfaceDeclaration>> {
        @Override
        public void visit(ClassOrInterfaceDeclaration cid, List<ClassOrInterfaceDeclaration> collector) {
            super.visit(cid, collector);
            collector.add(cid);
        }
    }

    public static List<ClassOrInterfaceDeclaration> getClassOrInterfaceDecl(CompilationUnit cu) {
        List<ClassOrInterfaceDeclaration> l = new ArrayList<>();
        VoidVisitorAdapter<List<ClassOrInterfaceDeclaration>> v = new classOrInterfaceDeclarationGetter();
        v.visit(cu, l);
        return l;
    }

    private static class constructorDeclarationGetter extends VoidVisitorAdapter<List<ConstructorDeclaration>> {
        @Override
        public void visit(ConstructorDeclaration cd, List<ConstructorDeclaration> collector) {
            super.visit(cd, collector);
            collector.add(cd);
        }
    }

    public static List<ConstructorDeclaration> getConstructorDecl(CompilationUnit cu) {
        List<ConstructorDeclaration> l = new ArrayList<>();
        VoidVisitorAdapter<List<ConstructorDeclaration>> v = new constructorDeclarationGetter();
        v.visit(cu, l);
        return l;
    }

    private static class doStmtGetter extends VoidVisitorAdapter<List<DoStmt>> {
        @Override
        public void visit(DoStmt ds, List<DoStmt> collector) {
            super.visit(ds, collector);
            collector.add(ds);
        }
    }

    public static List<DoStmt> getDostmt(CompilationUnit cu) {
        List<DoStmt> l = new ArrayList<>();
        VoidVisitorAdapter<List<DoStmt>> v = new doStmtGetter();
        v.visit(cu, l);
        return l;
    }

    private static class enumConstantDeclarationGetter extends VoidVisitorAdapter<List<EnumConstantDeclaration>> {
        @Override
        public void visit(EnumConstantDeclaration ecd, List<EnumConstantDeclaration> collector) {
            super.visit(ecd, collector);
            collector.add(ecd);
        }
    }

    public static List<EnumConstantDeclaration> getEnumConstantDeclaration(CompilationUnit cu) {
        List<EnumConstantDeclaration> l = new ArrayList<>();
        VoidVisitorAdapter<List<EnumConstantDeclaration>> v = new enumConstantDeclarationGetter();
        v.visit(cu, l);
        return l;
    }

    private static class enumDeclarationGetter extends VoidVisitorAdapter<List<EnumDeclaration>> {
        @Override
        public void visit(EnumDeclaration ecd, List<EnumDeclaration> collector) {
            super.visit(ecd, collector);
            collector.add(ecd);
        }
    }

    public static List<EnumDeclaration> getEnumDeclaration(CompilationUnit cu) {
        List<EnumDeclaration> l = new ArrayList<>();
        VoidVisitorAdapter<List<EnumDeclaration>> v = new enumDeclarationGetter();
        v.visit(cu, l);
        return l;
    }

    private static class explicitConstructorInvocationStmtGetter
            extends VoidVisitorAdapter<List<ExplicitConstructorInvocationStmt>> {
        @Override
        public void visit(ExplicitConstructorInvocationStmt ecd, List<ExplicitConstructorInvocationStmt> collector) {
            super.visit(ecd, collector);
            collector.add(ecd);
        }
    }

    public static List<ExplicitConstructorInvocationStmt> getExplicitConstructorInvocationStmt(CompilationUnit cu) {
        List<ExplicitConstructorInvocationStmt> l = new ArrayList<>();
        VoidVisitorAdapter<List<ExplicitConstructorInvocationStmt>> v = new explicitConstructorInvocationStmtGetter();
        v.visit(cu, l);
        return l;
    }

    private static class expressionStmtGetter extends VoidVisitorAdapter<List<ExpressionStmt>> {
        @Override
        public void visit(ExpressionStmt ecd, List<ExpressionStmt> collector) {
            super.visit(ecd, collector);
            collector.add(ecd);
        }
    }

    public static List<ExpressionStmt> getExpressionStmt(CompilationUnit cu) {
        List<ExpressionStmt> l = new ArrayList<>();
        VoidVisitorAdapter<List<ExpressionStmt>> v = new expressionStmtGetter();
        v.visit(cu, l);
        return l;
    }

    private static class fieldDeclarationGetter extends VoidVisitorAdapter<List<FieldDeclaration>> {
        @Override
        public void visit(FieldDeclaration ecd, List<FieldDeclaration> collector) {
            super.visit(ecd, collector);
            collector.add(ecd);
        }
    }

    public static List<FieldDeclaration> getFieldDeclaration(CompilationUnit cu) {
        List<FieldDeclaration> l = new ArrayList<>();
        VoidVisitorAdapter<List<FieldDeclaration>> v = new fieldDeclarationGetter();
        v.visit(cu, l);
        return l;
    }

    private static class foreachStmtGetter extends VoidVisitorAdapter<List<ForEachStmt>> {
        @Override
        public void visit(ForEachStmt ecd, List<ForEachStmt> collector) {
            super.visit(ecd, collector);
            collector.add(ecd);
        }
    }

    public static List<ForEachStmt> getForEachStmt(CompilationUnit cu) {
        List<ForEachStmt> l = new ArrayList<>();
        VoidVisitorAdapter<List<ForEachStmt>> v = new foreachStmtGetter();
        v.visit(cu, l);
        return l;
    }

    private static class forStmtGetter extends VoidVisitorAdapter<List<ForStmt>> {
        @Override
        public void visit(ForStmt ecd, List<ForStmt> collector) {
            super.visit(ecd, collector);
            collector.add(ecd);
        }
    }

    public static List<ForStmt> getForStmt(CompilationUnit cu) {
        List<ForStmt> l = new ArrayList<>();
        VoidVisitorAdapter<List<ForStmt>> v = new forStmtGetter();
        v.visit(cu, l);
        return l;
    }

    private static class ifStmtGetter extends VoidVisitorAdapter<List<IfStmt>> {
        @Override
        public void visit(IfStmt ecd, List<IfStmt> collector) {
            super.visit(ecd, collector);
            collector.add(ecd);
        }
    }

    public static List<IfStmt> getIfStmt(CompilationUnit cu) {
        List<IfStmt> l = new ArrayList<>();
        VoidVisitorAdapter<List<IfStmt>> v = new ifStmtGetter();
        v.visit(cu, l);
        return l;
    }

    private static class ImportDeclarationGetter extends VoidVisitorAdapter<List<ImportDeclaration>> {
        @Override
        public void visit(ImportDeclaration id, List<ImportDeclaration> collector) {
            super.visit(id, collector);
            collector.add(id);
        }
    }

    public static List<ImportDeclaration> getImportDeclaration(CompilationUnit cu) {
        List<ImportDeclaration> l = new ArrayList<>();
        VoidVisitorAdapter<List<ImportDeclaration>> v = new ImportDeclarationGetter();
        v.visit(cu, l);
        return l;
    }

    private static class initializerDeclarationGetter extends VoidVisitorAdapter<List<InitializerDeclaration>> {
        @Override
        public void visit(InitializerDeclaration id, List<InitializerDeclaration> collector) {
            super.visit(id, collector);
            collector.add(id);
        }
    }

    public static List<InitializerDeclaration> getInitializerDeclaration(CompilationUnit cu) {
        List<InitializerDeclaration> l = new ArrayList<>();
        VoidVisitorAdapter<List<InitializerDeclaration>> v = new initializerDeclarationGetter();
        v.visit(cu, l);
        return l;
    }

    private static class labeledStmtGetter extends VoidVisitorAdapter<List<LabeledStmt>> {
        @Override
        public void visit(LabeledStmt id, List<LabeledStmt> collector) {
            super.visit(id, collector);
            collector.add(id);
        }
    }

    public static List<LabeledStmt> getLabeledStmt(CompilationUnit cu) {
        List<LabeledStmt> l = new ArrayList<>();
        VoidVisitorAdapter<List<LabeledStmt>> v = new labeledStmtGetter();
        v.visit(cu, l);
        return l;
    }

    private static class localClassDeclarationStmtGetter extends VoidVisitorAdapter<List<LocalClassDeclarationStmt>> {
        @Override
        public void visit(LocalClassDeclarationStmt id, List<LocalClassDeclarationStmt> collector) {
            super.visit(id, collector);
            collector.add(id);
        }
    }

    public static List<LocalClassDeclarationStmt> getLocalClassDeclarationStmt(CompilationUnit cu) {
        List<LocalClassDeclarationStmt> l = new ArrayList<>();
        VoidVisitorAdapter<List<LocalClassDeclarationStmt>> v = new localClassDeclarationStmtGetter();
        v.visit(cu, l);
        return l;
    }

    private static class methodDeclarationGetter extends VoidVisitorAdapter<List<MethodDeclaration>> {
        @Override
        public void visit(MethodDeclaration id, List<MethodDeclaration> collector) {
            super.visit(id, collector);
            collector.add(id);
        }
    }

    public static List<MethodDeclaration> getMethodDeclaration(CompilationUnit cu) {
        List<MethodDeclaration> l = new ArrayList<>();
        VoidVisitorAdapter<List<MethodDeclaration>> v = new methodDeclarationGetter();
        v.visit(cu, l);
        return l;
    }

    private static class packageDeclarationGetter extends VoidVisitorAdapter<List<PackageDeclaration>> {
        @Override
        public void visit(PackageDeclaration id, List<PackageDeclaration> collector) {
            super.visit(id, collector);
            collector.add(id);
        }
    }

    public static List<PackageDeclaration> getPackageDeclaration(CompilationUnit cu) {
        List<PackageDeclaration> l = new ArrayList<>();
        VoidVisitorAdapter<List<PackageDeclaration>> v = new packageDeclarationGetter();
        v.visit(cu, l);
        return l;
    }

    private static class returnStmtGetter extends VoidVisitorAdapter<List<ReturnStmt>> {
        @Override
        public void visit(ReturnStmt id, List<ReturnStmt> collector) {
            super.visit(id, collector);
            collector.add(id);
        }
    }

    public static List<ReturnStmt> getReturnStmt(CompilationUnit cu) {
        List<ReturnStmt> l = new ArrayList<>();
        VoidVisitorAdapter<List<ReturnStmt>> v = new returnStmtGetter();
        v.visit(cu, l);
        return l;
    }

    private static class switchStmtGetter extends VoidVisitorAdapter<List<SwitchStmt>> {
        @Override
        public void visit(SwitchStmt id, List<SwitchStmt> collector) {
            super.visit(id, collector);
            collector.add(id);
        }
    }

    public static List<SwitchStmt> getSwitchStmt(CompilationUnit cu) {
        List<SwitchStmt> l = new ArrayList<>();
        VoidVisitorAdapter<List<SwitchStmt>> v = new switchStmtGetter();
        v.visit(cu, l);
        return l;
    }

    private static class synchronizedStmtGetter extends VoidVisitorAdapter<List<SynchronizedStmt>> {
        @Override
        public void visit(SynchronizedStmt id, List<SynchronizedStmt> collector) {
            super.visit(id, collector);
            collector.add(id);
        }
    }

    public static List<SynchronizedStmt> getSynchronizedStmt(CompilationUnit cu) {
        List<SynchronizedStmt> l = new ArrayList<>();
        VoidVisitorAdapter<List<SynchronizedStmt>> v = new synchronizedStmtGetter();
        v.visit(cu, l);
        return l;
    }

    private static class throwStmtStmtGetter extends VoidVisitorAdapter<List<ThrowStmt>> {
        @Override
        public void visit(ThrowStmt id, List<ThrowStmt> collector) {
            super.visit(id, collector);
            collector.add(id);
        }
    }

    public static List<ThrowStmt> getThrowStmt(CompilationUnit cu) {
        List<ThrowStmt> l = new ArrayList<>();
        VoidVisitorAdapter<List<ThrowStmt>> v = new throwStmtStmtGetter();
        v.visit(cu, l);
        return l;
    }

    private static class tryStmtGetter extends VoidVisitorAdapter<List<TryStmt>> {
        @Override
        public void visit(TryStmt id, List<TryStmt> collector) {
            super.visit(id, collector);
            collector.add(id);
        }
    }

    public static List<TryStmt> getTryStmt(CompilationUnit cu) {
        List<TryStmt> l = new ArrayList<>();
        VoidVisitorAdapter<List<TryStmt>> v = new tryStmtGetter();
        v.visit(cu, l);
        return l;
    }

    private static class whileStmtGetter extends VoidVisitorAdapter<List<WhileStmt>> {
        @Override
        public void visit(WhileStmt id, List<WhileStmt> collector) {
            super.visit(id, collector);
            collector.add(id);
        }
    }

    public static List<WhileStmt> getWhileStmt(CompilationUnit cu) {
        List<WhileStmt> l = new ArrayList<>();
        VoidVisitorAdapter<List<WhileStmt>> v = new whileStmtGetter();
        v.visit(cu, l);
        return l;
    }
}
