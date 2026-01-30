package com.riakgu.digilo.product;

public enum ProductSortOption {
    LATEST,      // createdAt DESC
    TRENDING,    // orderCount DESC
    PRICE_ASC,   // minPrice ASC
    PRICE_DESC   // minPrice DESC
}
