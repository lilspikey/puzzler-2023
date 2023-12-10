package ast;

import java.util.List;

public record Line(String label, List<Statement> statements) {
    public int numericLabel() {
        return Integer.parseInt(label);
    }

    void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
