package gratatouille.processor.ir

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Origin
import com.squareup.kotlinpoet.ksp.toClassName

class IrPlugin(
  val id: String,
  val packageName: String,
  val simpleName: String,
  val extensionName: String?
)
internal fun KSClassDeclaration.toIrPlugin(logger: KSPLogger): IrPlugin? {
  val annotation = annotations.first { it.shortName.asString() == "GExtension" }

  val pluginId = annotation.arguments.first { it.name?.asString() == "pluginId" }.value as String
  var extensionName = annotation.arguments.first { it.name?.asString() == "extensionName" }
    .takeIf { it.origin != Origin.SYNTHETIC }?.value?.toString()

  val constructor = getConstructors().singleOrNull()
  if (constructor == null || !constructor.hasSingleProjectParameter()) {
    logger.error("Gratatouille: @GPlugin classes must have a single constructor(Project).")
    return null
  }

  if (extensionName == null) {
    extensionName = pluginId.split(".").last()
  }

  return IrPlugin(
    pluginId,
    packageName.asString(),
    simpleName.asString(),
    null
  )
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
    simpleName.asString(),
    null
  )
}

private fun KSFunctionDeclaration.hasSingleProjectParameter(): Boolean {
  if (parameters.size != 1 || parameters.single().type.resolve().toClassName().toString() != "org.gradle.api.Project") {
    return false
  }

  return true
}
