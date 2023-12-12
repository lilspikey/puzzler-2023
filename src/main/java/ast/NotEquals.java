package ast;

public record NotEquals(Expression lhs, Expression rhs) implements BinaryExpression {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}