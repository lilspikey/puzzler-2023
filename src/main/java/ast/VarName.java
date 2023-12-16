package ast;

public record VarName(String name, DataType dataType) {
    @Override
    public String toString() {
        return name;
    }
}
