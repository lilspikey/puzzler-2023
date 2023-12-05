package ast;

public interface AstVisitor {
    void visit(PrintStatement statement);

    void visit(GotoStatement statement);

    void visit(IfStatement statement);

    void visit(FloatAssignment statement);

    void visit(FloatInput statement);

    void visit(StringConstant expression);

    void visit(FloatConstant expression);

    void visit(FloatVariable expression);

    void visit(FloatEquality expression);

    void visit(FloatAddition expression);

    void visit(FloatSubtraction expression);

    void visit(FloatMultiplication expression);

    void visit(FloatDivision expression);
}
