package com.masprog.shopping_list_api.product;

public final class ProductNameNormalizer {

    private ProductNameNormalizer() {
    }

    public static String normalize(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        return name.trim().replaceAll("\\s+", " ");
    }
}