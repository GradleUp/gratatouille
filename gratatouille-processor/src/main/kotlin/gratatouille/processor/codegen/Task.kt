package gratatouille.processor.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import gratatouille.processor.*
import gratatouille.processor.ir.Classpath
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
import gratatouille.processor.ir.Type
import gratatouille.processor.ir.inputs
import gratatouille.processor.ir.isInput
import gratatouille.processor.ir.iterate
import gratatouille.processor.ir.outputs
import gratatouille.processor.ir.properties


internal fun IrTask.taskFile(): FileSpec {
  val className = taskClassName()

  val fileSpec = FileSpec.builder(className)
      .addFunction(register())
      .addType(task())
      .addType(workParameters())
      .addFunction(invoke2())
      .addType(workAction())
      .build()

  return fileSpec
}

private fun IrTask.register(): FunSpec {
  val defaultTaskName = annotationName ?: taskName()
  val properties = parameters.properties

  return FunSpec.builder(registerName())
      .addModifiers(KModifier.INTERNAL)
      .receiver(ClassName("org.gradle.api", "Project"))
      .returns(ClassName("org.gradle.api.tasks", "TaskProvider").parameterizedBy(taskClassName()))
      .addParameter(
          ParameterSpec.builder(taskName, ClassName("kotlin", "String"))
              .defaultValue(defaultTaskName.toCodeBlock())
              .build()
      )
      .addParameter(
          ParameterSpec.builder(taskDescription, ClassName("kotlin", "String").copy(nullable = true))
              .defaultValue(description.toCodeBlock())
              .build()
      )
      .addParameter(
          ParameterSpec.builder(taskGroup, ClassName("kotlin", "String").copy(nullable = true))
              .defaultValue(group.toCodeBlock())
              .build()
      )
      .addParameter(
          ParameterSpec.builder(extraClasspath, ClassName("org.gradle.api.file", "FileCollection").copy(nullable = true))
              .defaultValue("null")
              .build()
      )
      .apply {
        properties.filter { it.type.isInput() }.forEach {

          addParameter(
              ParameterSpec.builder(it.name, it.type.toProviderType())
                  .build()
          )
        }

        (properties.outputs + this@register.returnValues).forEach {
          if (it.manuallyWired) {
            addParameter(
                ParameterSpec.builder(it.name, it.type.toProviderType())
                    .build()
            )
          }
        }
      }
      .addCode(
          buildCodeBlock {
            add(
                "val configuration = this@%L.configurations.detachedConfiguration()\n",
                registerName(),
            )
            if (implementationCoordinates != null) {
              add("configuration.dependencies.add(dependencies.create(%S))\n", implementationCoordinates)
            }
            add("return tasks.register(${taskName},%T::class.java) {\n", taskClassName())
            withIndent {
              add("it.description = ${taskDescription}\n")
              add("it.group = ${taskGroup}\n")
              add("it.${classpath}.from(configuration)\n")
              add("if (extraClasspath != null) {\n")
              withIndent {
                add("it.${classpath}.from(extraClasspath)\n")
              }
              add("}\n")
              add("// inputs\n")
              properties.inputs.forEach {
                when (it.type) {
                  is InputFiles, Classpath -> {
                    add("it.%L.from(%L)\n", it.name, it.name)
                  }

                  else -> {
                    add("it.%L.set(%L)\n", it.name, it.name)
                  }
                }
              }

              add("// outputs\n")
              (properties.outputs + this@register.returnValues).forEach {
                if (it.manuallyWired) {
                  add("it.%L.set(%L)\n", it.name, it.name)
                } else {
                  val method = when (it.type) {
                    is OutputDirectory -> "dir"
                    is KotlinxSerializableOutput, is OutputFile -> "file"
                    else -> error("Gratatouille: invalid output type for '${it.name}': ${it.type}")
                  }
                  val fileName = when (it.type) {
                    is OutputDirectory -> it.type.fileName
                    is KotlinxSerializableOutput -> it.type.fileName
                    is OutputFile -> it.type.fileName
                    else -> error("Gratatouille: invalid output type for '${it.name}': ${it.type}")
                  }

                  add(
                      "it.%L.set(this@%L.layout.buildDirectory.$method(%L))\n",
                      it.name,
                      registerName(),
                      "\"gtask/\${${taskName}}/${fileName}\""
                  )
                }
              }
            }
            add("}\n")
          }
      )
      .build()
}


private fun Type.toProviderType(): TypeName {
  return when (this) {
    is OutputDirectory, InputDirectory -> ClassName("org.gradle.api.provider", "Provider")
        .parameterizedBy(ClassName("org.gradle.api.file", "Directory"))

    is KotlinxSerializableInput, is KotlinxSerializableOutput, is OutputFile, InputFile -> ClassName(
        "org.gradle.api.provider",
        "Provider"
    )
        .parameterizedBy(ClassName("org.gradle.api.file", "RegularFile"))

    InputFiles, Classpath -> ClassName("org.gradle.api.file", "FileCollection")
    is JvmType -> typename.toGradleProvider()
  }
}

private fun IrTask.task(): TypeSpec {
  return TypeSpec.classBuilder(taskClassName().simpleName)
      .apply {
        if (pure) {
          addAnnotation(
              AnnotationSpec.builder(ClassName("org.gradle.api.tasks", "CacheableTask"))
                  .build()
          )
        } else {
          addAnnotation(
              AnnotationSpec.builder(ClassName("org.gradle.work", "DisableCachingByDefault"))
                  .addMember("because = %S", "This task has side effects and is not cacheable.")
                  .build()
          )
          addKdoc("This task is impure and not cacheable")
          addInitializerBlock(CodeBlock.of("outputs.upToDateWhen { false }\n"))
        }
      }
      .addModifiers(KModifier.ABSTRACT, KModifier.INTERNAL)
      .superclass(ClassName("org.gradle.api", "DefaultTask"))
      .apply {
        (listOf(classpathParameter) + parameters.properties + returnValues).forEach {
          addProperty(
              it.toPropertySpec()
          )
        }
      }
      .addFunction(
          FunSpec.Companion.builder(workerExecutor)
              .addModifiers(KModifier.ABSTRACT)
              .addAnnotation(
                  AnnotationSpec.builder(ClassName("javax.inject", "Inject")).build()
              )
              .returns(ClassName("org.gradle.workers", "WorkerExecutor"))
              .build()
      )
      .addFunction(isolate())
      .addFunction(isolate2())
      .addFunction(taskAction())
      .build()
}

/**
 * This is hardwired because we don't want to add dependencies to the api module
 */
private fun isolate(): FunSpec {
  return FunSpec.builder("isolate")
      .addTypeVariable(TypeVariableName("T"))
      .receiver(TypeVariableName("T"))
      .returns(TypeVariableName("T"))
      .addCode(
          """
            @kotlin.Suppress("UNCHECKED_CAST")
            return when (this) {
                is Set<*> -> {
                    this.map { it.isolate() }.toSet() as T
                }
        
                is List<*> -> {
                    this.map { it.isolate() } as T
                }
        
                is Map<*, *> -> {
                    entries.map { it.key.isolate() to it.value.isolate() }.toMap() as T
                }
        
                else -> this
            }
            """.trimIndent(),
          ClassName("org.gradle.api.file", "FileCollection")
      )
      .addModifiers(KModifier.PRIVATE)
      .build()
}

/**
 * This is hardwired because we don't want to add dependencies to the api module
 */
private fun isolate2(): FunSpec {
  return FunSpec.builder("isolate2")
      .receiver(ClassName("org.gradle.api.file", "FileCollection"))
      .returns(ClassName("kotlin.collections", "List").parameterizedBy(ClassName("kotlin", "Any")))
      .addCode(
          """
            return buildList {
                asFileTree.visit { 
                    add(it.file)
                    add(it.path)
                }
            }
            """.trimIndent(),

          )
      .addModifiers(KModifier.PRIVATE)
      .build()
}

/**
 * This is hardwired because we don't want to add dependencies to the api module
 */
private fun invoke2(): FunSpec {
  return FunSpec.builder("invoke2")
      .receiver(ClassName("java.lang.reflect", "Method"))
      .addParameter(ParameterSpec.builder("obj", ClassName("kotlin", "Any").copy(nullable = true)).build())
      .addParameter(ParameterSpec.builder("args", ClassName("kotlin", "Any").copy(nullable = true)).addModifiers(KModifier.VARARG).build())
      .returns(ClassName("kotlin", "Any").copy(nullable = true))
      .addCode(
          """
          try {
            return invoke(obj, *args)
          } catch (e: %T) {
            /**
             * Unwrap the exception so it's displayed in Gradle error messages
             */
            throw e.cause!!
          }
      """.trimIndent(),
          ClassName("java.lang.reflect", "InvocationTargetException")
      )
      .addModifiers(KModifier.PRIVATE)
      .build()
}

private fun IrTask.taskAction(): FunSpec {
  return FunSpec.builder("taskAction")
      .addAnnotation(
          AnnotationSpec.builder(ClassName("org.gradle.api.tasks", "TaskAction"))
              .build()
      )
      .addCode(
          buildCodeBlock {
            add("${workerExecutor}().noIsolation().submit(%T::class.java) {\n", workActionClassName())
            withIndent {
              add("it.${classpath} = ${classpath}.files\n")
              parameters.properties.forEach {
                workActionProperty(it)
              }
              returnValues.forEach {
                workActionProperty(it)
              }
            }
            add("}\n")
          }
      )
      .build()
}

private fun CodeBlock.Builder.workActionProperty(property: IrTaskProperty) {
  val extra = buildCodeBlock {
    when (property.type) {
      InputDirectory,
      InputFile,
      is KotlinxSerializableInput,
        -> {
        add(".asFile")
        add(if (property.optional) ".orNull" else ".get()")
      }

      is KotlinxSerializableOutput,
      is OutputDirectory,
      is OutputFile,
        -> {
        add(".asFile.get()")
      }

      InputFiles, Classpath -> {
        add(".isolate2()")
      }

      is JvmType -> {
        add(if (property.optional) ".orNull?" else ".get()")
        add(".isolate()")
      }
    }
  }
  add("it.%L = %L%L\n", property.name, property.name, extra)
}

private fun PropertySpec.Builder.annotateInput(
    packageName: String,
    simpleName: String,
    internal: Boolean,
    optional: Boolean,
) = apply {
  if (internal) {
    addAnnotation(
        AnnotationSpec.builder(ClassName("org.gradle.api.tasks", "Internal"))
            .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
            .build()
    )
  } else {
    if (optional) {
      addAnnotation(
          AnnotationSpec.builder(ClassName("org.gradle.api.tasks", "Optional"))
              .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
              .build()
      )
    }
    addAnnotation(
        AnnotationSpec.builder(ClassName(packageName, simpleName))
            .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
            .build()
    )
  }
}

private fun IrTaskProperty.toPropertySpec(): PropertySpec {
  val builder = when (type) {
    is InputDirectory -> {
      PropertySpec.builder(name, ClassName("org.gradle.api.file", "DirectoryProperty"))
          .annotateInput("org.gradle.api.tasks", "InputDirectory", internal, optional)
          .addAnnotation(
              AnnotationSpec.builder(ClassName("org.gradle.api.tasks", "PathSensitive"))
                  .addMember(CodeBlock.of("%T.RELATIVE", ClassName("org.gradle.api.tasks", "PathSensitivity")))
                  .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                  .build()
          )
    }

    is KotlinxSerializableInput, is InputFile -> {
      PropertySpec.builder(name, ClassName("org.gradle.api.file", "RegularFileProperty"))
          .annotateInput("org.gradle.api.tasks", "InputFile", internal, optional)
          .addAnnotation(
              AnnotationSpec.builder(ClassName("org.gradle.api.tasks", "PathSensitive"))
                  .addMember(CodeBlock.of("%T.RELATIVE", ClassName("org.gradle.api.tasks", "PathSensitivity")))
                  .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                  .build()
          )
    }

    is InputFiles, Classpath -> {
      PropertySpec.builder(name, ClassName("org.gradle.api.file", "ConfigurableFileCollection"))
          .apply {
            if (type is InputFiles) {
              annotateInput("org.gradle.api.tasks", "InputFiles", internal, optional)
              addAnnotation(
                  AnnotationSpec.builder(ClassName("org.gradle.api.tasks", "PathSensitive"))
                      .addMember(CodeBlock.of("%T.RELATIVE", ClassName("org.gradle.api.tasks", "PathSensitivity")))
                      .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                      .build()
              )
            } else {
              annotateInput("org.gradle.api.tasks", "Classpath", internal, optional)
            }
          }
    }

    is JvmType -> {
      PropertySpec.builder(name, type.typename.toGradleProperty())
          .annotateInput("org.gradle.api.tasks", "Input", internal, optional)
    }

    is OutputDirectory -> {
      PropertySpec.builder(name, ClassName("org.gradle.api.file", "DirectoryProperty"))
          .addAnnotation(
              AnnotationSpec.builder(ClassName("org.gradle.api.tasks", "OutputDirectory"))
                  .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                  .build()
          )
    }

    is KotlinxSerializableOutput, is OutputFile -> {
      PropertySpec.builder(name, ClassName("org.gradle.api.file", "RegularFileProperty"))
          .addAnnotation(
              AnnotationSpec.builder(ClassName("org.gradle.api.tasks", "OutputFile"))
                  .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                  .build()
          )
    }
  }


  return builder.addModifiers(KModifier.ABSTRACT).build()
}

private fun TypeName.toGradleProperty(): TypeName {
  return when {
    this is ParameterizedTypeName && this.rawType == ClassName("kotlin.collections", "Map") -> {
      ClassName("org.gradle.api.provider", "MapProperty").parameterizedBy(this.typeArguments)
    }

    this is ParameterizedTypeName && this.rawType == ClassName("kotlin.collections", "List") -> {
      ClassName("org.gradle.api.provider", "ListProperty").parameterizedBy(this.typeArguments)
    }

    this is ParameterizedTypeName && this.rawType == ClassName("kotlin.collections", "Set") -> {
      ClassName("org.gradle.api.provider", "SetProperty").parameterizedBy(this.typeArguments)
    }

    this is ClassName -> ClassName("org.gradle.api.provider", "Property").parameterizedBy(this)
    else -> error("Gratatouille: cannot convert '$this' to a Gradle property")
  }
}

private fun TypeName.toGradleProvider(): TypeName {
  return ClassName("org.gradle.api.provider", "Provider").parameterizedBy(this)
}

private fun IrTask.workAction(): TypeSpec {
  return TypeSpec.classBuilder(workActionClassName().simpleName)
      .addModifiers(KModifier.PRIVATE, KModifier.ABSTRACT)
      .addSuperinterface(ClassName("org.gradle.workers", "WorkAction").parameterizedBy(workParametersClassName()))
      .addFunction(workActionExecute())

      .build()
}

private fun IrTask.workActionExecute(): FunSpec {
  return FunSpec.builder("execute")
      .addModifiers(KModifier.OVERRIDE)
      .addCode(
          buildCodeBlock {
            add("with(parameters) {\n")
            withIndent {
              if (implementationCoordinates != null) {
                add("%T(\n", ClassName("java.net", "URLClassLoader"))
                withIndent {
                  add("${classpath}.map { it.toURI().toURL() }.toTypedArray(),\n")
                  add("%T.getPlatformClassLoader()\n", ClassName("java.lang", "ClassLoader"))
                }
                add(")")
                add(".loadClass(%S)\n", entryPointClassName().canonicalName)
                add(".declaredMethods.single()\n")
                add(".invoke2(\n")
                withIndent {
                  add("null,\n")
                  addRunArguments(this@workActionExecute)
                }
                add(")\n")
              } else {
                add("%T.run(\n", entryPointClassName())
                withIndent {
                  addRunArguments(this@workActionExecute)
                }
                add(")\n")
              }
            }
            add("}\n")
          })
      .build()
}

private fun CodeBlock.Builder.addRunArguments(irTask: IrTask) {
  with(irTask) {
    parameters.iterate(
        onProperty = {
          add("%L,\n", it.name)
        },
        onLoggerParameter = {
          add("object: %T{\n", biConsumer)
          withIndent {
            // Sadly, there is no way to inject logging yet: https://github.com/gradle/gradle/issues/16991
            add("val logger = %T.getLogger(%S)\n", ClassName("org.gradle.api.logging", "Logging"), irTask.functionName)
            add("override fun accept(t: Int, u: String) {\n")
            withIndent {
              add("logger.log(%T.entries[t], u)\n", ClassName("org.gradle.api.logging", "LogLevel"))
            }
            add("}\n")
          }
          add("},\n")
        }
    )
    returnValues.forEach {
      add("%L,\n", it.name)
    }
  }
}

private fun IrTask.workParameters(): TypeSpec {
  return TypeSpec.interfaceBuilder(workParametersClassName().simpleName)
      .addModifiers(KModifier.PRIVATE)
      .addSuperinterface(ClassName("org.gradle.workers", "WorkParameters"))
      .apply {
        addProperty(
            PropertySpec.builder(
                classpath,
                ClassName("kotlin.collections", "Set").parameterizedBy(ClassName("java.io", "File"))
            ).mutable(true).build()
        )
        parameters.properties.forEach {
          addProperty(PropertySpec.builder(it.name, it.toTypeName()).mutable(true).build())
        }
        returnValues.forEach {
          addProperty(PropertySpec.builder(it.name, it.toTypeName()).mutable(true).build())
        }
      }
      .build()
}

private fun IrTask.registerName(): String {
  return "register" + this.functionName.capitalizeFirstLetter() + "Task"
}

private fun IrTask.taskName(): String {
  return this.functionName.decapitalizeFirstLetter()
}

private fun IrTask.taskClassName(): ClassName {
  val simpleName = this.functionName.capitalizeFirstLetter() + "Task"
  return ClassName(this.packageName, simpleName)
}

internal fun IrTask.workParametersClassName(): ClassName {
  val simpleName = this.functionName.capitalizeFirstLetter() + "WorkParameters"
  return ClassName(this.packageName, simpleName)
}


private fun IrTask.workActionClassName(): ClassName {
  val simpleName = this.functionName.capitalizeFirstLetter() + "WorkAction"
  return ClassName(this.packageName, simpleName)
}
