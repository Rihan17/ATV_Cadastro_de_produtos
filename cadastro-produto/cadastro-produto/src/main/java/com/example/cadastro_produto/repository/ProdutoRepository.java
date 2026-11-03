package com.example.cadastro_produto.repository;

import com.example.cadastro_produto.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByNomeContainingIgnoreCaseOrCategoriaContainingIgnoreCase(
            String nome, String categoria);
}