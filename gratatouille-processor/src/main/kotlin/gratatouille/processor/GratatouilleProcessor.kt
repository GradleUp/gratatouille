package gratatouille.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ksp.writeTo

class GratatouilleProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("gratatouille.GTaskAction")

        val coordinates = options.get("gratatouilleCoordinates") ?: error("Gratatouille requires coordinates of the implementation module")
        symbols.forEach {
            when (it) {
                is KSFunctionDeclaration -> {
                    it.toGTaskAction().apply {
                        entryPoint().writeTo(codeGenerator, Dependencies.ALL_FILES)

                        taskFile(coordinates).let { fileSpec ->
                            codeGenerator.createNewFile(
                                Dependencies.ALL_FILES,
                                "",
                                "META-INF/gratatouille/${fileSpec.packageName.replace(".", "/")}/${fileSpec.name}.kt",
                                ""
                            ).writer().use {
                                fileSpec.writeTo(it)
                            }
                        }
                    }
                }

                else -> error("@GTaskAction is only valid on functions")
            }
        }
        return emptyList()
    }
}

class GratatouilleProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return GratatouilleProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options
        )
    }
}

internal val classpathProperty = Property(InputFiles, classpath, false, false, false)