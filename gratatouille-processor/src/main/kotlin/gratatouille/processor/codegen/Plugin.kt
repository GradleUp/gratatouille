package gratatouille.processor.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import gratatouille.processor.ir.IrPlugin

fun IrPlugin.plugin(): FileSpec {
  return FileSpec.builder(
    packageName = packageName,
    fileName = simpleName,
  )
    .addType(typeSpec())
    .build()
}

private fun IrPlugin.typeSpec(): TypeSpec {
  return TypeSpec.classBuilder(simpleName)
    .addSuperinterface(ClassNames.Plugin.parameterizedBy(ClassNames.Project)).addModifiers(KModifier.ABSTRACT)
    .addFunction(
      FunSpec.builder("apply")
        .addParameter(ParameterSpec("target", ClassNames.Project))
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

object ClassNames {
  val Project = ClassName("org.gradle.api", "Project")
  val Plugin = ClassName("org.gradle.api", "Plugin")
}