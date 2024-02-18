package gratatouille.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

internal fun GTaskAction.taskFile(coordinates: String): FileSpec {
    val className = taskClassName()

    val fileSpec = FileSpec.builder(className)
        .addFunction(register(coordinates))
        .addType(task())
        .addType(workParameters())
        .addType(workAction())
        .build()

    return fileSpec
}

private fun GTaskAction.register(coordinates: String): FunSpec {
    val thisTaskName = name ?: taskName()
    return FunSpec.builder(registerName())
        .addModifiers(KModifier.INTERNAL)
        .receiver(ClassName("org.gradle.api", "Project"))
        .returns(ClassName("org.gradle.api.tasks", "TaskProvider").parameterizedBy(taskClassName()))
        .addParameter(
            ParameterSpec.builder(taskName, ClassName("kotlin", "String"))
                .defaultValue(thisTaskName.toCodeBlock())
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
            ParameterSpec.builder(
                extraClasspath,
                ClassName("org.gradle.api.file", "FileCollection").copy(nullable = true)
            )
                .defaultValue(CodeBlock.of("null"))
                .build()
        )
        .apply {
            this@register.parameters.filter { it.type.isInput() }.forEach {

                addParameter(
                    ParameterSpec.builder(it.name, it.type.toProviderType())
                        .build()
                )
            }

            (this@register.parameters.filter { !it.type.isInput() } + this@register.returnValues).forEach {
                if (it.manuallyWired) {
                    addParameter(
                        ParameterSpec.builder(it.name, it.type.toProviderType())
                            .build()
                    )
                }
            }
        }
        .addCode(
            CodeBlock.builder()
                .add(
                    "val configuration = this@%L.configurations.detachedConfiguration(dependencies.create(%S))\n",
                    registerName(),
                    coordinates
                )
                .add("return tasks.register($taskName,%T::class.java) {\n", taskClassName())
                .indent()
                .add("it.description = $taskDescription\n")
                .add("it.group = $taskGroup\n")
                .add("it.$classpath.from(configuration)\n")
                .apply {
                    add("// infrastructure\n")
                    add("if ($extraClasspath != null) {\n")
                    indent()
                    add("it.$classpath.from($extraClasspath)\n")
                    unindent()
                    add("}\n")
                }
                .apply {
                    add("// inputs\n")
                    this@register.parameters.filter { it.type.isInput() }.forEach {
                        when (it.type) {
                            is InputFiles -> {
                                add("it.%L.from(%L)\n", it.name, it.name)
                            }

                            else -> {
                                add("it.%L.set(%L)\n", it.name, it.name)
                            }
                        }
                    }
                }
                .apply {
                    add("// outputs\n")
                    (this@register.parameters.filter { !it.type.isInput() } + this@register.returnValues).forEach {
                        if (it.manuallyWired) {
                            add("it.%L.set(%L)\n", it.name, it.name)
                        } else {
                            val method = when (it.type) {
                                is OutputDirectory -> "dir"
                                is KotlinxSerializableOutput, is OutputFile -> "file"
                                else -> error("Gratatouille: invalid output type for '${it.name}': ${it.type}")
                            }
                            add(
                                "it.%L.set(this@%L.layout.buildDirectory.$method(%S))\n",
                                it.name,
                                registerName(),
                                "gtask/$thisTaskName/${it.name}"
                            )
                        }
                    }
                }
                .unindent()
                .add("}\n")
                .build()
        )
        .build()
}

private fun Type.isInput(): Boolean {
    return when (this) {
        is OutputDirectory, is OutputFile -> false
        else -> true
    }
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

        InputFiles -> ClassName("org.gradle.api.file", "FileCollection")
        is JvmType -> typename.toGradleProvider()
    }
}

private fun GTaskAction.task(): TypeSpec {
    return TypeSpec.classBuilder(taskClassName().simpleName)
        .addAnnotation(
            AnnotationSpec.builder(ClassName("org.gradle.api.tasks", "CacheableTask"))
                .build()
        )
        .addModifiers(KModifier.ABSTRACT)
        .superclass(ClassName("org.gradle.api", "DefaultTask"))
        .apply {
            (listOf(classpathProperty) + parameters + returnValues).forEach {
                addProperty(
                    it.toPropertySpec()
                )
            }
        }
        .addFunction(
            FunSpec.builder(workerExecutor)
                .addModifiers(KModifier.ABSTRACT)
                .addAnnotation(
                    AnnotationSpec.builder(ClassName("javax.inject", "Inject")).build()
                )
                .returns(ClassName("org.gradle.workers", "WorkerExecutor"))
                .build()
        )
        .addFunction(isolate())
        .addFunction(taskAction())
        .build()
}

private fun isolate(): FunSpec {
    return FunSpec.builder("isolate")
        .addTypeVariable(TypeVariableName("T"))
        .receiver(TypeVariableName("T"))
        .returns(TypeVariableName("T"))
        .addCode(
            """
            @kotlin.Suppress("UNCHECKED_CAST")
            when (this) {
                is Set<*> -> {
                    return this.map { it.isolate() }.toSet() as T
                }
        
                is List<*> -> {
                    return this.map { it.isolate() } as T
                }
        
                is Map<*, *> -> {
                    return entries.map { it.key.isolate() to it.value.isolate() }.toMap() as T
                }
        
                else -> {
                    return this
                }
            }
            """.trimIndent()
        )
        .addModifiers(KModifier.PRIVATE)
        .build()
}

private fun GTaskAction.taskAction(): FunSpec {
    return FunSpec.builder("taskAction")
        .addAnnotation(
            AnnotationSpec.builder(ClassName("org.gradle.api.tasks", "TaskAction"))
                .build()
        )
        .addCode(
            CodeBlock.builder()
                .add("$workerExecutor().noIsolation().submit(%T::class.java) {\n", workActionClassName())
                .indent()
                .apply {
                    (listOf(classpathProperty) + parameters + returnValues).forEach {
                        val extra = CodeBlock.builder()
                            .apply {
                                when (it.type) {
                                    InputDirectory,
                                    InputFile,
                                    is KotlinxSerializableInput,
                                    is KotlinxSerializableOutput,
                                    is OutputDirectory,
                                    is OutputFile
                                    -> {
                                        add(".asFile")
                                    }

                                    InputFiles -> {
                                        add(".files")
                                    }

                                    else -> Unit
                                }
                                if (it.type !is InputFiles) {
                                    if (it.optional) {
                                        add(".orNull?")
                                    } else {
                                        add(".get()")
                                    }
                                }
                                add(".isolate()")
                            }
                            .build()
                        add("it.%L = %L%L\n", it.name, it.name, extra)
                    }
                }
                .unindent()
                .add("}\n")
                .build()
        )
        .build()
}

private fun PropertySpec.Builder.annotateInput(
    packageName: String,
    simpleName: String,
    internal: Boolean,
    optional: Boolean
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

private fun Property.toPropertySpec(): PropertySpec {
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

        is InputFiles -> {
            PropertySpec.builder(name, ClassName("org.gradle.api.file", "ConfigurableFileCollection"))
                .annotateInput("org.gradle.api.tasks", "InputFiles", internal, optional)
                .addAnnotation(
                    AnnotationSpec.builder(ClassName("org.gradle.api.tasks", "PathSensitive"))
                        .addMember(CodeBlock.of("%T.RELATIVE", ClassName("org.gradle.api.tasks", "PathSensitivity")))
                        .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                        .build()
                )
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

private fun GTaskAction.workAction(): TypeSpec {
    return TypeSpec.classBuilder(workActionClassName().simpleName)
        .addModifiers(KModifier.PRIVATE, KModifier.ABSTRACT)
        .addSuperinterface(ClassName("org.gradle.workers", "WorkAction").parameterizedBy(workParametersClassName()))
        .addFunction(workActionExecute())

        .build()
}

private fun GTaskAction.workActionExecute(): FunSpec {
    return FunSpec.builder("execute")
        .addModifiers(KModifier.OVERRIDE)
        .addCode(CodeBlock.builder()
            .add("with(parameters) {\n")
            .indent()
            .add("%T(\n", ClassName("java.net", "URLClassLoader"))
            .indent()
            .add("$classpath.map { it.toURI().toURL() }.toTypedArray(),\n")
            .add("%T.getPlatformClassLoader()\n", ClassName("java.lang", "ClassLoader"))
            .unindent()
            .add(")")
            .add(".loadClass(%S)\n", entryPointClassName().toString())
            .add(".declaredMethods.single()\n")
            .add(".invoke(\n")
            .indent()
            .add("null,\n")
            .apply {
                (parameters + returnValues).forEach {
                    add("%L,\n", it.name)
                }
            }
            .unindent()
            .add(")\n")
            .unindent()
            .add("}\n")
            .build())
        .build()
}

private fun GTaskAction.workParameters(): TypeSpec {
    return TypeSpec.interfaceBuilder(workParametersClassName().simpleName)
        .addModifiers(KModifier.PRIVATE)
        .addSuperinterface(ClassName("org.gradle.workers", "WorkParameters"))
        .apply {
            addProperty(
                PropertySpec.builder(
                    classpath,
                    ClassName("kotlin.collections", "Set")
                        .parameterizedBy(ClassName("java.io", "File"))
                ).mutable(true).build()
            )
            (parameters + returnValues).forEach {
                addProperty(PropertySpec.builder(it.name, it.toTypeName()).mutable(true).build())
            }
        }
        .build()
}

private fun GTaskAction.registerName(): String {
    return "register" + this.functionName.capitalizeFirstLetter() + "Task"
}

private fun GTaskAction.taskName(): String {
    return this.functionName.decapitalizeFirstLetter()
}

private fun GTaskAction.taskClassName(): ClassName {
    val simpleName = this.functionName.capitalizeFirstLetter() + "Task"
    return ClassName(this.packageName, simpleName)
}

internal fun GTaskAction.workParametersClassName(): ClassName {
    val simpleName = this.functionName.capitalizeFirstLetter() + "WorkParameters"
    return ClassName(this.packageName, simpleName)
}


private fun GTaskAction.workActionClassName(): ClassName {
    val simpleName = this.functionName.capitalizeFirstLetter() + "WorkAction"
    return ClassName(this.packageName, simpleName)
}
