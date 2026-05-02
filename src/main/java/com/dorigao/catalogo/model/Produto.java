package com.dorigao.catalogo.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Produto(
    UUID id,
    @NotBlank String nome,
    String descricao,
    @Positive BigDecimal preco,
    String categoria,
    int quantidadeEstoque,
    Instant createdAt,
    Instant updatedAt
) {
    public static Produto criar(String nome, String descricao, BigDecimal preco, String categoria, int quantidadeEstoque) {
        var agora = Instant.now();
        return new Produto(
            UUID.randomUUID(),
            nome,
            descricao,
            preco,
            categoria,
            quantidadeEstoque,
            agora,
            agora
        );
    }
}
