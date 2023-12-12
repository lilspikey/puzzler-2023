package runtime;

import java.io.InputStream;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.Random;
import java.util.Scanner;

/*
 Used as a template to create the final class file output, but also
 provides methods that are useful for implementing some functionality,
 without having to manually create lots of byte code
 */
public class BasRuntime implements Runnable {
    private static final int PRINT_ZONE_WIDTH = 14;
    // these are here so we can swap them out in tests
    private InputStream in = System.in;
    private PrintStream out = System.out;
    private int currentTab = 0;
    private Random random = new Random();
    private float prevRandom;

    float fnINT(float f) {
        return (int) f;
    }

    float fnSIN(float f) {
        return (float) Math.sin(Math.toRadians(f));
    }

    float fnRND(float f) {
        if (f < 0.0f) {
            random = new Random((int) f);
        }
        if (f == 0.0f) {
            return prevRandom;
        }
        return (prevRandom = random.nextFloat());
    }

    String fnTAB(float f) {
        var builder = new StringBuilder();
        for (var i = currentTab; i < f; i++) {
            builder.append(' ');
        }
        return builder.toString();
    }

    void print(String s) {
        out.print(s);
        currentTab += s.length();
    }

    void print(float f) {
        print(formatFloat(f));
    }

    void nextPrintZone() {
        var nextZone = currentTab % 14;
        for (var i = 0; i < nextZone; i++) {
            print(" ");
        }
    }

    void println() {
        out.println();
        currentTab = 0;
    }

    private String formatFloat(float f) {
        return MessageFormat.format("{0,number,0.###}", f);
    }

    float inputFloat() {
        var in = new Scanner(this.in);
        while (!in.hasNextFloat()) {
            in.next();
        }
        return in.nextFloat();
    }

    @Override
    public void run() {

    }

    public InputStream getIn() {
        return in;
    }

    public void setIn(InputStream in) {
        this.in = in;
    }

    public PrintStream getOut() {
        return out;
    }

    public void setOut(PrintStream out) {
        this.out = out;
    }

    public static void main(String[] args) {
        new BasRuntime().run();
    }
}
