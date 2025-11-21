# felles-openapi-customizer-lib

Takes an OpenAPI specification and customizes all paths with the standard
security scheme and headers typically used in SPK.

These headers are normally omitted from the OpenAPI specification for
practical purposes and provided to the client and consumed by the server
using implicit infrastructure libraries such as `felles-outbound-lib`. This
class allows adding these headers back in, in order for the server to show a
complete specification using SwaggerUI.

There are two standard security schemes typically supported in SPK:

1. Token based authentication using a security token.
2. Basic authentication using the employee's username and password.

There are three standard headers supported in SPK:

1. `X-Application-Id` which contains the caller's application identity.
2. `X-Correlation-Id` which correlates the invocation across systems.
3. `X-Request-Origin` which contains the origin's application identity.

## Usage

### Maven

Add the following dependency to your project:

```xml
<!-- add this to your pom.xml -->
<dependency>
    <groupId>no.spk.felles</groupId>
    <artifactId>felles-openapi-customizer-lib</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### Customize an existing OpenAPI-spec (contract-first)

Takes the filename of an OpenAPI specification on classpath as input,
reads the file, customizes it with the given security schemes and
headers, and returns it as an `OpenAPI` object.

The `OpenAPI` object can be provided to Spring as a `@Bean` in order for
documentation providers such as Springdoc to show the specification on
the server using SwaggerUI.

```kotlin
@Bean
fun createOpenAPI() {
    val customizer = OpenAPICustomizer(
        customizeWithSecuritySchemes = true,
        customizeWithStandardHeaders = true,
    )
    return customizer.readAndCustomizeOpenAPI("/openapi/openapi.yaml")
}
```

Note that the above requires the OpenAPI specification to be unpacked
into `/openapi/openapi.yaml` on the classpath.

This is well-suited for OpenAPI specifications written contract-first,
where the specification is available as a file.

If using Springdoc, it is also advisable to disable scanning of packages
adding `springdoc.packages-to-scan=none` to Spring Boot's configuration,
otherwise annotated resources might show up twice in the specification.

### Customize a generated OpenAPI-spec (code-first)

Customizes an existing OpenAPI specification with the given security
schemes and headers.

The `OpenAPI` object can be provided to Spring as a `@Bean` in order for
documentation providers such as Springdoc to show the specification on
the server using SwaggerUI.

```kotlin
@Bean
fun createOpenAPI() {
    val customizer = OpenAPICustomizer(
        customizeWithSecuritySchemes = true,
        customizeWithStandardHeaders = true,
    )
    val generatedOpenAPI = OpenAPI()
    return customizer.customizeOpenAPI(generatedOpenAPI)
}
```

This is well-suited for OpenAPI specifications that are generated from
annotated source code, after which these customizations are applied.

## Development

### Requirements

Requirements to build the project on your machine:

* JDK
* Maven

### Build

Run the following command to build the project:

```shell
mvn clean install
```

### Branch and release

1. Branch from `main`.
2. Create a pull-request and merge to `main`.
3. This will release a new version.
