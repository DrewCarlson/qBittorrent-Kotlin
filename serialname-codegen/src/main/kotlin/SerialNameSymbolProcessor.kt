package codegen

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.writeTo

private const val MARKER_ANNOTATION = "qbittorrent.models.GenerateSerialNameMapper"
private const val GENERATED_PACKAGE_NAME = "qbittorrent.models"

/**
 * Generates a field name to serial name mapping function for `@Serializable` classes.
 */
class SerialNameSymbolProcessor(
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    class Provider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            return SerialNameSymbolProcessor(codeGenerator = environment.codeGenerator)
        }
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val targetClasses = resolver.getSymbolsWithAnnotation(MARKER_ANNOTATION)
            .filterIsInstance<KSClassDeclaration>()

        targetClasses.forEach { classDeclaration ->
            val mapFunctionBody = CodeBlock.builder()
                .add("return when (fieldName) {\n")
                .indent()
                .apply {
                    classDeclaration.getDeclaredProperties().forEach { property ->
                        property.serialName?.let { serialName ->
                            add("%S -> %S\n", property.simpleName.asString(), serialName)
                        }
                    }
                }
                .add("else -> fieldName")
                .unindent()
                .add("\n}\n")
                .build()
            val mapFunction = FunSpec.builder("map")
                .addParameter("fieldName", String::class)
                .addCode(mapFunctionBody)
                .returns(String::class)
                .build()

            val mapperName = "${classDeclaration.simpleName.asString()}SerialNameMap"
            FileSpec.builder(GENERATED_PACKAGE_NAME, mapperName)
                .addType(
                    TypeSpec.objectBuilder(mapperName)
                        .addModifiers(KModifier.INTERNAL)
                        .addFunction(mapFunction)
                        .addOriginatingKSFile(classDeclaration.containingFile!!)
                        .build()
                )
                .build()
                .writeTo(codeGenerator, aggregating = false)
        }

        return emptyList()
    }

    private val KSPropertyDeclaration.serialName: String?
        get() = annotations
            .firstOrNull { it.shortName.asString() == "SerialName" }
            ?.arguments
            ?.firstOrNull()
            ?.value as? String
}
