package ast;

public record ReturnStatement() implements Statement {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
