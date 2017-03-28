package com.grosner.kpoet

import com.squareup.javapoet.*
import kotlin.reflect.KClass

typealias CodeMethod = CodeBlock.Builder.() -> CodeBlock.Builder
typealias MethodMethod = MethodSpec.Builder.() -> MethodSpec.Builder
typealias FieldMethod = FieldSpec.Builder.() -> FieldSpec.Builder

infix fun MethodSpec.Builder.returns(typeName: TypeName) = returns(typeName)!!

infix fun MethodSpec.Builder.returns(typeName: KClass<*>) = returns(typeName.java)!!

inline fun MethodSpec.Builder.code(codeMethod: CodeMethod) = addCode(codeMethod(CodeBlock
        .builder()).build())!!

inline fun MethodSpec.Builder.statement(codeMethod: CodeMethod)
        = addStatement("\$L", codeMethod(CodeBlock.builder()).build().toString())!!

fun MethodSpec.Builder.statement(arg: Args) = addStatement(arg.code, *arg.args)!!

fun MethodSpec.Builder.statement(code: String, vararg args: Any?) = addStatement(code, *args)!!

fun MethodSpec.Builder.comment(comment: String) = addComment(comment)!!

infix fun MethodSpec.Builder.annotation(type: KClass<*>) = addAnnotation(type.java)!!

infix fun MethodSpec.Builder.annotation(type: ClassName) = addAnnotation(type)!!

inline fun MethodSpec.Builder.annotation(className: ClassName,
                                         function: AnnotationSpec.Builder.() -> AnnotationSpec.Builder)
        = addAnnotation(AnnotationSpec.builder(className).function().build())!!

inline fun MethodSpec.Builder.annotation(className: KClass<*>,
                                         function: AnnotationSpec.Builder.() -> AnnotationSpec.Builder)
        = addAnnotation(AnnotationSpec.builder(className.java).function().build())!!

// control flow extensions
inline fun MethodSpec.Builder.`if`(statement: String, vararg args: Any?,
                                   function: CodeMethod)
        = beginControl("if", statement = statement, args = *args, function = function)

inline fun MethodSpec.Builder.`do`(statement: String, vararg args: Any?,
                                   function: CodeMethod)
        = beginControl("do", statement = statement, args = *args, function = function)

fun MethodSpec.Builder.`while`(statement: String, vararg args: Any?) = endControl("while", statement = statement, args = *args)

fun MethodSpec.Builder.`while`(arg: Args) = endControl("while", arg = arg)

infix inline fun MethodSpec.Builder.`else`(function: CodeMethod)
        = nextControl("else", function = function)

inline fun MethodSpec.Builder.`else if`(statement: String, vararg args: Any?,
                                        function: CodeMethod)
        = nextControl("else if", statement = statement, args = *args, function = function)


fun MethodSpec.Builder.end(statement: String = "", vararg args: Any?) = end(Args(statement, args))

fun MethodSpec.Builder.end(arg: Args) =
        (if (arg.code.isNullOrBlank().not()) endControlFlow(arg.code, *arg.args) else endControlFlow())!!

inline fun MethodSpec.Builder.`for`(statement: String, vararg args: Any?,
                                    function: CodeMethod)
        = beginControl("for", statement = statement, args = *args, function = function).endControlFlow()!!

inline fun MethodSpec.Builder.`switch`(statement: String, vararg args: Any?,
                                       function: CodeMethod)
        = beginControl("switch", statement = statement, args = *args, function = function).endControlFlow()!!

fun MethodSpec.Builder.`return`(statement: String, vararg args: Any?) = addStatement("return $statement", *args)!!

fun MethodSpec.Builder.`return`(arg: Args) = addStatement("return ${arg.code}", *arg.args)!!

fun MethodSpec.Builder.`break`() = addStatement("break")!!

fun MethodSpec.Builder.`continue`() = addStatement("continue")!!

fun MethodSpec.Builder.`throw new`(type: KClass<*>, statement: String, vararg arg: Any?)
        = addStatement("throw new \$T(\"$statement\")", type.java, *arg)!!

fun MethodSpec.Builder.`throw new`(type: ClassName, statement: String, vararg arg: Any?)
        = addStatement("throw new \$T(\"$statement\")", type, *arg)!!

inline fun MethodSpec.Builder.nextControl(name: String, statement: String = "", vararg args: Any?,
                                          function: CodeMethod)
        = nextControlFlow("$name${if (statement.isNullOrEmpty()) "" else " ($statement)"}", *args)
        .addCode(function(CodeBlock.builder()).build())!!

inline fun MethodSpec.Builder.beginControl(name: String, statement: String = "", vararg args: Any?,
                                           function: CodeMethod)
        = beginControlFlow("$name${if (statement.isNullOrEmpty()) "" else " ($statement)"}", *args)
        .addCode(function(CodeBlock.builder()).build())!!

inline fun MethodSpec.Builder.endControl(name: String, statement: String = "", vararg args: Any?)
        = endControlFlow("$name${if (statement.isNullOrEmpty()) "" else " ($statement)"}", *args)!!

inline fun MethodSpec.Builder.endControl(name: String, arg: Args)
        = endControlFlow("$name${if (arg.code.isNullOrEmpty()) "" else " (${arg.code})"}", *arg.args)!!