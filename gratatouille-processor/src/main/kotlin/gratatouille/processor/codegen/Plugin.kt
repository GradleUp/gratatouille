package gratatouille.processor.codegen

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.withIndent
import gratatouille.processor.capitalizeFirstLetter
import gratatouille.processor.ir.IrPlugin

fun IrPlugin.plugin(): FileSpec {
  return FileSpec.builder(
    packageName = packageName,
    fileName = simpleName.capitalizeFirstLetter() + "Plugin",
  )
    .addType(typeSpec())
    .apply {
      if (extensionName != null) {
        addProperty(
          PropertySpec.builder(extensionName, ClassName(packageName, simpleName))
            .receiver(ClassNames.Project)
            .getter(
              FunSpec.getterBuilder()
                .addAnnotation(AnnotationSpec.builder(ClassName("kotlin.jvm", "Synchronized")).build())
                .addCode(
                  buildCodeBlock {
                    add(
                      "val existing = extensions.getByName(%S) as %T?\n",
                      extensionName,
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
                        extensionName,
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
  return TypeSpec.classBuilder(simpleName.capitalizeFirstLetter() + "Plugin")
    .addSuperinterface(ClassNames.Plugin.parameterizedBy(ClassNames.Project)).addModifiers(KModifier.ABSTRACT)
    .addFunction(
      FunSpec.builder("apply")
        .addParameter(ParameterSpec("target", ClassNames.Project))
        .addModifiers(KModifier.OVERRIDE)
        .addCode(buildCodeBlock {
          if (extensionName != null) {
            add(
              "target.extensions.create(%S, %T::class.java, target)",
              extensionName,
              ClassName(packageName, simpleName)
            )
          } else {
            add("%M(target)", MemberName(packageName, simpleName))
          }
        })
        .build()
    ).build()
}

object ClassNames {
  val Project = ClassName("org.gradle.api", "Project")
  val Plugin = ClassName("org.gradle.api", "Plugin")
}