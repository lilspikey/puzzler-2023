package ast;

import java.util.List;

public record Line(String label, List<Statement> statements) {
    void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
