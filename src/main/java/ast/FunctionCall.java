package ast;

// TODO support variable number of arguments + string data types
public record FunctionCall(String fn, Expression arg) implements Expression {
    @Override
    public DataType getDataType() {
        return DataType.FLOAT;
    }

    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
