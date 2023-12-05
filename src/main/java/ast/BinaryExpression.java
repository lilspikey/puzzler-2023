package ast;

public interface BinaryExpression extends Expression {
    Expression lhs();
    Expression rhs();
}
