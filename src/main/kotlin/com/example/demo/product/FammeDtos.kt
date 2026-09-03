package com.example.demo.product

data class FammeProductsResponse(
    val products: List<FammeProduct> = emptyList(),
)

data class FammeProduct(
    val title: String,
    val handle: String,
    val vendor: String? = null,
    val variants: List<ProductVariant> = emptyList(),
)
