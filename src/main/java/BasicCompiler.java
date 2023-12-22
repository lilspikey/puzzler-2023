import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class BasicCompiler {

    public static void main(String[] args) throws Exception {
        String sourceFile = null;
        var run = false;
        var list = false;
        for (var arg: args) {
            if (arg.startsWith("-")) {
                if (arg.equals("--run")) {
                    run = true;
                } else if (arg.equals("--list")) {
                    list = true;
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            } else {
                if (sourceFile != null) {
                    throw new IllegalArgumentException("Unexpected argument: " + arg);
                }
                sourceFile = arg;
            }
        }
        
        var javaAsm = new JavaASM();
        try (var in = new BufferedInputStream(new FileInputStream(sourceFile))) {
            var parser = new Parser();
            var program = parser.parse(new InputStreamReader(in));
            if (list) {
                program.visit(new ProgramListing());
            }
            program.visit(javaAsm);
        }
        var className = new File(sourceFile).getName().replaceAll("[^a-z]", "_");
        var bytes = javaAsm.generateClass(className);
        
        if (run) {
            var classLoader = new ClassLoader() {
                @Override
                protected Class<?> findClass(String name) {
                    return defineClass(name, bytes, 0, bytes.length);
                }
            };

            Class<? extends Runnable> clazz = (Class) classLoader.findClass(className);
            Runnable runnable = clazz.getDeclaredConstructor().newInstance();
            runnable.run();
        } else {
            var classFileName = className + ".class";
            try (var out = new FileOutputStream(classFileName)) {
                out.write(bytes);
            }
            System.out.println(classFileName);
        }
    }
}
