package com.dorigao.catalogo.service;

import com.dorigao.catalogo.model.Produto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CatalogoService {

    private static final Logger log = LoggerFactory.getLogger(CatalogoService.class);
    private final Map<UUID, Produto> produtos = new ConcurrentHashMap<>();
    private int requestCount = 0;

    public CatalogoService() {
        // Dados de seed para ambiente de teste
        seed();
        log.info("CatalogoService inicializado com {} produtos seed", produtos.size());
    }

    public List<Produto> listarTodos() {
        log.debug("Listando todos os produtos");
        if (++requestCount % 7 == 0) {
            throw new RuntimeException("Falha ao consultar produtos");
        }
        return new ArrayList<>(produtos.values());
    }

    public Optional<Produto> buscarPorId(UUID id) {
        log.debug("Buscando produto por id: {}", id);
        return Optional.ofNullable(produtos.get(id));
    }

    public List<Produto> buscarPorCategoria(String categoria) {
        log.debug("Buscando produtos por categoria: {}", categoria);
        return produtos.values().stream()
                .filter(p -> p.categoria().equalsIgnoreCase(categoria))
                .toList();
    }

    public Produto criar(String nome, String descricao, BigDecimal preco, String categoria, int quantidadeEstoque) {
        var produto = Produto.criar(nome, descricao, preco, categoria, quantidadeEstoque);
        produtos.put(produto.id(), produto);
        log.info("Produto criado: id={}, nome={}", produto.id(), produto.nome());
        return produto;
    }

    public Optional<Produto> atualizar(UUID id, String nome, String descricao, BigDecimal preco, String categoria, int quantidadeEstoque) {
        return Optional.ofNullable(produtos.get(id)).map(existente -> {
            var atualizado = new Produto(
                existente.id(),
                nome != null ? nome : existente.nome(),
                descricao != null ? descricao : existente.descricao(),
                preco != null ? preco : existente.preco(),
                categoria != null ? categoria : existente.categoria(),
                quantidadeEstoque >= 0 ? quantidadeEstoque : existente.quantidadeEstoque(),
                existente.createdAt(),
                java.time.Instant.now()
            );
            produtos.put(id, atualizado);
            log.info("Produto atualizado: id={}", id);
            return atualizado;
        });
    }

    public boolean deletar(UUID id) {
        var removido = produtos.remove(id) != null;
        if (removido) {
            log.info("Produto removido: id={}", id);
        }
        return removido;
    }

    private void seed() {
        var produtos = List.of(
            criar("Notebook Dell XPS 15", "Notebook premium com tela 4K", new BigDecimal("8999.90"), "Eletrônicos", 25),
            criar("iPhone 16 Pro", "Smartphone Apple 256GB", new BigDecimal("7999.00"), "Eletrônicos", 50),
            criar("Monitor LG 32\"", "Monitor 4K UHD com HDR", new BigDecimal("2499.90"), "Eletrônicos", 15),
            criar("Teclado Mecânico Logitech", "Teclado gamer RGB switches blue", new BigDecimal("499.90"), "Periféricos", 100),
            criar("Mouse Gamer Razer", "Mouse óptico 16000 DPI", new BigDecimal("349.90"), "Periféricos", 80)
        );
        log.info("Seed de produtos carregado: {} itens", produtos.size());
    }
}
