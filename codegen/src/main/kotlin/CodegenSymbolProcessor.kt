package codegen

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.KProperty1

private const val SERIAL_NAME_MAPPER_ANNOTATION = "qbittorrent.models.serialization.GenerateSerialNameMapper"
private const val PROPERTY_SERIALIZER_MAPPER_ANNOTATION =
    "qbittorrent.models.serialization.GeneratePropertySerializerMapper"
private const val ENUM_AS_INT_SERIALIZER_ANNOTATION = "qbittorrent.models.serialization.GenerateEnumAsIntSerializer"
private const val GENERATED_PACKAGE_NAME = "qbittorrent.models"

/**
 * Provides various codegen to reduce code maintenance and simplify the public API.
 *
 * - Generates field name to serial name mapping function for `@Serializable` classes
 * - Generates function to fetch property serializer for class KProperty1s
 * - Generates Enum as Int serializers
 */
class CodegenSymbolProcessor(
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    private var generatedSerializers = false

    class Provider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            return CodegenSymbolProcessor(codeGenerator = environment.codeGenerator)
        }
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        return if (generatedSerializers) {
            resolver.getSymbolsWithAnnotation(PROPERTY_SERIALIZER_MAPPER_ANNOTATION)
                .filterIsInstance<KSClassDeclaration>()
                .forEach(::generateSerializerMappers)
            emptyList()
        } else {
            generatedSerializers = true
            resolver.getSymbolsWithAnnotation(SERIAL_NAME_MAPPER_ANNOTATION)
                .filterIsInstance<KSClassDeclaration>()
                .forEach(::generateSerialNameMappers)

            resolver.getSymbolsWithAnnotation(ENUM_AS_INT_SERIALIZER_ANNOTATION)
                .filterIsInstance<KSClassDeclaration>()
                .forEach(::generateEnumAsIntSerializer)

            resolver.getSymbolsWithAnnotation(PROPERTY_SERIALIZER_MAPPER_ANNOTATION).toList()
        }
    }

    private fun generateEnumAsIntSerializer(classDeclaration: KSClassDeclaration) {
        val enumName = classDeclaration.simpleName.asString()
        val serializerName = "${enumName}Serializer"
        val enumTypeName = classDeclaration.asType(emptyList()).toTypeName()

        val classSpec = TypeSpec.objectBuilder(serializerName)
            .addModifiers(KModifier.INTERNAL)
            .addSuperinterface(KSerializer::class.asTypeName().parameterizedBy(enumTypeName))
            .addProperty(
                PropertySpec.builder("descriptor", SerialDescriptor::class)
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("PrimitiveSerialDescriptor(%S, %T.INT)", enumName, PrimitiveKind::class)
                    .build()
            )
            .addFunction(
                FunSpec.builder("deserialize")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(enumTypeName)
                    .addParameter("decoder", Decoder::class)
                    .addCode("val value = decoder.decodeInt()\n")
                    .addCode("return %T.entries.firstOrNull { it.value == value }\n", enumTypeName)
                    .addCode(
                        "   ?: throw %T(%S)",
                        SerializationException::class,
                        "Unknown $enumName value '\$value'"
                    )
                    .build()
            )
            .addFunction(
                FunSpec.builder("serialize")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("encoder", Encoder::class)
                    .addParameter("value", enumTypeName)
                    .addCode("encoder.encodeInt(value.value)")
                    .build()
            )
            .addOriginatingKSFile(classDeclaration.containingFile!!)
            .build()

        FileSpec.builder("$GENERATED_PACKAGE_NAME.serialization", serializerName)
            .addType(classSpec)
            .addImport("kotlinx.serialization.descriptors", listOf("PrimitiveSerialDescriptor"))
            .build()
            .writeTo(codeGenerator, aggregating = true)
    }

    private fun generateSerializerMappers(classDeclaration: KSClassDeclaration) {
        val classTypeName = classDeclaration.asType(emptyList()).toTypeName()
        val func = FunSpec.builder("getSerializer")
            .addModifiers(KModifier.INTERNAL)
            .addAnnotation(PublishedApi::class)
            .receiver(KProperty1::class.asTypeName().parameterizedBy(classTypeName, STAR))
            .returns(KSerializer::class.asTypeName().parameterizedBy(STAR))
            .addOriginatingKSFile(classDeclaration.containingFile!!)
            .addCode(
                CodeBlock.builder()
                    .apply {
                        add("return when (name) {\n")
                        indent()
                        classDeclaration.getDeclaredProperties().forEach { property ->
                            val propName = property.simpleName.asString()
                            val propSerializer = property.annotations.map { annotation ->
                                if (annotation.shortName.asString() == "Serializable") {
                                    annotation.arguments.first().value as? KSType
                                } else {
                                    null
                                }
                            }.firstOrNull()
                            if (propSerializer == null) {
                                add("%S -> serializer<%T>()\n", propName, property.type.toTypeName())
                            } else {
                                add("%S -> %T\n", propName, propSerializer.toTypeName())
                            }
                        }
                        add("else -> error(\"no serializer found for property\")")
                        unindent()
                        add("\n}\n")
                    }
                    .build()
            )
            .build()

        val mapperName = "${classDeclaration.simpleName.asString()}SerializerMapping"
        FileSpec.builder(GENERATED_PACKAGE_NAME, mapperName)
            .addFunction(func)
            .addImport("kotlinx.serialization", listOf("serializer"))
            .build()
            .writeTo(codeGenerator, aggregating = true)
    }

    private fun generateSerialNameMappers(classDeclaration: KSClassDeclaration) {
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
            .writeTo(codeGenerator, aggregating = true)
    }

    private val KSPropertyDeclaration.serialName: String?
        get() = annotations
            .firstOrNull { it.shortName.asString() == "SerialName" }
            ?.arguments
            ?.firstOrNull()
            ?.value as? String
}
