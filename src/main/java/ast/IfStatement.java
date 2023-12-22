package ast;

public record IfStatement(Expression predicate, Statement then) implements Statement {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
