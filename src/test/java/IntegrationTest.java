import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.beans.PropertyDescriptor;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntegrationTest {

    @ParameterizedTest
    @ValueSource(strings = { "hello", "loop" })
    void givenSource_whenCompilingAndRunning_thenCorrectOutputGenerated(String exampleDir) throws Exception {
        var javaAsm = new JavaASM();
        var inputSource = "examples/" + exampleDir + "/input.bas";
        try (var in = new BufferedInputStream(getClass().getResourceAsStream(inputSource))) {
            var parser = new Parser();
            var program = parser.parse(new InputStreamReader(in));
            program.visit(javaAsm);
        }
        var className = exampleDir + "_test";
        var bytes = javaAsm.generateClass(className);
        var classLoader = new ClassLoader() {
            @Override
            protected Class<?> findClass(String name) {
                return defineClass(name, bytes, 0, bytes.length);
            }
        };

        Class<? extends Runnable> clazz = (Class) classLoader.findClass(className);
        Runnable runnable = clazz.getDeclaredConstructor().newInstance();
        PropertyDescriptor outProperty = new PropertyDescriptor("out", clazz);
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        try (PrintStream printOut = new PrintStream(capturedOutput)) {
            outProperty.getWriteMethod().invoke(runnable, printOut);
            runnable.run();
        }

        assertEquals(
            readResource("examples/" + exampleDir + "/output.txt"),
            capturedOutput.toString(StandardCharsets.UTF_8)
        );
    }

    private String readResource(String path) throws URISyntaxException, IOException {
        return Files.readString(Paths.get(getClass().getClassLoader().getResource(path).toURI()));
    }

}
