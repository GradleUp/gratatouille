package gratatouille.processor.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import gratatouille.processor.ir.IrTask
import gratatouille.processor.ir.InputDirectory
import gratatouille.processor.ir.InputFile
import gratatouille.processor.ir.InputFiles
import gratatouille.processor.ir.JvmType
import gratatouille.processor.ir.KotlinxSerializableInput
import gratatouille.processor.ir.KotlinxSerializableOutput
import gratatouille.processor.ir.OutputDirectory
import gratatouille.processor.ir.OutputFile
import gratatouille.processor.ir.IrTaskProperty
import gratatouille.processor.capitalizeFirstLetter
import gratatouille.processor.ir.Classpath
import gratatouille.processor.ir.IrPropertyParameter
import gratatouille.processor.ir.IrLoggerParameter
import gratatouille.processor.optInGratatouilleInternalAnnotationSpec
import gratatouille.processor.biConsumer

internal fun IrTask.entryPoint(): FileSpec {
  val className = entryPointClassName()

  val fileSpec = FileSpec.builder(className)
    .addAnnotation(optInGratatouilleInternalAnnotationSpec)
    .addType(
      TypeSpec.classBuilder(className.simpleName)
        .addType(
          TypeSpec.companionObjectBuilder()
            .addFunction(funSpec())
            .build()
        ).build()
    )
    .build()

  return fileSpec
}

internal fun IrTask.entryPointClassName(): ClassName {
  val simpleName = this.functionName.capitalizeFirstLetter() + "EntryPoint"
  return ClassName(this.packageName, simpleName)
}

internal fun IrTaskProperty.toTypeName(): TypeName {
  return when (type) {
    InputDirectory -> ClassName("java.io", "File")
    InputFile -> ClassName("java.io", "File")
    InputFiles, Classpath -> ClassName("kotlin.collections", "List").parameterizedBy(ClassName("kotlin", "Any"))
    is OutputFile -> ClassName("java.io", "File")
    is OutputDirectory -> ClassName("java.io", "File")
    is JvmType -> type.typename
    is KotlinxSerializableInput, is KotlinxSerializableOutput -> ClassName("java.io", "File")
  }.copy(nullable = optional)
}


private fun IrTask.funSpec(): FunSpec {
  return FunSpec.builder("run")
    .addAnnotation(AnnotationSpec.builder(ClassName("kotlin.jvm", "JvmStatic")).build())
    .apply {
      this@funSpec.parameters.forEach { parameter ->
        when (parameter) {
          is IrPropertyParameter -> {
            addParameter(ParameterSpec.builder(parameter.property.name, parameter.property.toTypeName()).build())
          }
          is IrLoggerParameter -> {
            addParameter(
              ParameterSpec.builder(
                parameter.name,
                biConsumer
              ).build()
            )
          }
        }

      }
      this@funSpec.returnValues.forEach {
        addParameter(ParameterSpec.builder(it.name, it.toTypeName()).build())
      }
    }
    .addCode(
      CodeBlock.builder()
        .add("%M(\n", MemberName(this@funSpec.packageName, this@funSpec.functionName))
        .indent()
        .apply {
          this@funSpec.parameters.forEach {
            when (it) {
              is IrPropertyParameter -> {
                val extra = when (it.property.type) {
                  is KotlinxSerializableInput -> {
                    CodeBlock.of(".%M()", MemberName("gratatouille", "decodeJson"))
                  }

                  is InputFiles, is Classpath -> {
                    CodeBlock.of(".%M()", MemberName("gratatouille", "toGInputFiles"))
                  }

                  else -> {
                    CodeBlock.of("")
                  }
                }
                add("%L = %L%L,\n", it.property.name, it.property.name, extra)
              }
              is IrLoggerParameter -> {
                add("%L = %T(%L),\n", it.name, ClassName("gratatouille", "DefaultGLogger"), it.name)
              }
            }
          }
        }
        .unindent()
        .add(")")
        .apply {
          when {
            returnValues.isEmpty() -> {
              add("\n")
            }

            returnValues.size == 1 -> {
              add(".%M(%L)", MemberName("gratatouille", "encodeJsonTo"), returnValues.single().name)
            }

            else -> {
              add(".let {\n")
              indent()
              returnValues.forEach {
                add("it.%L.%M(%L)\n", it.name, MemberName("gratatouille", "encodeJsonTo"), it.name)
              }
              unindent()
              add("}\n")
            }
          }
        }
        .build()
    )
    .build()
}
