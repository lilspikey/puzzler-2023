package ast;

public record RemarkStatement(String lineLabel, String comment) implements Statement {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}
