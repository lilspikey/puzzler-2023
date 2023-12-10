import ast.AstVisitor;
import ast.BinaryExpression;
import ast.FloatAddition;
import ast.FloatAssignment;
import ast.FloatConstant;
import ast.FloatDivision;
import ast.FloatEquals;
import ast.FloatGreaterThan;
import ast.FloatGreaterThanEquals;
import ast.FloatInput;
import ast.FloatLessThan;
import ast.FloatLessThanEquals;
import ast.FloatMultiplication;
import ast.FloatNotEquals;
import ast.FloatSubtraction;
import ast.FloatVariable;
import ast.ForStatement;
import ast.FunctionCall;
import ast.GotoStatement;
import ast.IfStatement;
import ast.Line;
import ast.NextStatement;
import ast.PrintStatement;
import ast.RemarkStatement;
import ast.Statement;
import ast.StringConstant;

/*
 Just a simple visitor to walk the AST and print it out
 */
public class ProgramListing implements AstVisitor {

    @Override
    public void visit(Line line) {
        System.out.print(line.label() + " ");
        for (var statement: line.statements()) {
            statement.visit(this);
        }
        System.out.println();
    }

    @Override
    public void visit(PrintStatement statement) {
        System.out.print("PRINT ");
        for (var expression: statement.expressions()) {
            expression.visit(this);
            System.out.print(' ');
        }
    }

    @Override
    public void visit(GotoStatement statement) {
        System.out.print("GOTO " + statement.destinationLabel());
    }

    @Override
    public void visit(IfStatement statement) {
        System.out.print("IF ");
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
    }

    @Override
    public void visit(FloatAssignment statement) {
        System.out.print(statement.name() + " = ");
        statement.expression().visit(this);
    }

    @Override
    public void visit(FloatInput statement) {
        System.out.print("INPUT " + statement.name());
    }

    @Override
    public void visit(RemarkStatement statement) {
        System.out.print("REM " + statement.comment());
    }

    @Override
    public void visit(ForStatement statement) {
        System.out.print("FOR " + statement.varname() + " = ");
        statement.start().visit(this);
        System.out.print(" TO ");
        statement.end().visit(this);
        if (statement.increment() != null) {
            System.out.print(" STEP ");
            statement.increment().visit(this);
        }
    }

    @Override
    public void visit(NextStatement statement) {
        System.out.print("NEXT");
        if (statement.varname() != null) {
            System.out.print(" " + statement.varname());
        }
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
    public void visit(FloatEquals expression) {
        binaryExpression("=", expression);
    }

    @Override
    public void visit(FloatNotEquals expression) {
        binaryExpression("<>", expression);
    }

    @Override
    public void visit(FloatGreaterThan expression) {
        binaryExpression(">", expression);
    }

    @Override
    public void visit(FloatGreaterThanEquals expression) {
        binaryExpression(">=", expression);
    }

    @Override
    public void visit(FloatLessThan expression) {
        binaryExpression("<", expression);
    }

    @Override
    public void visit(FloatLessThanEquals expression) {
        binaryExpression("<=", expression);
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

    @Override
    public void visit(FunctionCall expression) {
        System.out.print(expression.fn() + "(");
        expression.arg().visit(this);
        System.out.print(")");
    }

    private void binaryExpression(String operator, BinaryExpression expression) {
        System.out.print("(");
        expression.lhs().visit(this);
        System.out.print(operator);
        expression.rhs().visit(this);
        System.out.print(")");
    }
}
