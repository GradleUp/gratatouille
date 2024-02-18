package gratatouille.processor

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.*

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

/**
 * Suppresses usage of internal symbols
 * @See https://publicobject.com/2024/01/30/internal-visibility/
 *
 * @Suppress(
 *     "CANNOT_OVERRIDE_INVISIBLE_MEMBER",
 *     "INVISIBLE_MEMBER",
 *     "INVISIBLE_REFERENCE",
 * )
 */
internal val suppressInternalAnnotationSpec = AnnotationSpec.builder(ClassName("kotlin", "Suppress"))
    .addMember("%S", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")
    .addMember("%S", "INVISIBLE_MEMBER")
    .addMember("%S", "INVISIBLE_REFERENCE")
    .build()

internal fun KSPropertyDeclaration.isPublic(): Boolean {
    return modifiers.none {
        it in setOf(Modifier.PRIVATE, Modifier.PROTECTED, Modifier.INTERNAL)
    }
}

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