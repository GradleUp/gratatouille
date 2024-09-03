package gratatouille.processor

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Origin
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName

internal class GTaskAction(
    val packageName: String,
    val functionName: String,
    val annotationName: String?,
    val description: String?,
    val group: String?,
    val parameters: List<Property>,
    val returnValues: List<Property>,
    /**
     * The coordinates where to find the implementation for this action
     * May be null if the plugin is not isolated
     */
    val implementationCoordinates: String?
)

internal sealed interface Type

internal object InputFile : Type
internal object InputFiles : Type
internal object InputDirectory : Type
internal class KotlinxSerializableInput(val typename: TypeName) : Type
internal class KotlinxSerializableOutput(val typename: TypeName) : Type
internal class JvmType(val typename: TypeName) : Type
internal class OutputFile : Type
internal class OutputDirectory : Type

internal class Property(
    val type: Type,
    val name: String,
    val internal: Boolean,
    val optional: Boolean,
    val manuallyWired: Boolean
)


internal fun KSFunctionDeclaration.toGTaskAction(implementationCoordinates: String?): GTaskAction {
    val parameters = mutableListOf<Property>()
    val returnValues = returnType.toReturnValues()
    val reservedNames = setOf(taskName, taskDescription, taskGroup, classpath, workerExecutor)
    val returnValuesNames = returnValues.map { it.name }.toSet()

    this.parameters.forEach { valueParameter ->
        val resolvedType = valueParameter.type.resolve()
        val typename = valueParameter.type.toTypeName()
        val optional = typename.isNullable
        val rawTypename = typename.copy(nullable = false)
        val internal = valueParameter.annotations.containsGInternal()
        val name = valueParameter.name?.asString()
            ?: error("Gratatouille: anonymous parameters are not supported at ${valueParameter.location}")

        val manuallyWired = valueParameter.annotations.containsManuallyWired()
        check(!reservedNames.contains(name)) {
            "Gratatouille: parameter name '${name}' is reserved for internal uses. Please use another name at ${valueParameter.location}."
        }
        check(!returnValuesNames.contains(name)) {
            "Gratatouille: parameter name '${name}' is already used as return value. Please use another name at ${valueParameter.location}."
        }

        val parameterType: Type = when {
            rawTypename == ClassName("gratatouille", "GOutputFile") -> {
                OutputFile()
            }

            rawTypename == ClassName("gratatouille", "GOutputDirectory") -> {
                OutputDirectory()
            }

            rawTypename == ClassName("gratatouille", "GInputFile") -> InputFile
            rawTypename == ClassName("gratatouille", "GInputFiles") -> {
                check(!optional) {
                    "Gratatouille: optional GInputFiles are not supported ${valueParameter.location}"
                }
                InputFiles
            }

            rawTypename == ClassName("gratatouille", "GInputDirectory") -> InputDirectory
            rawTypename.isSimpleJvmObject() -> JvmType(rawTypename)
            resolvedType.isSerializable() -> KotlinxSerializableInput(rawTypename)
            else -> error("Gratatouille: '$rawTypename' is not a supported parameter at ${valueParameter.location}")
        }

        when (parameterType) {
            is OutputDirectory, is OutputFile -> {
                check(!internal) {
                    "Gratatouille: outputs cannot be annotated with @GInternal at ${valueParameter.location}"
                }
                check(!optional) {
                    "Gratatouille: outputs cannot be optional at ${valueParameter.location}"
                }
            }

            else -> {
                check(!manuallyWired) {
                    "Gratatouille: inputs cannot be annotated with @GManuallyWired at ${valueParameter.location}"
                }
            }
        }

        parameters.add(
            Property(
                name = name,
                type = parameterType,
                internal = internal,
                optional = optional,
                manuallyWired = manuallyWired
            )
        )
    }

    val gTaskActionAnnotation = annotations.first { it.shortName.asString() == "GTaskAction" }
    val name = gTaskActionAnnotation.arguments.firstOrNull { it.name?.asString() == "name" }
        ?.takeIf { it.origin != Origin.SYNTHETIC }?.value?.toString()
    val description = gTaskActionAnnotation.arguments.firstOrNull { it.name?.asString() == "description" }
        ?.takeIf { it.origin != Origin.SYNTHETIC }?.value?.toString()
    val group = gTaskActionAnnotation.arguments.firstOrNull { it.name?.asString() == "group" }
        ?.takeIf { it.origin != Origin.SYNTHETIC }?.value?.toString()

    return GTaskAction(
        packageName = this.packageName.asString(),
        functionName = this.simpleName.asString(),
        parameters = parameters,
        returnValues = returnValues,
        annotationName = name,
        description = description,
        group = group,
        implementationCoordinates = implementationCoordinates
    )
}

private fun KSTypeReference?.toReturnValues(): List<Property> {
    if (this == null) {
        return emptyList()
    }

    val typename = toTypeName()
    check(!typename.isNullable) {
        "Gratatouille: optional outputs are not supported $location"
    }

    if (typename == ClassName("kotlin", "Unit")) {
        return emptyList()
    }

    val resolvedType = this.resolve()
    if (resolvedType.isSerializable()) {
        return listOf(Property(KotlinxSerializableOutput(typename), outputFile, false, false, false))
    }
//    if (typename.isSimpleJvmObject()) {
//        return listOf(ReturnValue(outputFile, JvmType(typename)))
//    }

    val declaration = resolvedType.declaration
    check(declaration is KSClassDeclaration) {
        "Gratatouille: only classes are allowed as return values"
    }

    return declaration.getAllProperties().filter { it.isPublic() }.toList().map {
        it.toReturnValue()
    }
}

private fun KSPropertyDeclaration.toReturnValue(): Property {
    val typename = type.toTypeName()
    val resolvedType = type.resolve()

    check(!typename.isNullable) {
        "Gratatouille: optional outputs are not supported $location"
    }
    if (resolvedType.isSerializable()) {
        return Property(KotlinxSerializableOutput(typename), simpleName.asString(), false, false, false)
    }
//    if (typename.isSimpleJvmObject()) {
//        return ReturnValue(outputFile, JvmType(typename))
//    }

    error("Gratatouille: property '${simpleName.asString()}' cannot be serialize to a file at $location")
}


private fun KSType.isSerializable(): Boolean {
    val declaration = this.declaration
    if (declaration !is KSClassDeclaration) {
        return false
    }
    return declaration.annotations.any {
        it.annotationType.toTypeName() == ClassName("kotlinx.serialization", "Serializable")
    }
}

private fun Sequence<KSAnnotation>.containsGInternal(): Boolean {
    return any {
        it.annotationType.toTypeName() == ClassName("gratatouille", "GInternal")
    }
}

private fun Sequence<KSAnnotation>.containsManuallyWired(): Boolean {
    return any {
        it.annotationType.toTypeName() == ClassName("gratatouille", "GManuallyWired")
    }
}

private fun TypeName.isSimpleJvmObject(): Boolean {
    return when (this) {
        is ClassName -> when (this.canonicalName) {
            "kotlin.String", "kotlin.Float", "kotlin.Int", "kotlin.Boolean", "kotlin.Double" -> true
            else -> false
        }

        is ParameterizedTypeName -> when (this.rawType.canonicalName) {
            "kotlin.collections.Set", "kotlin.collections.List", "kotlin.collections.Map" -> {
                this.typeArguments.all {
                    it.isSimpleJvmObject()
                }
            }

            else -> false
        }

        else -> false
    }
}

