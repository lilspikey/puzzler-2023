import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class BasicCompiler {
    public static void main(String[] args) throws IOException {
        var javaAsm = new JavaASM();
        var sourceFile = args[0];
        try (var in = new BufferedInputStream(new FileInputStream(sourceFile))) {
            var parser = new Parser();
            var program = parser.parse(new InputStreamReader(in));
            program.visit(new ProgramListing());
            program.visit(javaAsm);
        }
        var className = new File(sourceFile).getName().replaceAll("[^a-z]", "_");
        var classFileName = className + ".class";
        var bytes = javaAsm.generateClass(className);
        try (var out = new FileOutputStream(classFileName)) {
            out.write(bytes);
        }
        System.out.println(classFileName);
    }
}
