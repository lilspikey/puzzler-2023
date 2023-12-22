package ast;

public interface AstVisitor {
    default void visit(Program program) {
        for (var line: program.lines()) {
            line.visit(this);
        }
    }

    void visit(Line line);

    void visit(PrintStatement statement);

    void visit(GotoStatement statement);
    
    void visit(OnGotoStatement statement);

    void visit(GoSubStatement statement);

    void visit(ReturnStatement statement);

    void visit(IfStatement statement);

    void visit(RemarkStatement statement);

    void visit(DataStatement statement);

    void visit(ReadStatement statement);

    void visit(RestoreStatement statement);

    void visit(EndStatement statement);

    void visit(StopStatement statement);

    void visit(ForStatement statement);

    void visit(NextStatement statement);

    void visit(LetStatement statement);

    void visit(InputStatement statement);

    void visit(DimStatement statement);

    void visit(StringConstant expression);

    void visit(FloatConstant expression);

    void visit(Variable expression);

    void visit(FloatNegation expression);

    void visit(Equals expression);

    void visit(NotEquals expression);

    void visit(GreaterThan expression);

    void visit(GreaterThanEquals expression);

    void visit(LessThan expression);

    void visit(LessThanEquals expression);

    void visit(AndExpression expression);

    void visit(OrExpression expression);

    void visit(Addition expression);

    void visit(FloatSubtraction expression);

    void visit(FloatMultiplication expression);

    void visit(FloatDivision expression);

    void visit(FloatPower expression);

    void visit(FunctionCall expression);
}
