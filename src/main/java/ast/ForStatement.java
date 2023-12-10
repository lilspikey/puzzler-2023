package ast;

public record ForStatement(String varname, Expression start, Expression end, Expression increment) implements Statement {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
