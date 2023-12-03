package ast;

public record FloatAssignment(String lineLabel, String name, Expression expression) implements Statement {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
