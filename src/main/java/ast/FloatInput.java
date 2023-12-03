package ast;

public record FloatInput(String lineLabel, String name) implements Statement {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
