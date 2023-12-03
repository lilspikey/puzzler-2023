package ast;

public interface Expression {
    DataType getDataType();
    void visit(AstVisitor visitor);
}
