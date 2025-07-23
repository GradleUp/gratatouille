package gratatouille.processor.ir

import cast.cast
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import gratatouille.processor.*

internal class IrTask(
  val packageName: String,
  val functionName: String,
  val annotationName: String?,
  val description: String?,
  val group: String?,
  val parameters: List<IrParameter>,
  val returnValues: List<IrTaskProperty>,
  /**
   * The coordinates where to find the implementation for this action
   * May be null if the plugin is not isolated
   */
  val implementationCoordinates: String?,
  val pure: Boolean
)

internal val List<IrParameter>.properties: List<IrTaskProperty> get() {
  return filterIsInstance<IrPropertyParameter>().map { it.property }
}
internal fun List<IrParameter>.iterate(
  onProperty: (IrTaskProperty) -> Unit,
  onLoggerParameter: (IrLoggerParameter) -> Unit)
{
  forEach {
    when (it) {
      is IrPropertyParameter -> onProperty(it.property)
      is IrLoggerParameter -> onLoggerParameter(it)
    }
  }
}
internal val List<IrTaskProperty>.inputs: List<IrTaskProperty> get() = filter { it.type.isInput() }
internal val List<IrTaskProperty>.outputs: List<IrTaskProperty> get() = filter { !it.type.isInput() }

internal sealed interface Type

internal object InputFile : Type
internal object InputFiles : Type
internal object Classpath : Type
internal object InputDirectory : Type
internal class KotlinxSerializableInput(val typename: TypeName) : Type
internal class KotlinxSerializableOutput(val typename: TypeName, val fileName: String) : Type
internal class JvmType(val typename: TypeName) : Type
internal class OutputFile(val fileName: String) : Type
internal class OutputDirectory(val fileName: String) : Type

/**
 * Something that is annotated with one of Gradle's input/output annotations.
 */
internal class IrTaskProperty(
  val type: Type,
  val name: String,
  val internal: Boolean,
  val optional: Boolean,
  val manuallyWired: Boolean
)

internal fun Type.isInput(): Boolean {
  return when (this) {
    is OutputDirectory, is OutputFile -> false
    else -> true
  }
}

internal class IrPropertyParameter(val property: IrTaskProperty): IrParameter
internal class IrLoggerParameter(val name: String): IrParameter

/**
 * A parameter in the task action function.
 *
 * Parameters may either be task properties or injected parameters.
 */
internal sealed interface IrParameter

internal fun KSFunctionDeclaration.toGTask(implementationCoordinates: String?, enableKotlinxSerialization: Boolean): IrTask {
  val parameters = mutableListOf<IrParameter>()
  val returnValues = returnType.toReturnValues(enableKotlinxSerialization)
  val reservedNames = setOf(
    // reserved because they clash with Gratatouille built-in parameters and properties
    taskName,
    taskDescription,
    taskGroup,
    classpath,
    workerExecutor,
    extraClasspath,
    // reserved because they clash with Gradle built-in properties
    "project",
  )
  val returnValuesNames = returnValues.map { it.name }.toSet()

  this.parameters.forEach { valueParameter ->
    val resolvedType = valueParameter.type.resolve()
    val typename = valueParameter.type.toTypeName()
    val optional = typename.isNullable
    val rawTypename = typename.copy(nullable = false)
    val internal = valueParameter.annotations.containsGInternal()
    val name = valueParameter.name?.asString()
      ?: error("Gratatouille: anonymous parameters are not supported at ${valueParameter.location}")

    check(!reservedNames.contains(name)) {
      "Gratatouille: parameter name '${name}' is reserved for internal uses. Please use another name at ${valueParameter.location}."
    }
    check(!name.startsWith("is")) {
      // See somewhere around there https://github.com/gradle/gradle/blob/b3169d65b2d6fbf273930cade0fa41ac8303f8be/platforms/core-configuration/model-core/src/main/java/org/gradle/internal/instantiation/generator/AbstractClassGenerator.java#L338
      // Gradle fails in those cases with:
      // Caused by: java.lang.IllegalArgumentException: Cannot have abstract method ApolloGenerateSourcesTask.isFoo(): DirectoryProperty.
      "Gratatouille: parameter name '${name}' starts with 'is' and will not be representable as a Gradle task property. Please choose another name."
    }
    check(!returnValuesNames.contains(name)) {
      "Gratatouille: parameter name '${name}' is already used as return value. Please use another name at ${valueParameter.location}."
    }

    val parameterType: Type = when {
      rawTypename == ClassName(gratatouilleTasksPackageName, "GLogger") -> {
        check(!optional) {
          "Gratatouille: The logger parameter may not be nullable."
        }
        parameters.add(IrLoggerParameter(name))
        return@forEach
      }

      rawTypename == ClassName(gratatouilleTasksPackageName, "GOutputFile") -> {
        OutputFile(valueParameter.fileName())
      }

      rawTypename == ClassName(gratatouilleTasksPackageName, "GOutputDirectory") -> {
        OutputDirectory(valueParameter.fileName())
      }

      rawTypename == ClassName(gratatouilleTasksPackageName, "GInputFile") -> InputFile
      rawTypename == ClassName(gratatouilleTasksPackageName, "GClasspath") -> {
        check(!optional) {
          "Gratatouille: optional GClasspath are not supported ${valueParameter.location}"
        }
        Classpath
      }
      rawTypename == ClassName(gratatouilleTasksPackageName, "GInputFiles") -> {
        check(!optional) {
          "Gratatouille: optional GInputFiles are not supported ${valueParameter.location}"
        }
        InputFiles
      }

      rawTypename.isFile() -> {
        check(internal) {
          "Gratatouille: using java.io.File is only allowed with @GInternal at ${valueParameter.location}. Use @GInputFile or @GOutputFile for input or output files."
        }
        JvmType(rawTypename)
      }
      resolvedType.isSerializable() -> KotlinxSerializableInput(rawTypename)
      else -> {
        val typename = rawTypename.toSimpleJvmType()
        if (typename != null) {
          JvmType(typename)
        } else {
          error("Gratatouille: '$rawTypename' is not a supported parameter at ${valueParameter.location}")
        }
      }
    }

    val manuallyWired = valueParameter.annotations.containsManuallyWired()
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
      IrPropertyParameter(
        IrTaskProperty(
          name = name,
          type = parameterType,
          internal = internal,
          optional = optional,
          manuallyWired = manuallyWired
        )
      )
    )
  }

  val taskAnnotation = annotations.first { it.shortName.asString() == "GTask" }
  val name = taskAnnotation.arguments.firstOrNull { it.name?.asString() == "name" }
    ?.takeIf { it.origin != Origin.SYNTHETIC }?.value?.toString()
  val description = taskAnnotation.arguments.firstOrNull { it.name?.asString() == "description" }
    ?.takeIf { it.origin != Origin.SYNTHETIC }?.value?.toString()
  val group = taskAnnotation.arguments.firstOrNull { it.name?.asString() == "group" }
    ?.takeIf { it.origin != Origin.SYNTHETIC }?.value?.toString()
  val pure = taskAnnotation.arguments.firstOrNull { it.name?.asString() == "pure" }
    ?.takeIf { it.origin != Origin.SYNTHETIC }?.value?.cast<Boolean>() != false

  return IrTask(
    packageName = this.packageName.asString(),
    functionName = this.simpleName.asString(),
    parameters = parameters,
    returnValues = returnValues,
    annotationName = name,
    description = description,
    group = group,
    implementationCoordinates = implementationCoordinates,
    pure = pure
  )
}

private fun KSTypeReference?.toReturnValues(enableKotlinxSerialization: Boolean): List<IrTaskProperty> {
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

  if (!enableKotlinxSerialization) {
    error("Gratatouille: return values are not enabled, use enableKotlinxSerialization.set(true) to opt-in experimental support.")
  }

  val resolvedType = this.resolve()
  if (resolvedType.isSerializable()) {
    return listOf(IrTaskProperty(KotlinxSerializableOutput(typename, outputFile), outputFile, false, false, false))
  }

  val declaration = resolvedType.declaration
  check(declaration is KSClassDeclaration) {
    "Gratatouille: only classes are allowed as return values"
  }

  return declaration.getAllProperties().filter { it.isPublic() }.toList().map {
    it.toReturnValue()
  }
}

private fun KSPropertyDeclaration.toReturnValue(): IrTaskProperty {
  val typename = type.toTypeName()
  val resolvedType = type.resolve()

  check(!typename.isNullable) {
    "Gratatouille: optional outputs are not supported $location"
  }
  if (resolvedType.isSerializable()) {
    return IrTaskProperty(KotlinxSerializableOutput(typename, fileName()), simpleName.asString(), false, false, false)
  }
//    if (typename.isSimpleJvmType()) {
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

private fun Sequence<KSAnnotation>.fileName(): String? {
  return firstOrNull {
    it.annotationType.toTypeName() == ClassName(gratatouilleTasksPackageName, "GFileName")
  }?.arguments
    ?.firstOrNull { it.name?.asString() == "name" }
    ?.value
    ?.toString()
}

private fun KSValueParameter.fileName(): String {
  return annotations.fileName() ?: name!!.asString()
}

private fun KSPropertyDeclaration.fileName(): String {
  return annotations.fileName() ?: simpleName.asString()
}

private fun Sequence<KSAnnotation>.containsGInternal(): Boolean {
  return any {
    it.annotationType.toTypeName() == ClassName(gratatouilleTasksPackageName, "GInternal")
  }
}

private fun Sequence<KSAnnotation>.containsManuallyWired(): Boolean {
  return any {
    it.annotationType.toTypeName() == ClassName(gratatouilleTasksPackageName, "GManuallyWired")
  }
}

private fun TypeName.toSimpleJvmType(): TypeName? {
  return when (this) {
    is ClassName -> when (this.canonicalName) {
      "kotlin.String", "kotlin.Float", "kotlin.Int", "kotlin.Long", "kotlin.Boolean", "kotlin.Double", -> this
      "$gratatouilleTasksPackageName.GAny" -> ClassName("kotlin", "Any").copy(nullable = this.isNullable)
      else -> null
    }

    is ParameterizedTypeName -> when (this.rawType.canonicalName) {
      "kotlin.collections.Set", "kotlin.collections.List", "kotlin.collections.Map" -> {
        val typeArguments = typeArguments.map {
          val arg = it.toSimpleJvmType()
          if (arg == null) {
            return null
          }
          arg
        }
        this.copy(
            typeArguments = typeArguments
        )
      }

      else -> null
    }

    else -> null
  }
}

private fun TypeName.isFile(): Boolean {
  return when (this) {
    is ClassName -> when (this.canonicalName) {
      "java.io.File" -> true
      else -> false
    }
    else -> false
  }
}


