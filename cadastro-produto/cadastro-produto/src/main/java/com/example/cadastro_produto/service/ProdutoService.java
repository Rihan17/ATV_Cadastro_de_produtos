package com.example.cadastro_produto.service;

import com.example.cadastro_produto.model.Produto;
import com.example.cadastro_produto.repository.ProdutoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProdutoService {

    private final ProdutoRepository repository;

    public ProdutoService(ProdutoRepository repository) {
        this.repository = repository;
    }

    public List<Produto> listar() {
        return repository.findAll();
    }

    public List<Produto> buscar(String termo) {
        if (termo == null || termo.isBlank()) {
            return repository.findAll();
        }
        return repository
                .findByNomeContainingIgnoreCaseOrCategoriaContainingIgnoreCase(termo, termo);
    }

    public void adicionar(Produto produto) {
        repository.save(produto);
    }

    public void excluir(Long id) {
        repository.deleteById(id);
    }
}