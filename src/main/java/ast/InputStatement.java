package ast;

import java.util.List;

public record InputStatement(String prompt, List<VarName> names) implements Statement {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
