package com.example.demo.product

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal

@Controller
class ProductController(
    private val productRepository: ProductRepository,
    private val jsonMapper: JsonMapper,
) {
    @GetMapping("/")
    fun index() = "index"

    @GetMapping("/products/table")
    fun productTable(model: Model): String {
        model.addAttribute("products", productRepository.findAll())
        return "fragments/product-table :: content"
    }

    @PostMapping("/products")
    fun addProduct(
        @RequestParam title: String,
        @RequestParam(required = false) vendor: String?,
        @RequestParam(required = false) price: BigDecimal?,
        @RequestParam handle: String,
        @RequestParam(required = false) variantTitles: List<String>?,
        @RequestParam(required = false) variantPrices: List<String>?,
        model: Model,
    ): String {
        val variants = buildVariantsFromInput(variantTitles, variantPrices)

        if (hasVariantMissingPrice(variants)) {
            model.addAttribute("errorKey", "form.variant.error.missingPrice")
            model.addAttribute("products", productRepository.findAll())
            return "fragments/product-table :: content"
        }

        val resolvedPrice = minVariantPrice(variants) ?: price

        val variantsJson =
            if (variants.isEmpty()) {
                "[]"
            } else {
                jsonMapper.writeValueAsString(variants)
            }

        productRepository.insert(
            title = title.trim(),
            vendor = vendor?.trim()?.ifBlank { null },
            price = resolvedPrice,
            handle = handle.trim(),
            variantsJson = variantsJson,
        )
        model.addAttribute("products", productRepository.findAll())
        return "fragments/product-table :: content"
    }
}
