package gratatouille.processor.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import gratatouille.processor.ir.IrPlugin

internal fun IrPlugin.plugin(): FileSpec {
  return FileSpec.builder(
    packageName = packageName,
    // prefix with 'Generated' to avoid having duplicate source files when building the sources jar
    // This typically happens if the users declare their @GPlugin function in a file having the same name
    fileName = "Generated$simpleName",
  )
    .addType(typeSpec())
    .build()
}

private fun IrPlugin.typeSpec(): TypeSpec {
  return TypeSpec.classBuilder(simpleName)
    .addSuperinterface(ClassNames.Plugin.parameterizedBy(target)).addModifiers(KModifier.ABSTRACT)
    .addFunction(
      FunSpec.builder("apply")
        .addParameter(ParameterSpec("target", target))
        .addModifiers(KModifier.OVERRIDE)
        .addCode(buildCodeBlock {
          if (extension != null) {
            add(
              "target.extensions.create(%S, %T::class.java",
              extension.name,
              ClassName(extension.packageName, extension.simpleName)
            )
            if (extension.hasProjectParameter) {
              add(", target")
            }
            add(")")
          }
          if (applyFunction != null) {
            add("%M(target)", MemberName(applyFunction.packageName, applyFunction.simpleName))
          }
        })
        .build()
    ).build()
}

internal object ClassNames {
  val Plugin = ClassName("org.gradle.api", "Plugin")
}