# Puzzler 2023

This year the idea was to do something with the programming language "Basic".

I chose to write a compiler that would compile Basic programs (e.g. from http://www.vintage-basic.net/games.html)
to Java byte code.  To do this I am using the [ASM](https://asm.ow2.io/) library.

The parser is a classic hand-rolled recursive descent parser that generates an
"abstract syntax tree" (AST).  Then a visitor can be used to walk that tree.  Two
visitors are implemented:

* One that just prints the tree
* One that uses the ASM library to generated byte code

Theoretically it could be possible to have other visitors to generate
other kinds of output.

## Known limitations

* GOSUB is not implemented as it would require the JSR and RET instructions, but it seems those are no longer used by the Java Compiler.
  It does not seem easy (possible?) to use them in the Java ASM library
  without a lot of work (messing with stack frames etc).
* Pairing of FOR + NEXT is done statically at compile time