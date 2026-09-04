package com.example.demo.product

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
class ProductRepository(
    private val jdbcClient: JdbcClient,
) {
    fun findAll(): List<Product> =
        jdbcClient
            .sql(
                """
                SELECT id, title, vendor, price, handle, variants::text AS variants_json,
                       COALESCE(jsonb_array_length(variants), 0) AS variant_count
                FROM product
                ORDER BY id
                """.trimIndent(),
            ).query { rs, _ ->
                Product(
                    id = rs.getLong("id"),
                    title = rs.getString("title"),
                    vendor = rs.getString("vendor"),
                    price = rs.getBigDecimal("price"),
                    handle = rs.getString("handle"),
                    variantsJson = rs.getString("variants_json"),
                    variantCount = rs.getInt("variant_count"),
                )
            }.list()

    fun findByTitleContaining(query: String): List<Product> =
        jdbcClient
            .sql(
                """
                SELECT id, title, vendor, price, handle, variants::text AS variants_json,
                       COALESCE(jsonb_array_length(variants), 0) AS variant_count
                FROM product
                WHERE title ILIKE :pattern
                ORDER BY id
                """.trimIndent(),
            ).param("pattern", "%$query%")
            .query { rs, _ ->
                Product(
                    id = rs.getLong("id"),
                    title = rs.getString("title"),
                    vendor = rs.getString("vendor"),
                    price = rs.getBigDecimal("price"),
                    handle = rs.getString("handle"),
                    variantsJson = rs.getString("variants_json"),
                    variantCount = rs.getInt("variant_count"),
                )
            }.list()

    fun insert(
        title: String,
        vendor: String?,
        price: BigDecimal?,
        handle: String,
        variantsJson: String?,
    ): Long =
        jdbcClient
            .sql(
                """
                INSERT INTO product (title, vendor, price, handle, variants)
                VALUES (:title, :vendor, :price, :handle, CAST(:variantsJson AS jsonb))
                RETURNING id
                """.trimIndent(),
            ).param("title", title)
            .param("vendor", vendor)
            .param("price", price)
            .param("handle", handle)
            .param("variantsJson", variantsJson ?: "[]")
            .query(Long::class.java)
            .single()

    fun upsert(
        title: String,
        vendor: String?,
        price: BigDecimal?,
        handle: String,
        variantsJson: String?,
    ) {
        jdbcClient
            .sql(
                """
                INSERT INTO product (title, vendor, price, handle, variants)
                VALUES (:title, :vendor, :price, :handle, CAST(:variantsJson AS jsonb))
                ON CONFLICT (handle) DO UPDATE SET
                    title = EXCLUDED.title,
                    vendor = EXCLUDED.vendor,
                    price = EXCLUDED.price,
                    variants = EXCLUDED.variants
                """.trimIndent(),
            ).param("title", title)
            .param("vendor", vendor)
            .param("price", price)
            .param("handle", handle)
            .param("variantsJson", variantsJson ?: "[]")
            .update()
    }
}
