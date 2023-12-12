package ast;

public enum DataType {
    FLOAT,
    STRING;

    public static DataType fromVarName(String name) {
        if (name.endsWith("$")) {
            return DataType.STRING;
        }
        return DataType.FLOAT;
    }
}
