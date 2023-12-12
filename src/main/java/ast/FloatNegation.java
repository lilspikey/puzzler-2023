package ast;

public record FloatNegation(Expression expr) implements UnaryExpression {
    @Override
    public DataType getDataType() {
        return DataType.FLOAT;
    }

    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
