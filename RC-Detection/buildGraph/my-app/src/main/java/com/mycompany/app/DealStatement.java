package com.mycompany.app;

import java.util.List;
import java.util.Optional;

import com.github.javaparser.Position;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
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

public class DealStatement {
    public static void dealWithStmt(Statement st, List<Unit> allPair) {
        if (st.isAssertStmt()) {
            AssertStmt aStmt = st.asAssertStmt();
            Unit u = new Unit(aStmt.getBegin().get(), aStmt.getEnd().get(), AssertStmt.class.getSimpleName(), ';');

            allPair.add(u);

        } else if (st.isBreakStmt()) {
            BreakStmt bStmt = st.asBreakStmt();
            Unit u = new Unit(bStmt.getBegin().get(), bStmt.getEnd().get(), BreakStmt.class.getSimpleName(), ';');

            allPair.add(u);

        } else if (st.isContinueStmt()) {
            ContinueStmt cStmt = st.asContinueStmt();
            Unit u = new Unit(cStmt.getBegin().get(), cStmt.getEnd().get(), ContinueStmt.class.getSimpleName(), ';');

            allPair.add(u);

        } else if (st.isExplicitConstructorInvocationStmt()) {
            ExplicitConstructorInvocationStmt eStmt = st.asExplicitConstructorInvocationStmt();
            Unit u = new Unit(eStmt.getBegin().get(), eStmt.getEnd().get(),
                    ExplicitConstructorInvocationStmt.class.getSimpleName(), ';');

            allPair.add(u);

        } else if (st.isExpressionStmt()) {
            ExpressionStmt eStmt = st.asExpressionStmt();
            Unit u = new Unit(eStmt.getBegin().get(), eStmt.getEnd().get(), ExpressionStmt.class.getSimpleName(), ';');

            allPair.add(u);

        } else if (st.isLocalClassDeclarationStmt()) {
            LocalClassDeclarationStmt lStmt = st.asLocalClassDeclarationStmt();
            Unit u = new Unit(lStmt.getBegin().get(), lStmt.getEnd().get(),
                    LocalClassDeclarationStmt.class.getSimpleName(), ';');

            allPair.add(u);

        } else if (st.isThrowStmt()) {
            ThrowStmt tStmt = st.asThrowStmt();
            Unit u = new Unit(tStmt.getBegin().get(), tStmt.getEnd().get(), ThrowStmt.class.getSimpleName(), ';');

            allPair.add(u);

        } else if (st.isReturnStmt()) {
            ReturnStmt rStmt = st.asReturnStmt();
            Unit u = new Unit(rStmt.getBegin().get(), rStmt.getEnd().get(), ReturnStmt.class.getSimpleName(), ';');

            allPair.add(u);
        } else if (st.isLabeledStmt()) {
            LabeledStmt lStmt = st.asLabeledStmt();
            Unit u = new Unit(lStmt.getBegin().get(), lStmt.getEnd().get(), LabeledStmt.class.getSimpleName(), ';');

            allPair.add(u);
        } else if (st.isBlockStmt()) {
            BlockStmt bstmt = st.asBlockStmt();
            NodeList<Statement> l = bstmt.getStatements();
            for (var stmt : l) {
                dealWithStmt(stmt, allPair);
            }
        } else if (st.isDoStmt()) {
            


            DoStmt dStmt = st.asDoStmt();

            Unit u_=new Unit(st.getBegin().get(),dStmt.getBody().getBegin().get(),"do",'o');
            u_.setSkipAhead(true);
            allPair.add(u_);

            Unit u = new Unit(dStmt.getBody().getEnd().get(), dStmt.getEnd().get(), DoStmt.class.getSimpleName(),
                    ')');
            u.setSkipTwoSides(true);
            u.setBeginChar('w');
            allPair.add(u);
            dealWithStmt(dStmt.getBody(), allPair);
        } else if (st.isIfStmt()) {

            IfStmt ifStmt = st.asIfStmt();
            Unit u = new Unit(ifStmt.getBegin().get(), ifStmt.getThenStmt().getBegin().get(),
                    IfStmt.class.getSimpleName(), ')');
            u.setSkipAhead(false);
            allPair.add(u);
            dealWithStmt(ifStmt.getThenStmt(), allPair);

            if (ifStmt.hasElseBranch()) {
                if (!ifStmt.getElseStmt().get().isIfStmt()) {
                    Unit u1 = new Unit(ifStmt.getThenStmt().getEnd().get(), ifStmt.getElseStmt().get().getBegin().get(),
                            "else", 'e');
                    u1.setSkipTwoSides(true);
                    u1.setBeginChar('e');
                    allPair.add(u1);
                }
                dealWithStmt(ifStmt.getElseStmt().get(), allPair);
            }
        } else if (st.isForStmt()) {
            ForStmt forStmt = st.asForStmt();
            Unit u = new Unit(forStmt.getBegin().get(), forStmt.getBody().getBegin().get(),
                    ForStmt.class.getSimpleName(), ')');
            u.setSkipAhead(false);
            allPair.add(u);
            dealWithStmt(forStmt.getBody(), allPair);
        } else if (st.isForEachStmt()) {
            ForEachStmt foreachStmt = st.asForEachStmt();
            Unit u = new Unit(foreachStmt.getBegin().get(), foreachStmt.getBody().getBegin().get(),
                    ForEachStmt.class.getSimpleName(),
                    ')');
            u.setSkipAhead(false);
            allPair.add(u);
            dealWithStmt(foreachStmt.getBody(), allPair);
        } else if (st.isWhileStmt()) {
            WhileStmt whileStmt = st.asWhileStmt();
            Unit u = new Unit(whileStmt.getBegin().get(), whileStmt.getBody().getBegin().get(),
                    "while", ')');
            u.setSkipAhead(false);
            allPair.add(u);
            dealWithStmt(whileStmt.getBody(), allPair);
        } else if (st.isTryStmt()) {
            TryStmt tryStmt = st.asTryStmt();
            Unit u = new Unit(tryStmt.getBegin().get(), tryStmt.getTryBlock().getBegin().get(),
                    TryStmt.class.getSimpleName(), '{');
            u.setSkipAhead(false);
            allPair.add(u);
            dealWithStmt(tryStmt.getTryBlock(), allPair);

            NodeList<CatchClause> l = tryStmt.getCatchClauses();
            for (var e : l) {
                Unit u_ = new Unit(e.getBegin().get(), e.getBody().getBegin().get(),
                        CatchClause.class.getSimpleName(), ')');
                u_.setSkipAhead(false);
                allPair.add(u_);
                dealWithStmt(e.getBody(), allPair);
            }

            Optional<BlockStmt> bs = tryStmt.getFinallyBlock();
            if (bs.isPresent()) {
                Unit u_ = new Unit(bs.get().getBegin().get(), bs.get().getBegin().get(), "finally", '{');
                u_.setSkipAhead(false);
                allPair.add(u_);
                dealWithStmt(bs.get(), allPair);
            }
        } else if (st.isSynchronizedStmt()) {
            SynchronizedStmt syncStmt = st.asSynchronizedStmt();
            Unit u = new Unit(syncStmt.getBegin().get(), syncStmt.getBody().getBegin().get(),
                    SynchronizedStmt.class.getSimpleName(),
                    '{');
            u.setSkipAhead(false);
            allPair.add(u);
            dealWithStmt(syncStmt.getBody(), allPair);
        } else if (st.isSwitchStmt()) {
            SwitchStmt switchStmt = st.asSwitchStmt();
            Unit u = new Unit(switchStmt.getBegin().get(), switchStmt.getEntries().getFirst().get().getBegin().get(),
                    SwitchStmt.class.getSimpleName(), '{');
            u.setSkipAhead(false);
            allPair.add(u);

            NodeList<SwitchEntry> l = switchStmt.getEntries();
            Position lastPos = null;
            for (int i = 0; i < l.size() - 1; ++i) {
                var se = l.get(i);
                NodeList<Expression> exprs = se.getLabels();

                Unit u_ = new Unit(se.getBegin().get(), exprs.getLast().get().getEnd().get(),
                        Expression.class.getSimpleName(), ':');
                u_.setSkipAhead(true);
                allPair.add(u_);
                NodeList<Statement> stmts = se.getStatements();
                for (var stmt : stmts) {
                    dealWithStmt(stmt, allPair);
                }
                // lastPos = stmts.getLast().get().getEnd().get();
            }

            // var lastSe = l.get(l.size() - 1);
            // if (lastSe.getLabels().isEmpty()) {
            //     Unit u_ = new Unit(lastPos, lastSe.getStatements().getFirst().get().getBegin().get(),
            //             "default", ':');
            //     u_.setSkipTwoSides(true);
            //     u_.setBeginChar('d');
            //     allPair.add(u_);
            // } else {
            //     NodeList<Expression> exprs = lastSe.getLabels();

            //     Unit u_ = new Unit(exprs.getFirst().get().getBegin().get(), exprs.getLast().get().getEnd().get(),
            //             Expression.class.getSimpleName(), ':');
            //     allPair.add(u_);
            //     NodeList<Statement> stmts = lastSe.getStatements();
            //     for (var stmt : stmts) {
            //         dealWithStmt(stmt, allPair);
            //     }
            // }
        }
    }
}
