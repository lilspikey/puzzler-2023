import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class BasicCompiler {
    public static void main(String[] args) throws IOException {
        try (var in = new BufferedInputStream(new FileInputStream(args[0]))) {
            var parser = new Parser();
            var program = parser.parse(new InputStreamReader(in));
            program.visit(new ProgramListing());
        }
    }
}
