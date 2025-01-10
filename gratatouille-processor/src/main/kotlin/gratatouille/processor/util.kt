package gratatouille.processor

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.*
import gratatouille.processor.codegen.toTypeName
import gratatouille.processor.ir.Property

fun String.capitalizeFirstLetter(): String {
    val builder = StringBuilder(length)
    var isCapitalized = false
    forEach {
        builder.append(if (!isCapitalized && it.isLetter()) {
            isCapitalized = true
            it.toString().uppercase()
        } else {
            it.toString()
        })
    }
    return builder.toString()
}

fun String.decapitalizeFirstLetter(): String {
    val builder = StringBuilder(length)
    var isCapitalized = false
    forEach {
        builder.append(if (!isCapitalized && it.isLetter()) {
            isCapitalized = true
            it.toString().lowercase()
        } else {
            it.toString()
        })
    }
    return builder.toString()
}

internal fun KSPropertyDeclaration.isPublic(): Boolean {
    return modifiers.none {
        it in setOf(Modifier.PRIVATE, Modifier.PROTECTED, Modifier.INTERNAL)
    }
}

internal val optInGratatouilleInternalAnnotationSpec = AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
    .addMember("%T::class", ClassName("gratatouille", "GratatouilleInternal"))
    .build()

internal fun TypeSpec.Builder.addConstructorProperties(properties: List<Property>) = apply {
    primaryConstructor(
        FunSpec.constructorBuilder()
            .apply {
                properties.forEach {
                    addParameter(ParameterSpec(it.name, it.toTypeName()))
                }
            }
        .build()
    )
    properties.forEach {
        addProperty(PropertySpec.builder(it.name, it.toTypeName()).initializer("%L", it.name).build())
    }
}

internal fun String?.toCodeBlock(): CodeBlock {
    return if (this == null) {
        CodeBlock.of("null")
    } else {
        CodeBlock.of("%S", this)
    }
}