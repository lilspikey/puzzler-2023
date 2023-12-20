package runtime;

import ast.DataType;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record FunctionDef(String name, DataType returnType, List<DataType> argTypes) {
    private static final Pattern NAME_PATTERN = Pattern.compile("^fn(.*)");
    private static final String DOLLAR_SUFFIX = "_DOLLAR";
    
    private static Set<FunctionDef> functions = null;

    public static Set<FunctionDef> getFunctionDefs() {
        if (functions == null) {
            functions = loadFunctionDefs();
        }
        return functions;
    }
    
    public static List<FunctionDef> findFunctions(String name) {
        return functions.stream()
            .filter(fn -> fn.name().equals(name))
            .toList();
    }

    public static String toRuntimeFn(String name) {
        if (name.endsWith("$")) {
            return "fn" + name.substring(0, name.length() - 1) + DOLLAR_SUFFIX;
        }
        return "fn" + name;
    }

    private static Set<FunctionDef> loadFunctionDefs() {
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
        name = name.replaceAll(DOLLAR_SUFFIX, "\\$");
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
