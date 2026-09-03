package com.example.demo.product

import java.math.BigDecimal

data class ProductVariant(
    val id: Long? = null,
    val title: String? = null,
    val price: String? = null,
)

fun buildVariantsFromInput(
    titles: List<String>?,
    prices: List<String>?,
): List<ProductVariant> {
    val titleList = titles.orEmpty()
    val priceList = prices.orEmpty()
    val maxSize = maxOf(titleList.size, priceList.size)

    return (0 until maxSize).mapNotNull { index ->
        val title = titleList.getOrNull(index)?.trim().orEmpty()
        if (title.isBlank()) {
            return@mapNotNull null
        }

        val price = priceList.getOrNull(index)?.trim()?.ifBlank { null }
        ProductVariant(title = title, price = price)
    }
}

fun hasVariantMissingPrice(variants: List<ProductVariant>): Boolean =
    variants.any { variant -> variant.price.isNullOrBlank() }

fun minVariantPrice(variants: List<ProductVariant>): BigDecimal? =
    variants.mapNotNull { it.price?.toBigDecimalOrNull() }.minOrNull()
