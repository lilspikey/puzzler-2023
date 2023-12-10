package runtime;

import ast.DataType;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record FunctionDef(String name, DataType returnType, List<DataType> argTypes) {
    private static final Pattern NAME_PATTERN = Pattern.compile("^fn(.*)");

    public static Set<FunctionDef> getFunctionDefs() {
        var funtionDefs = new HashSet<FunctionDef>();
        for (var method: BasRuntime.class.getDeclaredMethods()) {
            var matcher = NAME_PATTERN.matcher(method.getName());
            if (matcher.matches()) {
                funtionDefs.add(toFunctionDef(matcher, method));
            }
        }
        return funtionDefs;
    }

    private static FunctionDef toFunctionDef(Matcher nameMatch, Method method) {
        var name = nameMatch.group(1);
        var returnType = toDataType(method.getReturnType());
        var argTypes = Arrays.stream(method.getParameterTypes())
            .map(FunctionDef::toDataType)
            .toList();
        return new FunctionDef(name, returnType, argTypes);
    }

    private static DataType toDataType(Class<?> clazz) {
        if (clazz.equals(Float.TYPE)) {
            return DataType.FLOAT;
        }
        if (clazz.equals(String.class)) {
            return DataType.STRING;
        }
        throw new IllegalArgumentException("Unknown data type for class: " + clazz);
    }
}
