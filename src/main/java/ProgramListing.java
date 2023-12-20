import ast.AndExpression;
import ast.AstVisitor;
import ast.BinaryExpression;
import ast.DataStatement;
import ast.DimStatement;
import ast.EndStatement;
import ast.Equals;
import ast.Expression;
import ast.FloatAddition;
import ast.FloatConstant;
import ast.FloatDivision;
import ast.FloatMultiplication;
import ast.FloatNegation;
import ast.FloatPower;
import ast.FloatSubtraction;
import ast.ForStatement;
import ast.FunctionCall;
import ast.GoSubStatement;
import ast.GotoStatement;
import ast.GreaterThan;
import ast.GreaterThanEquals;
import ast.IfStatement;
import ast.InputStatement;
import ast.LessThan;
import ast.LessThanEquals;
import ast.LetStatement;
import ast.Line;
import ast.NextStatement;
import ast.NotEquals;
import ast.OnGotoStatement;
import ast.OrExpression;
import ast.PrintSeperator;
import ast.PrintStatement;
import ast.ReadStatement;
import ast.RemarkStatement;
import ast.RestoreStatement;
import ast.ReturnStatement;
import ast.StopStatement;
import ast.StringConstant;
import ast.VarName;
import ast.Variable;

/*
 Just a simple visitor to walk the AST and print it out
 */
public class ProgramListing implements AstVisitor {

    @Override
    public void visit(Line line) {
        System.out.print(line.label() + " ");
        var separator = false;
        for (var statement: line.statements()) {
            if (separator) {
                System.out.print(" : ");
            }
            statement.visit(this);
            if (!separator && !(statement instanceof IfStatement)) {
                separator = true;
            }
        }
        System.out.println();
    }

    public void visit(VarName varName) {
        System.out.print(varName.name().replace("()", ""));
        if (varName.isArray()) {
            System.out.print("(");
            var first = true;
            for (var index: varName.indexes()) {
                if (!first) {
                    System.out.print(",");
                }
                index.visit(this);
                first = false;
            }
            System.out.print(")");
        }
    }

    @Override
    public void visit(PrintStatement statement) {
        System.out.print("PRINT ");
        for (var printable: statement.printables()) {
            if (printable == PrintSeperator.NONE) {
                System.out.print(";");
            } else if (printable == PrintSeperator.ZONE) {
                System.out.print(",");
            } else {
                ((Expression) printable).visit(this);
            }
        }
    }

    @Override
    public void visit(GotoStatement statement) {
        System.out.print("GOTO " + statement.destinationLabel());
    }

    @Override
    public void visit(GoSubStatement statement) {
        System.out.print("GOSUB " + statement.destinationLabel());
    }

    @Override
    public void visit(ReturnStatement statement) {
        System.out.print("RETURN");
    }

    @Override
    public void visit(OnGotoStatement statement) {
        System.out.print("ON ");
        statement.expression().visit(this);
        System.out.print(" GOTO ");
        System.out.print(String.join(",", statement.destinationLabels()));
    }

    @Override
    public void visit(IfStatement statement) {
        System.out.print("IF ");
        statement.predicate().visit(this);
        System.out.print(" THEN ");
    }

    @Override
    public void visit(LetStatement statement) {
        visit(statement.name());
        System.out.print("=");
        statement.expression().visit(this);
    }

    @Override
    public void visit(InputStatement statement) {
        System.out.print("INPUT ");
        if (statement.prompt() != null) {
            System.out.printf("\"%s\";", statement.prompt());
        }
        var first = true;
        for (var name: statement.names()) {
            if (!first) {
                System.out.print(",");
            }
            visit(name);
            first = false;
        }
    }

    @Override
    public void visit(DimStatement statement) {
        System.out.print("DIM ");
        var firstArray = true;
        for (var array: statement.arrays()) {
            if (!firstArray) {
                System.out.println(",");
            }
            System.out.print(array.name() + "(");
            var first = true;
            for (var size : array.sizes()) {
                if (!first) {
                    System.out.print(",");
                }
                size.visit(this);
                first = false;
            }
            System.out.print(")");
            firstArray = false;
        }
    }

    @Override
    public void visit(RemarkStatement statement) {
        System.out.print("REM " + statement.comment());
    }

    @Override
    public void visit(DataStatement statement) {
        System.out.print("DATA ");
        var first = true;
        for (var constant: statement.constants()) {
            if (!first) {
                System.out.print(",");
            }
            if (constant instanceof String) {
                System.out.print("\"" + constant + "\"");
            } else {
                System.out.print(constant);
            }
            first = false;
        }
    }

    @Override
    public void visit(ReadStatement statement) {
        System.out.print("READ ");
        var first = true;
        for (var name: statement.names()) {
            if (!first) {
                System.out.print(",");
            }
            visit(name);
            first = false;
        }
    }

    @Override
    public void visit(RestoreStatement statement) {
        System.out.print("RESTORE");
        if (statement.label() != null) {
            System.out.print(" " + statement.label());
        };
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
    public void visit(EndStatement statement) {
        System.out.print("END");
    }

    @Override
    public void visit(StopStatement statement) {
        System.out.print("STOP");
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
    public void visit(Variable expression) {
        visit(expression.name());
    }

    @Override
    public void visit(FloatNegation expression) {
        System.out.print("-(");
        expression.expr().visit(this);
        System.out.print(")");
    }

    @Override
    public void visit(Equals expression) {
        binaryExpression("=", expression);
    }

    @Override
    public void visit(NotEquals expression) {
        binaryExpression("<>", expression);
    }

    @Override
    public void visit(GreaterThan expression) {
        binaryExpression(">", expression);
    }

    @Override
    public void visit(GreaterThanEquals expression) {
        binaryExpression(">=", expression);
    }

    @Override
    public void visit(LessThan expression) {
        binaryExpression("<", expression);
    }

    @Override
    public void visit(LessThanEquals expression) {
        binaryExpression("<=", expression);
    }

    @Override
    public void visit(AndExpression expression) {
        binaryExpression("AND", expression);
    }

    @Override
    public void visit(OrExpression expression) {
        binaryExpression("OR", expression);
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
    public void visit(FloatPower expression) {
        binaryExpression("^", expression);
    }

    @Override
    public void visit(FunctionCall expression) {
        System.out.print(expression.fn().name() + "(");
        var first = true;
        for (var arg: expression.args()) {
            if (!first) {
                System.out.print(", ");
            }
            arg.visit(this);
            first = false;
        }
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
