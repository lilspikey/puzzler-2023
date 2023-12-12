package ast;

public interface BinaryExpression extends Expression {
    Expression lhs();
    Expression rhs();

    @Override
    default DataType getDataType() {
        return DataType.ensureSame(lhs().getDataType(), rhs().getDataType());
    }
}
