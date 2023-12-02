package ast;

public interface Statement {
    String lineLabel();

    void visit(AstVisitor visitor);
}
