import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

class Functions {
    static Set<String> getFunctionNames() {
        // use convention of fn prefix to look up methods
        return Arrays.stream(BasRuntime.class.getDeclaredMethods())
            .filter(method -> method.getName().startsWith("fn"))
            .map(method -> method.getName().replaceAll("^fn", ""))
            .collect(Collectors.toSet());
    }
}
