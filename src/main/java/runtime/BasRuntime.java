package runtime;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.InputMismatchException;
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
    private Scanner in = new Scanner(System.in);
    private PrintStream out = System.out;
    private int currentTab = 0;
    private Random random = new Random();
    private float prevRandom;
    private Object[] data;
    private int nextDataPtr = 0;
    // manually manage stack for GOSUB return addresses to workaround
    // issues with verification/Java ASM
    private final Deque<Integer> returnAddressStack = new ArrayDeque<>();

    float fnINT(float f) {
        return (int) f;
    }

    float fnABS(float f) {
        return Math.abs(f);
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

    float fnASC(String s) {
        var c = s.charAt(0);
        return c;
    }

    String fnCHR_DOLLAR(float f) {
        return String.valueOf((char) f);
    }
    
    float fnSGN(float f) {
        if (f < 0.0f) {
            return -1.0f;
        }
        if (f > 0.0f) {
            return 1.0f;
        }
        return 0.0f;
    }

    float fnLEN(String s) {
        return s.length();
    }

    String fnLEFT_DOLLAR(String s, float len) {
        return s.substring(0, Math.min((int) len, s.length()));
    }

    String fnMID_DOLLAR(String s, float i) {
        return fnMID_DOLLAR(s, i, s.length());
    }

    String fnMID_DOLLAR(String s, float i, float len) {
        var index = (int)(i - 1.0f);
        return s.substring(index, Math.min(index + (int) len, s.length()));
    }

    String fnRIGHT_DOLLAR(String s, float len) {
        var i = (int) Math.max(0, s.length() - len);
        return s.substring(i);
    }

    float fnVAL(String s) {
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            return 0.0f;
        }
    }

    float readFLOAT() {
        var data = this.data[nextDataPtr++];
        return (Float) data;
    }

    String readSTRING() {
        var data = this.data[nextDataPtr++];
        return (String) data;
    }

    void print(String s) {
        out.print(s);
        currentTab += s.length();
    }

    void print(float f) {
        print((f >= 0.0? " " : "") + formatFloat(f) + " ");
    }

    void nextPrintZone() {
        var nextZone = 14 - (currentTab % 14);
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

    float inputFLOAT() {
        while (true) {
            try {
                return in.nextFloat();
            } catch (InputMismatchException e) {
                print("Please enter a valid number");
                println();
            }
        }
    }

    String inputSTRING() {
        return in.nextLine();
    }

    void runtimeError(String error) {
        throw new RuntimeException(error);
    }

    void pushReturnAddress(int address) {
        returnAddressStack.push(address);
    }

    int popReturnAddress() {
        return returnAddressStack.pop();
    }

    @Override
    public void run() {

    }

    public Scanner getIn() {
        return in;
    }

    public void setIn(Scanner in) {
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
