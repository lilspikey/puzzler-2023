# Puzzler 2023

This year the idea was to do something with the programming language "Basic".

I chose to write a compiler that would compile Basic programs (e.g. from http://www.vintage-basic.net/games.html)
to Java byte code.  To do this I am using the [ASM](https://asm.ow2.io/) library.

I am using the [Vintage BASIC User's guide](http://www.vintage-basic.net/downloads/Vintage_BASIC_Users_Guide.html)
as my reference for correct behaviour of programs.

The parser is a classic hand-rolled recursive descent parser that generates an
"abstract syntax tree" (AST).  Then a visitor can be used to walk that tree.  Two
visitors are implemented:

* One that just prints the tree
* One that uses the ASM library to generated byte code

Theoretically it could be possible to have other visitors to generate
other kinds of output.

## Building and Compiling Basic

This repo uses Maven + Java 17.  Building should be a simple:
```
mvn clean package
```

To compile Basic to Java you can run the `BasicCompiler` class:
```
$ mvn -Dexec.mainClass=BasicCompiler exec:java -Dexec.args=example.bas
[INFO] Scanning for projects...
[INFO] 
[INFO] ----------------< com.littlespikeyland.com:puzzler2023 >----------------
[INFO] Building puzzler2023 0.0.1
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- exec-maven-plugin:3.1.1:java (default-cli) @ puzzler2023 ---
10 A = 1.0
20 B = (A+4.0)
30 PRINT "A = ";A
example_bas.class
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  0.706 s
[INFO] Finished at: 2023-12-16T11:23:47Z
[INFO] ------------------------------------------------------------------------
```

Then you can use Java to run the generated class file:
```
$ java example_bas 
A = 1
```

## Runtime

To make things easier there is a "runtime" (see [BasRuntime](src/main/java/runtime/BasRuntime.java)).  This class
is used as the basis for the final compiler output.  The ASM library is used to generate the `run()` method
of the class.  The entire Basic program is treated as a single Java method.

The `BasRuntime` class provides various methods for handling things like printing, reading input/data etc.

## Example generated code

Given code like

```
10 A = 1
20 B = A + 4
30 PRINT "A = "; A
```

The generated byte code looks like:

```
 public void run();
    descriptor: ()V
    flags: (0x0001) ACC_PUBLIC
    Code:
      stack=2, locals=3, args_size=1
         0: ldc           #159                // float 1.0f
         2: fstore_1
         3: fload_1
         4: ldc           #160                // float 4.0f
         6: fadd
         7: fstore_2
         8: aload_0
         9: ldc           #162                // String A =
        11: invokevirtual #113                // Method print:(Ljava/lang/String;)V
        14: aload_0
        15: fload_1
        16: invokevirtual #164                // Method print:(F)V
        19: aload_0
        20: invokevirtual #165                // Method println:()V
        23: return
        24: athrow
      StackMapTable: number_of_entries = 1
        frame_type = 255 /* full_frame */
          offset_delta = 24
          locals = []
          stack = [ class java/lang/Throwable ]
      LineNumberTable:
        line 106: 24
      LocalVariableTable:
        Start  Length  Slot  Name   Signature
           24       1     0  this   Lruntime/BasRuntime;
```

Decompiling the generated code (to Java) looks like:

```
public void run() {
    float var1 = 1.0F;
    float var2 = var1 + 4.0F;
    this.print("A = ");
    this.print(var1);
    this.println();
}
```

Which is a fairly pleasingly direct translation and means (at least for simple code) performance
would be roughly similar to Java itself.  Though the fact that all arithmetic is done using floats
might not be so good.

## Notes and limitations

* GOSUB is not implemented using the JSR and RET instructions. It seems those are no longer 
  used by the Java Compiler. It does not seem easy (possible?) to use them in the Java ASM library
  without a lot of work (messing with stack frames etc). So instead a fake approach is used
  whe all GOSUBs are tracked and we push an int onto the stack which the RETURN statements use 
  with a lookup switch instruction to jump back to the correct line.  For small numbers of GOSUB
  statements this should be pretty quick/small, but as the number of GOSUBs increase we will be generating
  more and more code for the RETURN.
* Pairing of FOR + NEXT is done statically at compile time
* I suspect the less "structured" the BASIC code the more likely there will be problems
* Currently not everything is supported, but a few of the programs from http://www.vintage-basic.net/games.html
  should run:
  * http://www.vintage-basic.net/bcg/aceyducey.bas
  * http://www.vintage-basic.net/bcg/bagels.bas
  * http://www.vintage-basic.net/bcg/banner.bas
  * http://www.vintage-basic.net/bcg/digits.bas
  * http://www.vintage-basic.net/bcg/hi-lo.bas
