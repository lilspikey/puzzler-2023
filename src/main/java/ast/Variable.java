package ast;

public record Variable(VarName name) implements Expression {

    @Override
    public DataType getDataType() {
        return name.dataType();
    }

    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
