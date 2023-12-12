package ast;

public record StringVariable(String name) implements Expression {
    @Override
    public DataType getDataType() {
        return DataType.STRING;
    }

    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
