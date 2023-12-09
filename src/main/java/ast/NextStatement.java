package ast;

public record NextStatement(String lineLabel, String varname) implements Statement {
    @Override
    public void visit(AstVisitor visitor) {
        visitor.visit(this);
    }
}