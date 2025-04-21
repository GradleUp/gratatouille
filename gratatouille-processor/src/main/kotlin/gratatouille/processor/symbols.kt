package gratatouille.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

internal val stringConsumer = ClassName("java.util.function", "Consumer").parameterizedBy(ClassName("kotlin", "String"))