import ast.AstVisitor;
import ast.FloatAssignment;
import ast.FloatConstant;
import ast.GotoStatement;
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
        }
        System.out.println();
    }

    @Override
    public void visit(GotoStatement statement) {
        System.out.println(lineLabelPrefix(statement) + " GOTO " + statement.destinationLabel());
    }

    @Override
    public void visit(FloatAssignment statement) {
        System.out.print(lineLabelPrefix(statement) + " " + statement.name() + " = ");
        statement.expression().visit(this);
        System.out.println();
    }

    @Override
    public void visit(StringConstant expression) {
        System.out.print(expression.constant());
    }

    @Override
    public void visit(FloatConstant expression) {
        System.out.print(expression.constant());
    }

    private String lineLabelPrefix(Statement statement) {
        if (statement.lineLabel() == null) {
            return "";
        }
        return statement.lineLabel();
    }
}
