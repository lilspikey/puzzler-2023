import ast.AstVisitor;
import ast.BinaryExpression;
import ast.FloatAddition;
import ast.FloatAssignment;
import ast.FloatConstant;
import ast.FloatDivision;
import ast.FloatEquality;
import ast.FloatGreaterThan;
import ast.FloatInput;
import ast.FloatMultiplication;
import ast.FloatSubtraction;
import ast.FloatVariable;
import ast.GotoStatement;
import ast.IfStatement;
import ast.PrintStatement;
import ast.Statement;
import ast.StringConstant;

/*
 Just a simple visitor to walk the AST and print it out
 */
public class ProgramListing implements AstVisitor {
    @Override
    public void visit(PrintStatement statement) {
        System.out.print(lineLabelPrefix(statement) + " PRINT ");
        for (var expression: statement.expressions()) {
            expression.visit(this);
            System.out.print(' ');
        }
        System.out.println();
    }

    @Override
    public void visit(GotoStatement statement) {
        System.out.println(lineLabelPrefix(statement) + " GOTO " + statement.destinationLabel());
    }

    @Override
    public void visit(IfStatement statement) {
        System.out.print(lineLabelPrefix(statement) + " IF ");
        statement.predicate().visit(this);
        System.out.print(" THEN ");
        var first = true;
        for (var s: statement.trueStatements()) {
            s.visit(this);
            if (!first) {
                System.out.print(':');
            }
            first = false;
        }
        System.out.println();
    }

    @Override
    public void visit(FloatAssignment statement) {
        System.out.print(lineLabelPrefix(statement) + " " + statement.name() + " = ");
        statement.expression().visit(this);
        System.out.println();
    }

    @Override
    public void visit(FloatInput statement) {
        System.out.print(lineLabelPrefix(statement) + " INPUT " + statement.name());
    }

    @Override
    public void visit(StringConstant expression) {
        System.out.print('\"' + expression.constant() + '\"');
    }

    @Override
    public void visit(FloatConstant expression) {
        System.out.print(expression.constant());
    }

    @Override
    public void visit(FloatVariable expression) {
        System.out.print(expression.name());
    }

    @Override
    public void visit(FloatEquality expression) {
        binaryExpression("=", expression);
    }

    @Override
    public void visit(FloatGreaterThan expression) {
        binaryExpression(">", expression);
    }

    @Override
    public void visit(FloatAddition expression) {
        binaryExpression("+", expression);
    }

    @Override
    public void visit(FloatSubtraction expression) {
        binaryExpression("-", expression);
    }

    @Override
    public void visit(FloatMultiplication expression) {
        binaryExpression("*", expression);
    }

    @Override
    public void visit(FloatDivision expression) {
        binaryExpression("/", expression);
    }

    private void binaryExpression(String operator, BinaryExpression expression) {
        System.out.print("(");
        expression.lhs().visit(this);
        System.out.print(operator);
        expression.rhs().visit(this);
        System.out.print(")");
    }

    private String lineLabelPrefix(Statement statement) {
        if (statement.lineLabel() == null) {
            return "";
        }
        return statement.lineLabel();
    }
}
