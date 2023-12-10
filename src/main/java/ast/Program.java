package ast;

import java.util.List;

public record Program(List<Line> lines) {
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
