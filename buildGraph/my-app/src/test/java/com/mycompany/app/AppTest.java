package com.mycompany.app;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import com.mycompany.Project.Project;
import com.mycompany.Project.SourceFile;
import com.mycompany.Project.UnionProject;
import com.mycompany.app.App;
import com.mycompany.genFinalGraph.GenGraph;
import com.mycompany.genFinalGraph.TrimGraph;
import com.mycompany.mapping.LineMapping;
import com.mycompany.parseJoernDot.JoernGraph;
import com.mycompany.parsePatch.Patch;
import com.mycompany.parsePatch.PatchFile;
import com.mycompany.parsePatch.PatchLine;

import org.junit.Test;
import com.alibaba.fastjson.*;

/**
 * Unit test for simple App.
 */
public class AppTest {
    /**
     * Rigorous Test :-)
     */

    String trainDataPath = ''
    @Test
    public void shouldAnswerWithTrue() {

        try {
            for (int i = 0; i <= 1572; ++i) {
                System.out.println("begin to deal test " + i);
                UnionProject up = new UnionProject(
                        trainDataPath + "/test" + i);
                up.writeJson(trainDataPath + "/test"
                        + i + "/graph.json");
                System.out.println("finish dealing test " + i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
