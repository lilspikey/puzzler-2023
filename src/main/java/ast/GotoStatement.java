package ast;

public record GotoStatement(String destinationLabel) implements Statement {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
