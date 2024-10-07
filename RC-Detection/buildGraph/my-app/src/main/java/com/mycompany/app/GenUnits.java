package com.mycompany.app;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import com.github.javaparser.Position;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

public class GenUnits {
    String[] fContent;
    ArrayList<Unit> units;
    CompilationUnit cu;

    public GenUnits(String[] fc, ArrayList<Unit> us, CompilationUnit c) {

        fContent = fc;
        cu = c;
        units = us;
        genUnits();
        genCodeForUnits();
        removeDup();

    }

    private Position skipAhead(Position p) throws Exception {
        Position p_ = null;
        if (p.column == fContent[p.line - 1].length() || fContent[p.line - 1].length() == 0) {
            p_ = new Position(p.line + 1, 1);
        } else {
            p_ = new Position(p.line, p.column + 1);
        }
        if (!isValid(p_)) {
            throw new Exception("position p_ with line:" + p_.line + " and column:" + p.column + " is invalid\n");
        }
        return p_;
    }

    private Position skipBack(Position p) throws Exception {
        Position p_ = null;
        if (p.column == 1 || fContent[p.line - 1].length() == 0) {
            p_ = new Position(p.line - 1, fContent[p.line - 2].length());
        } else {
            p_ = new Position(p.line, p.column - 1);
        }
        if (!isValid(p_)) {
            throw new Exception("position p_ with line:" + p_.line + " and column:" + p.column + " is invalid\n");
        }
        return p_;
    }

    private Position skipAheadTo(Position p, char c) throws Exception {
        while (fContent[p.line - 1].length() == 0) {
            p = skipAhead(p);
        }
        while (getChar(p) != c) {
            p = skipAhead(p);
            while (fContent[p.line - 1].length() == 0) {
                p = skipAhead(p);
            }
        }
        return p;
    }

    private Position skipBackTo(Position p, char c) throws Exception {
        while (fContent[p.line - 1].length() == 0) {
            p = skipBack(p);
        }
        while (getChar(p) != c) {
            p = skipBack(p);
            while (fContent[p.line - 1].length() == 0) {
                p = skipBack(p);
            }
        }
        return p;
    }

    private Position skipToNonEmpty(Position p) throws Exception {
        while (fContent[p.line - 1].length() == 0) {
            p = skipAhead(p);
        }
        while (getChar(p) == '\t' || getChar(p) == ' ') {
            p = skipAhead(p);
            while (fContent[p.line - 1].length() == 0) {
                p = skipAhead(p);
            }
        }
        return p;
    }

    private String getCode(Position p1, Position p2) {
        String ret = "";
        if (p1.line == p2.line) {
            ret = fContent[p1.line - 1].substring(p1.column - 1, p2.column);
            return ret;
        }

        for (int i = p1.line; i <= p2.line; ++i) {
            try {
                if (i == p1.line) {
                    ret = ret + fContent[p1.line - 1].substring(p1.column - 1);
                } else if (i == p2.line) {
                    ret = ret + fContent[p2.line - 1].substring(0, p2.column).strip();
                } else {
                    ret = ret + fContent[i - 1].strip();
                }
            } catch (Exception e) {
                System.out.println("exception " + e + " occured during handling the following statement:");
                System.out.println(fContent[i]);
            }

        }
        return ret;
    }

    // notice
    private char getChar(Position p) {
        return fContent[p.line - 1].charAt(p.column - 1);
    }

    private boolean isValid(Position p) {
        if (p.line < 1 && p.line > fContent.length) {
            return false;
        } else if (p.column < 1 && p.column > fContent[p.line - 1].length()) {
            return false;
        }
        return true;
    }

    private void genUnits() {
        var asserts = Extract.getAssert(cu);
        for (var a : asserts) {
            DealStatement.dealWithStmt(a, units);
        }

        var breaks = Extract.getBreak(cu);
        for (var b : breaks) {
            DealStatement.dealWithStmt(b, units);
        }

        var classOrInterfaceDecls = Extract.getClassOrInterfaceDecl(cu);
        for (var c : classOrInterfaceDecls) {
            DealOthers.dealWithClassOrInterfaceDecl(c, units);
        }

        var constructDecls = Extract.getConstructorDecl(cu);
        for (var c : constructDecls) {
            DealOthers.dealWithConstructorDecl(c, units);
        }

        var ds = Extract.getDostmt(cu);
        for (var d : ds) {
            DealStatement.dealWithStmt(d, units);
        }

        var ecs = Extract.getEnumConstantDeclaration(cu);
        for (var e : ecs) {
            DealOthers.dealWithEnumConstDecl(e, units);
        }

        var eds = Extract.getEnumDeclaration(cu);
        for (var e : eds) {
            DealOthers.dealWithEnumDecl(e, units);
        }

        var ecis = Extract.getExplicitConstructorInvocationStmt(cu);
        for (var e : ecis) {
            DealStatement.dealWithStmt(e, units);
        }

        var es = Extract.getExpressionStmt(cu);
        for (var e : es) {
            DealStatement.dealWithStmt(e, units);
        }

        var fds = Extract.getFieldDeclaration(cu);
        for (var f : fds) {
            DealOthers.dealWithFieldDecl(f, units);
        }

        var fs = Extract.getForEachStmt(cu);
        for (var f : fs) {
            DealStatement.dealWithStmt(f, units);
        }

        var fors = Extract.getForStmt(cu);
        for (var f : fors) {
            DealStatement.dealWithStmt(f, units);
        }

        var ifs = Extract.getIfStmt(cu);
        for (var i : ifs) {
            DealStatement.dealWithStmt(i, units);
        }

        var ids = Extract.getImportDeclaration(cu);
        for (var i : ids) {
            DealOthers.dealWithImport(i, units);
        }

        var idecls = Extract.getInitializerDeclaration(cu);
        for (var i : idecls) {
            DealOthers.dealWithInitializerDecl(i, units);
        }

        var lbs = Extract.getLabeledStmt(cu);
        for (var l : lbs) {
            DealStatement.dealWithStmt(l, units);
        }

        var lcds = Extract.getLocalClassDeclarationStmt(cu);
        for (var l : lcds) {
            DealStatement.dealWithStmt(l, units);
        }

        var mds = Extract.getMethodDeclaration(cu);
        for (var m : mds) {
            DealOthers.dealWithMethodDecl(m, units);
        }

        var pds = Extract.getPackageDeclaration(cu);
        for (var p : pds) {
            DealOthers.dealWithPackageDecl(p, units);
        }

        var rs = Extract.getReturnStmt(cu);
        for (var r : rs) {
            DealStatement.dealWithStmt(r, units);
        }

        var ss = Extract.getSwitchStmt(cu);
        for (var s : ss) {
            DealStatement.dealWithStmt(s, units);
        }

        var syncs = Extract.getSynchronizedStmt(cu);
        for (var s : syncs) {
            DealStatement.dealWithStmt(s, units);
        }

        var ts = Extract.getThrowStmt(cu);
        for (var t : ts) {
            DealStatement.dealWithStmt(t, units);
        }

        var trys = Extract.getTryStmt(cu);
        for (var t : trys) {
            DealStatement.dealWithStmt(t, units);
        }

        var whiles = Extract.getWhileStmt(cu);
        for (var w : whiles) {
            DealStatement.dealWithStmt(w, units);
        }
    }

    private void genCodeForUnits() {
        for (Unit u : units) {
            try {
                if (u.getNotSkip()) {
                    u.setCode(getCode(u.getBegPos(), u.getEndPos()));
                } else if (u.getSkipTwoSides()) {
                    Position p1 = skipAheadTo(u.getBegPos(), u.getBeginChar());
                    Position p2 = skipBackTo(u.getEndPos(), u.GetEndChar());
                    u.setCode(getCode(p1, p2));
                    u.setBegPos(p1);
                    u.setEndPos(p2);
                } else if (u.getSkipAhead()) {
                    Position p1 = u.getBegPos();
                    if (u.getHasAnnotation()) {
                        p1 = skipAhead(p1);
                        p1 = skipToNonEmpty(p1);
                    }
                    Position p2 = skipAheadTo(p1, u.GetEndChar());

                    if (getChar(p2) != ')') {
                        p2 = skipBack(p2);
                    }

                    u.setCode(getCode(p1, p2));
                    u.setBegPos(p1);
                    u.setEndPos(p2);
                } else {
                    Position p1 = u.getBegPos();
                    if (u.getHasAnnotation()) {
                        p1 = skipAhead(p1);
                        p1 = skipToNonEmpty(p1);
                    }
                    Position p2 = u.getEndPos();
                    p2 = skipBackTo(p2, u.GetEndChar());
                    if (getChar(p2) != ')') {
                        p2 = skipBack(p2);
                    }

                    u.setCode(getCode(p1, p2));
                    u.setBegPos(p1);
                    u.setEndPos(p2);
                }
            } catch (Exception e) {
                System.out.println(u.getType());
                System.out.println("exception occurs during handling between postion:" + u.getBegPos()
                        + " and position:" + u.getEndPos());
                System.out.println(e);
                e.printStackTrace();
            }

        }
    }

    private static class UnitComparator implements Comparator<Unit> {
        @Override
        public int compare(Unit u1, Unit u2) {
            if (u1.getBegPos().equals(u2.getBegPos()) && u1.getEndPos().equals(u2.getEndPos())) {
                return 0;
            } else if (u1.getBegPos().isBefore(u2.getBegPos())) {
                return -1;
            } else if (u1.getBegPos().equals(u2.getBegPos()) && u1.getEndPos().isBefore(u2.getEndPos())) {
                return -1;
            }
            return 1;
        }
    }

    private void removeDup() {
        TreeSet<Unit> ts = new TreeSet<>(new UnitComparator());
        for (Unit u : units) {
            ts.add(u);
        }
        units = new ArrayList<>();
        for (Unit u : ts) {
            units.add(u);
        }
    }

    public ArrayList<Unit> getAllUnits() {
        return units;
    }

}
