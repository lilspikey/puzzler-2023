import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Scanner;

/*
 Used as a template to create the final class file output, but also
 provides methods that are useful for implementing some functionality,
 without having to manually create lots of byte code
 */
public class BasRuntime {

    void print(String s) {
        System.out.print(s);
    }

    void print(float f) {
        System.out.print(MessageFormat.format("{0,number,0.###}", f));
    }

    void println() {
        System.out.println();
    }

    float inputFloat() {
        var in = new Scanner(System.in);
        while (!in.hasNextFloat()) {
            in.next();
        }
        return in.nextFloat();
    }

    void run() {

    }

    public static void main(String[] args) {
        new BasRuntime().run();
    }
}
