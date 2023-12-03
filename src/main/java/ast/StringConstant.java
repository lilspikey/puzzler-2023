package ast;

public record StringConstant(String constant) implements Expression {
    @Override
    public DataType getDataType() {
        return DataType.STRING;
    }

    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
