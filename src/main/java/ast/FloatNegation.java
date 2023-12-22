package ast;

public record FloatNegation(Expression expr) implements UnaryExpression {
    @Override
    public DataType getDataType() {
        return DataType.ensureSame(DataType.FLOAT, expr.getDataType());
    }

    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
