package ast;

import runtime.FunctionDef;

import java.util.List;

public record FunctionCall(FunctionDef fn, List<Expression> args) implements Expression {
    @Override
    public DataType getDataType() {
        return fn.returnType();
    }

    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
