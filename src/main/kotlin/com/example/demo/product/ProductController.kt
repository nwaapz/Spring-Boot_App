package com.example.demo.product

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal

@Controller
class ProductController(
    private val productRepository: ProductRepository,
    private val jsonMapper: JsonMapper,
) {

    private fun addProductsToModel(model: Model){
        model.addAttribute("products", productRepository.findAll())
    }

    @GetMapping("/")
    fun index() = "index"

    @GetMapping("/search")
    fun search() = "search"

    @GetMapping("/products/search")
    fun searchProducts(
        @RequestParam(required = false) q: String?,
        model: Model,
    ): String {
        val query = q?.trim().orEmpty()
        val products =
            if (query.isEmpty()) {
                productRepository.findAll()
            } else {
                productRepository.findByTitleContaining(query)
            }
        model.addAttribute("products", products)
        model.addAttribute(
            "emptyMessageKey",
            if (query.isEmpty()) "table.empty" else "search.empty",
        )
        return "fragments/product-table :: table"
    }

    @GetMapping("/products/table")
    fun productTable(model: Model): String {
        addProductsToModel(model)
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
            addProductsToModel(model)
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
        addProductsToModel(model)
        return "fragments/product-table :: content"
    }

    @GetMapping("/products/{id}/edit")
    fun editProduct(
        @PathVariable id: Long,
        model: Model,
    ): String {
        val product =
            productRepository.findById(id)
                ?: return "redirect:/"

        model.addAttribute("product", product)
        return "edit"
    }

    @PostMapping("/products/{id}")
    fun updateProduct(
        @PathVariable id: Long,
        @RequestParam title: String,
        @RequestParam(required = false) vendor: String?,
        @RequestParam(required = false) price: BigDecimal?,
        @RequestParam handle: String,
        @RequestParam(required = false) variantTitles: List<String>?,
        @RequestParam(required = false) variantPrices: List<String>?,
        model: Model,
        redirectAttributes: RedirectAttributes,
    ): String {


        val variants = buildVariantsFromInput(variantTitles, variantPrices)

        if (hasVariantMissingPrice(variants)) {

            val existing =
                productRepository.findById(id)
                    ?: return "redirect:/"

            model.addAttribute("product", existing)
            model.addAttribute("errorKey", "form.variant.error.missingPrice")
            return "edit"
        }

        val resolvedPrice = minVariantPrice(variants) ?: price

        val variantsJson =
            if (variants.isEmpty()) {
                "[]"
            } else {
                jsonMapper.writeValueAsString(variants)
            }

        val updated =
            productRepository.update(
                id = id,
                title = title.trim(),
                vendor = vendor?.trim()?.ifBlank { null },
                price = resolvedPrice,
                handle = handle.trim(),
                variantsJson = variantsJson,
            )

        if (!updated) {
            return "redirect:/"
        }

        redirectAttributes.addFlashAttribute("successKey", "notification.product.updated")
        return "redirect:/"
    }
}
