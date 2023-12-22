package ast;

public record FloatDivision(Expression lhs, Expression rhs) implements BinaryExpression {
    @Override
    public DataType getDataType() {
        DataType.ensureSame(DataType.FLOAT, lhs().getDataType());
        return DataType.ensureSame(DataType.FLOAT, rhs().getDataType());
    }

    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}