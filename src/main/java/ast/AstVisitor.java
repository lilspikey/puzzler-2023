package ast;

public interface AstVisitor {
    void visit(PrintStatement statement);

    void visit(GotoStatement statement);

    void visit(FloatAssignment statement);

    void visit(StringConstant expression);

    void visit(FloatConstant expression);

    void visit(FloatVariable expression);
}
