package ast;

import java.util.List;

public record Program(List<Statement> statements) {
    public void visit(AstVisitor visitor) {
        for (var statement: statements) {
            statement.visit(visitor);
        }
    }
}
