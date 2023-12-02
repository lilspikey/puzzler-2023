import ast.AstVisitor;
import ast.GotoStatement;
import ast.PrintStatement;
import ast.Statement;

/*
 Just a simple visitor to walk the AST and print it out
 */
public class ProgramListing implements AstVisitor {
    @Override
    public void visit(PrintStatement statement) {
        System.out.println(lineLabelPrefix(statement) + " PRINT " + String.join(" ", statement.strings()));
    }

    @Override
    public void visit(GotoStatement statement) {
        System.out.println(lineLabelPrefix(statement) + " GOTO " + statement.destinationLabel());
    }

    private String lineLabelPrefix(Statement statement) {
        if (statement.lineLabel() == null) {
            return "";
        }
        return statement.lineLabel();
    }
}
