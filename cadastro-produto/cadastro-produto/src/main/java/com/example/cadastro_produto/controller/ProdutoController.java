package com.example.cadastro_produto.controller;

import com.example.cadastro_produto.model.Produto;
import com.example.cadastro_produto.service.ProdutoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    // GET / → redireciona para /cadastrar
    @GetMapping("/")
    public String index() {
        return "redirect:/cadastrar";
    }

    // GET /cadastrar → exibe o formulário vazio
    @GetMapping("/cadastrar")
    public String exibirFormulario(Model model) {
        model.addAttribute("produto", new Produto());
        return "cadastrar";
    }

    // POST /cadastrar → valida, salva e redireciona
    @PostMapping("/cadastrar")
    public String cadastrarProduto(@ModelAttribute Produto produto,
                                   RedirectAttributes redirect) {
        if (produto.getPreco() <= 0 || produto.getEstoque() < 0) {
            redirect.addFlashAttribute("erro", "Dados inválidos! Verifique o preço e o estoque.");
            return "redirect:/cadastrar";
        }
        produtoService.adicionar(produto);
        redirect.addFlashAttribute("sucesso", "Mercadoria registrada com sucesso!");
        return "redirect:/lista";
    }

    // GET /lista → exibe os produtos (com busca opcional)
    @GetMapping("/lista")
    public String exibirLista(@RequestParam(required = false) String busca, Model model) {
        model.addAttribute("produtos", produtoService.buscar(busca));
        model.addAttribute("busca", busca);
        return "lista";
    }

    // POST /excluir/{id} → remove o produto
    @PostMapping("/excluir/{id}")
    public String excluirProduto(@PathVariable Long id) {
        produtoService.excluir(id);
        return "redirect:/lista";
    }
}