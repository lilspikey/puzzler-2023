package ast;

import java.util.List;

public record PrintStatement(String lineLabel, List<String> strings) implements Statement {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
