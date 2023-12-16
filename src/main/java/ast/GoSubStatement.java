package ast;

public record GoSubStatement(String destinationLabel) implements Statement {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
