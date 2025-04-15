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
    .apply {
      if (extension != null) {
        // A Kotlin function for Kotlin callers that don't want to go through the plugin ceremony
        addProperty(
          PropertySpec.builder(extension.name, ClassName(packageName, simpleName))
            .receiver(ClassNames.Project)
            .getter(
              FunSpec.getterBuilder()
                .addAnnotation(AnnotationSpec.builder(ClassName("kotlin.jvm", "Synchronized")).build())
                .addCode(
                  buildCodeBlock {
                    add(
                      "val existing = extensions.getByName(%S) as %T?\n",
                      extension.name,
                      ClassName(packageName, simpleName)
                    )
                    add("return if (existing != null) {\n")
                    withIndent {
                      add("existing\n")
                    }
                    add("} else {\n")
                    withIndent {
                      add(
                        "extensions.create(%S, %T::class.java, this)\n",
                        extension.name,
                        ClassName(packageName, simpleName)
                      )
                    }
                    add("}\n")
                  }).build()
            )
            .build()
        )
      }
    }
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