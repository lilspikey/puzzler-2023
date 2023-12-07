package ast;

public record FloatEquals(Expression lhs, Expression rhs) implements BinaryExpression {
    @Override
    public DataType getDataType() {
        return DataType.FLOAT;
    }

    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
