package com.example.demo.product

import java.math.BigDecimal

data class Product(
    val id: Long,
    val title: String,
    val vendor: String?,
    val price: BigDecimal?,
    val handle: String,
    val variantsJson: String?,
    val variantCount: Int,
)
