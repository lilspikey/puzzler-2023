package ast;

public record LetStatement(VarName name, Expression expression) implements Statement {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
