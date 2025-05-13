package gratatouille.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

internal val biConsumer = ClassName("java.util.function", "BiConsumer").parameterizedBy(ClassName("kotlin", "Int"), ClassName("kotlin", "String"))