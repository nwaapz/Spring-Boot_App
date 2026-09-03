package com.example.demo.product

import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper

@Component
class ProductSyncJob(
    private val productRepository: ProductRepository,
    private val jsonMapper: JsonMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient =
        RestClient
            .builder()
            .defaultHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            ).defaultHeader("Accept", "application/json, text/plain, */*")
            .defaultHeader("Accept-Language", "en-US,en;q=0.9")
            .defaultHeader("Referer", "https://famme.no/")
            .build()
    //calling https://famme.no/products.json
    @Scheduled(initialDelay = 0, fixedDelay = Long.MAX_VALUE)
    fun syncProducts() {
        log.info("Fetching products from famme.no ...")

        val response = fetchRemote() ?: fetchFallback()
        if (response == null) {
            log.error("Could not load products from famme.no or fallback data")
            return
        }

        response.products.take(50).forEach { product ->
            val price = minVariantPrice(product.variants)
            val variantsJson = jsonMapper.writeValueAsString(product.variants)

            productRepository.upsert(
                title = product.title,
                vendor = product.vendor,
                price = price,
                handle = product.handle,
                variantsJson = variantsJson,
            )
        }

        log.info("Synced {} products", minOf(response.products.size, 50))
    }

    private fun fetchRemote(): FammeProductsResponse? =
        try {
            restClient
                .get()
                .uri("https://famme.no/products.json")
                .retrieve()
                .body(FammeProductsResponse::class.java)
        } catch (e: Exception) {
            log.warn("Remote fetch blocked ({}), using bundled snapshot", e.message)
            null
        }

    private fun fetchFallback(): FammeProductsResponse? =
        try {
            ClassPathResource("data/famme-products.json").inputStream.use { stream ->
                jsonMapper.readValue(stream, FammeProductsResponse::class.java)
            }
        } catch (e: Exception) {
            log.error("Failed to load fallback products: {}", e.message)
            null
        }
}
