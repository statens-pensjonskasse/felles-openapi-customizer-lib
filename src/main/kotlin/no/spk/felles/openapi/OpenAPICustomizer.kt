package no.spk.felles.openapi

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.HeaderParameter
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.parser.OpenAPIV3Parser
import java.io.FileNotFoundException

/**
 * Takes an OpenAPI specification and customizes all paths with the standard
 * security scheme and headers typically used in SPK.
 *
 * These headers are normally omitted from the OpenAPI specification for
 * practical purposes and provided to the client and consumed by the server
 * using implicit infrastructure libraries such as `felles-outbound-lib`. This
 * class allows adding these headers back in, in order for the server to show a
 * complete specification using SwaggerUI.
 *
 * There are two standard security schemes typically supported in SPK:
 * 1. Token based authentication using a security token.
 * 2. Basic authentication using the employee's username and password.
 *
 * If [customizeWithSecuritySchemes] is `true`, both are added.
 *
 * There are three standard headers supported in SPK:
 * 1. `X-Application-Id` which contains the caller's application identity.
 * 1. `X-Correlation-Id` which correlates the invocation across systems.
 * 1. `X-Request-Origin` which contains the origin's application identity.
 *
 * If [customizeWithStandardHeaders] is `true`, all three are added.
 *
 * @param customizeWithSecuritySchemes if security schemes should be added
 * @param customizeWithStandardHeaders if standard headers should be added
 */
class OpenAPICustomizer(
    private val customizeWithSecuritySchemes: Boolean = true,
    private val customizeWithStandardHeaders: Boolean = true,
) {

    /**
     * Takes the filename of an OpenAPI specification on classpath as input,
     * reads the file, customizes it with the given security schemes and
     * headers, and returns it as an [OpenAPI] object.
     *
     * The [OpenAPI] object can be provided to Spring as a `@Bean` in order for
     * documentation providers such as Springdoc to show the specification on
     * the server using SwaggerUI.
     *
     * ```kotlin
     * @Bean
     * fun createOpenAPI() {
     *     val customizer = OpenAPICustomizer(
     *         customizeWithSecuritySchemes = true,
     *         customizeWithStandardHeaders = true,
     *     )
     *     return customizer.readAndCustomizeOpenAPI("/openapi/openapi.yaml")
     * }
     * ```
     *
     * Note that the above requires the OpenAPI specification to be unpacked
     * into `/openapi/openapi.yaml` on the classpath.
     *
     * This is well-suited for OpenAPI specifications written contract-first,
     * where the specification is available as a file.
     *
     * If using Springdoc, it is also advisable to disable scanning of packages
     * adding `springdoc.packages-to-scan=none` to Spring Boot's configuration,
     * otherwise annotated resources might show up twice in the specification.
     *
     * @param openApiYamlFilenameOnClasspath filename of YAML file containing OpenAPI specification on classpath
     * @return read and customized OpenAPI object
     */
    fun readAndCustomizeOpenAPI(openApiYamlFilenameOnClasspath: String): OpenAPI {
        val api = OpenAPIV3Parser().read(openApiYamlFilenameOnClasspath)
            ?: throw FileNotFoundException("Could not read OpenAPI specification in file $openApiYamlFilenameOnClasspath on classpath")
        return customizeOpenAPI(api)
    }

    /**
     * Customizes an existing OpenAPI specification with the given security
     * schemes and headers.
     *
     * The [io.swagger.v3.oas.models.OpenAPI] object can be provided to Spring as a `@Bean` in order for
     * documentation providers such as Springdoc to show the specification on
     * the server using SwaggerUI.
     *
     * ```kotlin
     * @Bean
     * fun createOpenAPI() {
     *     val customizer = OpenAPICustomizer(
     *         customizeWithSecuritySchemes = true,
     *         customizeWithStandardHeaders = true,
     *     )
     *     val generatedOpenAPI = OpenAPI()
     *     return customizer.customizeOpenAPI(generatedOpenAPI)
     * }
     * ```
     *
     * This is well-suited for OpenAPI specifications that are generated from
     * annotated source code, after which these customizations are applied.
     *
     * @return customized OpenAPI object
     */
    fun customizeOpenAPI(api: OpenAPI): OpenAPI {
        if (customizeWithSecuritySchemes) {
            api.addSecurityItem(
                SecurityRequirement()
                    .addList("SpkToken")
                    .addList("BasicAuth")
            )
            api.components
                .addSecuritySchemes(
                    "SpkToken", SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .`in`(SecurityScheme.In.HEADER)
                        .name("Authorization")
                        .description("De fleste kall mellom tjenester i SPK bruker et autentiseringstoken for å autentisere kallet i test og produksjon.")
                )
                .addSecuritySchemes(
                    "BasicAuth", SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("basic")
                        .description("Du kan også bruke ditt eget brukernavn og passord for å autentisere kallet i test. Dette funker ikke i produksjon, der kun tokenbasert autentisering er tillatt.")
                )
        }
        if (customizeWithStandardHeaders) {
            api.components
                .addParameters(
                    "xApplicationId", HeaderParameter()
                        .name("X-Application-Id")
                        .required(true)
                        .schema(StringSchema())
                        .example("SwaggerUI")
                )
                .addParameters(
                    "xCorrelationId", HeaderParameter()
                        .name("X-Correlation-Id")
                        .required(true)
                        .schema(StringSchema())
                        .example("f8c0d93c-8761-4d35-9b4a-51ac9d12c319")
                )
                .addParameters(
                    "xRequestOrigin", HeaderParameter()
                        .name("X-Request-Origin")
                        .required(true)
                        .schema(StringSchema())
                        .example("SwaggerUI")
                )
            api.paths.forEach { (_, path) ->
                path.readOperations().forEach { operation ->
                    operation.addParametersItem(HeaderParameter().`$ref`("#/components/parameters/xApplicationId"))
                    operation.addParametersItem(HeaderParameter().`$ref`("#/components/parameters/xCorrelationId"))
                    operation.addParametersItem(HeaderParameter().`$ref`("#/components/parameters/xRequestOrigin"))
                }
            }
        }
        return api
    }
}
