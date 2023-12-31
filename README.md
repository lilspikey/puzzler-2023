# Puzzler 2023

This year the idea was to do something with the programming language "BASIC".

I chose to write a compiler that would compile Basic programs (e.g. from http://www.vintage-basic.net/games.html)
to [Java byte code](https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-6.html).  To do this I am using
the [ASM](https://asm.ow2.io/) library.

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

To compile BASIC to Java you can run the resulting jar file (`--list` shows the intermediate AST):
```
$ java -jar target/puzzler2023-0.0.1.jar example.bas --list
10 A=1.0
20 B=(A+4.0)
30 PRINT "A = ";A
example_bas.class
```

Then you can use Java to run the generated class file:
```
$ java example_bas 
A = 1
```

Alternatively you can compile and run the `.bas` file in one step with the `--run` flag:
```
$ java -jar target/puzzler2023-0.0.1.jar example.bas --run
A =  1
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
         0: ldc_w         #260                // float 0.0f
         3: fstore_2
         4: ldc_w         #260                // float 0.0f
         7: fstore_1
         8: ldc_w         #261                // float 1.0f
        11: fstore_1
        12: fload_1
        13: ldc_w         #262                // float 4.0f
        16: fadd
        17: fstore_2
        18: aload_0
        19: ldc_w         #264                // String A =
        22: invokevirtual #202                // Method print:(Ljava/lang/String;)V
        25: aload_0
        26: fload_1
        27: invokevirtual #266                // Method print:(F)V
        30: aload_0
        31: invokevirtual #225                // Method println:()V
        34: return
        35: athrow
      StackMapTable: number_of_entries = 1
        frame_type = 255 /* full_frame */
          offset_delta = 35
          locals = []
          stack = [ class java/lang/Throwable ]
      LineNumberTable:
        line 178: 35
      LocalVariableTable:
        Start  Length  Slot  Name   Signature
           35       1     0  this   Lruntime/BasRuntime;
```

Decompiling the generated code (to Java) looks like:

```
public void run() {
    float var2 = 0.0F;
    float var1 = 0.0F;
    var1 = 1.0F;
    var2 = var1 + 4.0F;
    this.print("A = ");
    this.print(var1);
    this.println();
}
```

Which is a fairly pleasingly direct translation and means (at least for simple code) performance
would be roughly similar to Java itself.  Though the fact that all arithmetic is done using floats
might not be so good.

## Notes and limitations

* Functions calls and arrays essentially sharing the same syntax makes life harder for the compiler. 
* All local variables are "hoisted" and initialised at the top-level in the generated byte code.  This is done,
  because the scoping of variables in BASIC seems to be pretty simple.  Explicit hoisting keeps things straight forward,
  as it ensures that variables are always accessible outside of loops, if previously only assigned inside a loop.
* `GOSUB` is not implemented using the `JSR` and `RET` instructions. It seems those are no longer 
  used by the Java Compiler. It does not seem easy (possible?) to use them in the Java ASM library
  without a lot of work (messing with stack frames etc). So instead a fake approach is used
  whe all `GOSUB`s are tracked and we push an int onto a `Dequeue` (userland stack) which the RETURN statements use 
  with a lookup switch instruction to jump back to the correct line.  For small numbers of `GOSUB`
  statements this should be pretty quick/small, but as the number of `GOSUB`s increase we will be generating
  more and more code for the `RETURN`.  The `Dequeue` was used, instead of the actual JVM stack, as it helps side step
  issues with class file verification and leaving things on the stack when jumping in a non-structured way.
* Pairing of `FOR` + `NEXT` is done statically at compile time
* Less "structured" the BASIC code has problems (e.g. if statements that `GOTO` earlier lines)
* If this was a real project, then I'd spend more time making the main parser do more validation.  Most things should
  get throw an error if there is a problem, but sometimes the errors will not provide much information about where
  the problem might really be.
* Currently not everything is supported, but a few of the programs from http://www.vintage-basic.net/games.html
  should run:
  * http://www.vintage-basic.net/bcg/aceyducey.bas
  * http://www.vintage-basic.net/bcg/amazing.bas
  * http://www.vintage-basic.net/bcg/bagels.bas
  * http://www.vintage-basic.net/bcg/banner.bas
  * http://www.vintage-basic.net/bcg/bug.bas
  * http://www.vintage-basic.net/bcg/digits.bas
  * http://www.vintage-basic.net/bcg/hi-lo.bas
  * http://www.vintage-basic.net/bcg/life.bas (compiles and runs, but not sure it's actually working)
  * http://www.vintage-basic.net/bcg/pizza.bas
* The program http://www.vintage-basic.net/bcg/checkers.bas seems to try to use 0-based arrays!

## Thoughts

So this was partly a proof of concept and it works surprisingly well.  It was definitely good fun trying out
the ASM library.  I learnt quite a bit about Java byte code.  A lot of the problems I had were around the 
need for byte code verification - where Java verifies that the stack and local variables are used with the correct
instructions/data types.  I also ran into quite a few issues with exceptions in the ASM library, but that usually
seemed to coincide with the less "structured" BASIC code.  My assumption is that the ASM library (and the JVM)
are really only meant for "modern" languages.  At least that seems to be the path of least resistance.

To fully support all the features of BASIC (e.g. user-defined functions and arbitrary jumps etc) I suspect I'd need
to change the approach used.  Some possible thoughts for that.

### Switch to using fields on the class instead of local variables

This would allow user defined functions to be methods on the resulting class.  Possibly with smart
optimisation to only do this for variables that need to be accessed inside user-defined functions. 

### Partial interpreter

Basically take more control of the program position.  So instead of generating instructions 
directly we would generate a large switch statement with one `case` per program line.  Roughly:

```
int line = 0;
float var2 = 0.0F;
float var1 = 0.0F;
while (true) {
    switch (line) {
      case 100: 
        var1 = 1.0F;
        line = 200;
        break;
      case 200:
        var2 = var1 + 4.0F;
        line = 300;
        break;
      case 300:
        this.print("A = ");
        this.print(var1);
        this.println();
        return;
    }
}
```

The main advantage of this is that it would statically define all variables and limit all jump instructions to
just within the `case` statements.  That would hopefully mean we would be fighting the verifier less.

## What is supported

* `PRINT`
* `GOTO`
* `GOSUB`
* `RETURN`
* `IF`
* `THEN`
* `INPUT`
* `REM`
* `FOR`
* `STEP`
* `NEXT`
* `END`
* `STOP`
* `DATA`
* `READ`
* `RESTORE`
* `LET`
* `DIM`
* `AND`
* `OR`
* `ON` (`ON GOTO` only)
* `<`, `>`, `>=`, `<=`, `=`, `<>`
* `+`, `-`, `*`, `/`, `^`
* `INT`
* `ABS`
* `SIN`
* `TAB`
* `RND`
* `ASC`
* `CHR$`
* `LEFT$`
* `MID$`
* `MID$`
* `RIGHT$`
* `LEN`
* `SGN`
* `VAL`
* `STR$`
