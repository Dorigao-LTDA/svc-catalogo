package com.dorigao.catalogo.controller;

import com.dorigao.catalogo.model.Produto;
import com.dorigao.catalogo.service.CatalogoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {

    private static final Logger log = LoggerFactory.getLogger(CatalogoController.class);
    private final CatalogoService service;

    public CatalogoController(CatalogoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Produto>> listar(@RequestParam(required = false) String categoria) {
        log.info("GET /api/catalogo - categoria={}", categoria);

        if (categoria != null && !categoria.isEmpty()) {
            var produtos = service.buscarPorCategoria(categoria);
            return ResponseEntity.ok(produtos);
        }

        var produtos = service.listarTodos();
        return ResponseEntity.ok(produtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Produto> buscarPorId(@PathVariable UUID id) {
        log.info("GET /api/catalogo/{}", id);

        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Produto> criar(@Valid @RequestBody CriarProdutoRequest request) {
        log.info("POST /api/catalogo - nome={}", request.nome());

        var produto = service.criar(
            request.nome(),
            request.descricao(),
            request.preco(),
            request.categoria(),
            request.quantidadeEstoque()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(produto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Produto> atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarProdutoRequest request) {
        log.info("PUT /api/catalogo/{}", id);

        return service.atualizar(
                id,
                request.nome(),
                request.descricao(),
                request.preco(),
                request.categoria(),
                request.quantidadeEstoque()
            )
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        log.info("DELETE /api/catalogo/{}", id);

        if (service.deletar(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // --- Request DTOs ---

    public record CriarProdutoRequest(
        @NotBlank String nome,
        String descricao,
        @Positive BigDecimal preco,
        String categoria,
        int quantidadeEstoque
    ) {}

    public record AtualizarProdutoRequest(
        String nome,
        String descricao,
        @Positive BigDecimal preco,
        String categoria,
        int quantidadeEstoque
    ) {}
}
