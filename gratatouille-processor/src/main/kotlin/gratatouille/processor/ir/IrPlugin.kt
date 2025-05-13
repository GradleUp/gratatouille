package gratatouille.processor.ir

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Origin
import com.squareup.kotlinpoet.ksp.toClassName
import gratatouille.processor.capitalizeFirstLetter
import gratatouille.processor.decapitalizeFirstLetter

class IrExtension(
  val name: String,
  val packageName: String,
  val simpleName: String,
  val hasProjectParameter: Boolean
)

class IrApplyFunction(
  val packageName: String,
  val simpleName: String,
)

class IrPlugin(
  val id: String,
  val packageName: String,
  val simpleName: String,
  val extension: IrExtension?,
  val applyFunction: IrApplyFunction?
)

internal fun KSClassDeclaration.toIrPlugin(logger: KSPLogger): IrPlugin? {
  val annotation = annotations.first { it.shortName.asString() == "GExtension" }

  val pluginId = annotation.arguments.first { it.name?.asString() == "pluginId" }.value as String
  var extensionName = annotation.arguments.first { it.name?.asString() == "extensionName" }
    .takeIf { it.origin != Origin.SYNTHETIC }?.value?.toString()

  val constructor = getConstructors().singleOrNull()
  var hasProjectParameter = false
  if (constructor != null) {
    constructor.parameters.forEach {
      if (it.type.resolve().toClassName().toString() == "org.gradle.api.Project") {
        if (!hasProjectParameter) {
          hasProjectParameter = true
        } else {
          logger.error("Gratatouille: only one 'Project' constructor parameter is allowed in @GExtension classes.")
        }
      } else {
        logger.error("Gratatouille: @GExtension classes only support one constructor parameter of 'org.gradle.apiProject' type.")
      }
    }
  }

  if (extensionName == null) {
    extensionName = simpleName.asString().removeSuffix("Extension").decapitalizeFirstLetter()
  }

  val extension = IrExtension(
    extensionName,
    packageName.asString(),
    simpleName.asString(),
    hasProjectParameter
  )
  return IrPlugin(
    pluginId,
    packageName.asString(),
    extensionName.capitalizeFirstLetter().maybeAddPluginSuffix(),
    extension,
    null
  )
}

internal fun String.maybeAddPluginSuffix():  String {
  return if (endsWith("Plugin")) this else this + "Plugin"
}
internal fun KSFunctionDeclaration.toIrPlugin(logger: KSPLogger): IrPlugin? {
  val annotation = annotations.first { it.shortName.asString() == "GPlugin" }

  val pluginId = annotation.arguments.first { it.name?.asString() == "id" }.value as String

  if (!hasSingleProjectParameter()) {
    logger.error("Gratatouille: @GPlugin functions must have a single 'Project' parameter.")
    return null
  }

  return IrPlugin(
    pluginId,
    packageName.asString(),
    simpleName.asString().capitalizeFirstLetter().maybeAddPluginSuffix(),
    null,
    IrApplyFunction(packageName.asString(), simpleName.asString())
  )
}

private fun KSFunctionDeclaration.hasSingleProjectParameter(): Boolean {
  if (parameters.size != 1 || parameters.single().type.resolve().toClassName().toString() != "org.gradle.api.Project") {
    return false
  }

  return true
}
