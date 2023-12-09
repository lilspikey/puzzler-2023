package ast;

public interface AstVisitor {
    void visit(PrintStatement statement);

    void visit(GotoStatement statement);

    void visit(IfStatement statement);

    default void visit(RemarkStatement statement) {
        // noop
    }

    void visit(ForStatement statement);

    void visit(NextStatement statement);

    void visit(FloatAssignment statement);

    void visit(FloatInput statement);

    void visit(StringConstant expression);

    void visit(FloatConstant expression);

    void visit(FloatVariable expression);

    void visit(FloatEquals expression);

    void visit(FloatNotEquals expression);

    void visit(FloatGreaterThan expression);

    void visit(FloatGreaterThanEquals expression);

    void visit(FloatLessThan expression);

    void visit(FloatLessThanEquals expression);

    void visit(FloatAddition expression);

    void visit(FloatSubtraction expression);

    void visit(FloatMultiplication expression);

    void visit(FloatDivision expression);
}
