# KPoet

KPoet is a Kotlin extensions library on top of [JavaPoet](https://github.com/square/javapoet) that helps you write code that writes code _feel_ like actually writing code.

From their main [Example](https://github.com/square/javapoet#example):

Here's a (boring) `HelloWorld` class:

```java
package com.example.helloworld;

public final class HelloWorld {
  public static void main(String[] args) {
    System.out.println("Hello, JavaPoet!");
  }
}

```

And this is the (exciting) code to generate it with JavaPoet:

```java

MethodSpec main = MethodSpec.methodBuilder("main")
    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
    .returns(void.class)
    .addParameter(String[].class, "args")
    .addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet!")
    .build();

TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
    .addMethod(main)
    .build();

JavaFile javaFile = JavaFile.builder("com.example.helloworld", helloWorld)
    .build();

javaFile.writeTo(System.out);

```

While JavaPoet provides a very nice library that makes it easier to write code that writes Java code, there are a few problems with vanilla JavaPoet code:
1. It is confusing why `MethodSpec` should get defined before writing your `TypeSpec`.
2. The code ordering does not follow normal code flow, so you have to think about the ordering and map it to code.
3. The code does not flow like how you would like to write code, leading to mistakes and making it more difficult to read when you need to generate complex code.

`KPoet` attempts to solve these issues by:
1. Mapping Kotlin DSL builders as close as possible to normal java code. (Yes it's quite possible)
2. Have the code you write appear like normal Java, meaning less thinking and better readibility.
3. Be more concise than JavaPoet so you can write less code but be more expressive.

So using `KPoet` from the previous example:

```kotlin

javaFile("com.example.helloworld") {
  `class`("HelloWorld") {  modifiers(publicFinal)

    `public static`(TypeName.VOID, "main",
        param(Array<String>::class, "args")) {
      statement("\$T.out.println(\$S)", System::class.java, "Hello, JavaPoet!")
    }
  }
}.writeTo(System.out)

```

As you can see, KPoet takes JavaPoet code and turns it into an expressive DSL that tries to map to regular java as much as possible.

if we want to output a method such as this:

```java

public boolean handleAction(String action) {
  switch(action) {
    case "bonus": {
      this.name = "BONUS";
      break;
    }
    default: {
      this.name = "NO BONUS";
      break;
    }
  }

  if (this.name == "BONUS") {
    return true
  } else if (this.name  == "NO BONUS") {
    return false
  }

  throw new IllegalStateException("Did not process proper action")
}

```

We represent it as:

```kotlin

`public`(TypeName.BOOLEAN, "handleAction",
       param(String::class, "action")) {
  switch("action") {
    case("bonus".S) {
       // str -> "\$S", "bonus"
      statement("this.name = ${"BONUS".S}")
      `break`()
    }
    default {
      statement("this.name = ${"NO BONUS".S}")
      `break`()
    }
  }

  `if`("this.name == ${"BONUS".S}") {
    `return`(true.L) // L -> true.toString()
  }.`else if`("this.name == ${"NO_BONUS".S}") {
    `return`(false.L)
  }.end() // end required for `if` and `else if`.

  `throw new`(IllegalStateException::class, "Did not process proper action")
}


```

The next few sections we attempt to mirror the JavaPoet readme, but converted syntax for KPoet, to give you an idea of what the library provides.


### Code & Control Flow

JavaPoet offers APIs to make code generation easier.

We want to write:

```java
void main() {
  int total = 0;
  for (int i = 0; i < 10; i++) {
    total += i;
  }
}

```

And so JavaPoet verbously gives us this `MethodSpec`:

```java

MethodSpec main = MethodSpec.methodBuilder("main")
    .addStatement("int total = 0")
    .beginControlFlow("for (int i = 0; i < 10; i++)")
    .addStatement("total += i")
    .endControlFlow()
    .build();

```

 This is slightly difficult to read and understand. What does this code actually look like when it's outputted? Also note that if you `beginControlFlow()` multiple times and dont `endControlFlow()`, you will receive runtime crashes that are hard to fix depending on implementation.

 With KPoet, you do less thinking about how the code will look:

```kotlin

`package private`(TypeName.VOID, "main") {
  statement("int total = 0")
  `for`("int i = 0; i < 10; i++") {
    statement("total += i")
  }
}

```

Next from their method generator example:
```java

private MethodSpec computeRange(String name, int from, int to, String op) {
  return MethodSpec.methodBuilder(name)
      .returns(int.class)
      .addStatement("int result = 0")
      .beginControlFlow("for (int i = " + from + "; i < " + to + "; i++)")
      .addStatement("result = result " + op + " i")
      .endControlFlow()
      .addStatement("return result")
      .build();
}

```

In KPoet:

```kotlin

fun computeRange(name: String, from: Int, to:Int, op: String) = `package private`(TypeName.Int, name) {
  statement("int result = 0")
  `for`("int i = $from; i < $to; i++") {
    statement("result = result $op i")
  }
  `return`("result")
}

```

### $L for Literals

KPoet has a couple helper methods for cases where we need to pass a literal value to a statement, or code block. The best example is `return`.

```java
addStatement("return \$L", someLiteral)
```

can easily be replaced with:

```kotlin
`return`(someLiteral.L)
```

This simply converts the object to string, but preserving the JavaPoet-like syntax.

### $S is for Strings

When using code that includes string literals, JavaPoet uses `$S` to emit a `string`, wrapping quotation marks to escape it.

With the power of Kotlin string interpolation, we barely need to use $S. For cases where we need to convert it to a string for code output, KPoet provides the `Any?.S` property to simply wrap the object's `toString()` value in quotes.

````kotlin

`public`(String::class, "getStatus", param(TypeName.BOOLEAN, "isReady")) {
  `if`("isReady") {
    `return`("BONUS".S)
  } else {
    `return`("NO BONUS".S)
  }
}

```

which outputs:


```java
public String getStatus(boolean isReady) {
  if (isReady) {
    return "BONUS";
  } else {
    return "NO BONUS";
  }
}

```
### $T is for Types

JavaPoet has spectacular handling of reference types by collecting and importing them to make the code much more readable, KPoet does not provide any extension on top of this functionality.

You will still need to pass that class or `TypeName` to JavaPoet:

```kotlin

`abstract class`("TestClass") {  modifiers(public)
    `package private field`(TypeName.BOOLEAN, isReady, { init(false.L) })
    `package private field`(String::class, isReady, { init("SomeName".S) })

    constructor(param(TypeName.BOOLEAN, isReady)) {
        statement("this.$isReady = $isReady")
    }
}

```


### Methods


KPoet supports all kinds of methods.

You can write `abstract` methods easily:

```kotlin

`abstract class`("HelloWorld") { modifiers(public)
  abstract(TypeName.VOID, "flux") {
    modifiers(protected)
  }
}

```

Which generates:

```kotlin
public abstract class HelloWorld {
  protected abstract void flux();
}
```
