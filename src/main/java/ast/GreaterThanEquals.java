package ast;

public record GreaterThanEquals(Expression lhs, Expression rhs) implements BinaryExpression {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}