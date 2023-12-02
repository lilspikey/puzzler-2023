package ast;

public record FloatAssignment(String lineLabel, String name, float value) implements Statement {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
