package ast;

public interface Expression extends Printable {
    DataType getDataType();
    void visit(AstVisitor visitor);
}
