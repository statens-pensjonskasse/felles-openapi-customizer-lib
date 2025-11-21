package no.spk.felles.openapi

import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

internal class OpenAPICustomizerTest {

    @Test
    fun `Gets a customized OpenAPI spec`() {

        val customizer = OpenAPICustomizer(
            customizeWithSecuritySchemes = true,
            customizeWithStandardHeaders = true,
        )
        val api = customizer.readAndCustomizeOpenAPI("/openapi.yaml")
        api shouldNotBe null

        val (first) = api.security
        first.shouldContainKey("SpkToken")
        first.shouldContainKey("BasicAuth")
    }
}