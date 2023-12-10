package ast;

public interface Statement {
    void visit(AstVisitor visitor);
}
