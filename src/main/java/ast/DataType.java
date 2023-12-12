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

    public static DataType ensureSame(DataType lhs, DataType rhs) {
        if (lhs != rhs) {
            throw new IllegalArgumentException(lhs + " != " + rhs);
        }
        return lhs;
    }
}
