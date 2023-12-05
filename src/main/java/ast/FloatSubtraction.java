package ast;

public record FloatSubtraction(Expression lhs, Expression rhs) implements Expression {
    @Override
    public DataType getDataType() {
        return DataType.FLOAT;
    }

    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
