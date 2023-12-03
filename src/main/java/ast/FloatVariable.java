package ast;

public record FloatVariable(String name) implements Expression {

    @Override
    public DataType getDataType() {
        return ast.DataType.FLOAT;
    }

    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
