# API Biblioteca Digital — FastAPI + Redis

Backend do case de estudo: CRUD completo de livros, API REST em Python (FastAPI) com persistência em Redis.

---

## 1. Manual de execução

### Pré-requisitos

Python 3.10 ou superior e um Redis acessível. No Windows, o caminho mais simples é o Docker Desktop (o `docker-compose.yml` deste projeto já sobe o Redis); sem Docker, dá para usar o WSL ou o Memurai.

### Passo 1 — subir o Redis

```bash
docker compose up -d
```

Para conferir se respondeu: `docker exec -it biblioteca-redis redis-cli ping` (deve retornar `PONG`).

### Passo 2 — instalar as dependências

```bash
python -m venv venv

# Windows
venv\Scripts\activate
# Linux / macOS
source venv/bin/activate

pip install -r requirements.txt
```

### Passo 3 — rodar a API

```bash
uvicorn main:app --reload
```

A API sobe em `http://localhost:8000` e a documentação interativa fica em **http://localhost:8000/docs**.

### Passo 4 — testar

Pelo navegador, em `/docs`, cada endpoint tem o botão *Try it out*. Ou, automaticamente:

```bash
python teste_api.py
```

O script cobre o CRUD inteiro, os filtros, as validações (422), o 404 e o 400 do PATCH vazio. Ele usa o banco 9 do Redis, então não apaga os dados do banco 0.

### Passo 5 — abrir o frontend

Com a API no ar, basta abrir `frontend/index.html` no navegador (duplo clique já funciona, porque o CORS está liberado). Para evitar qualquer bloqueio do navegador em arquivos locais, o caminho mais seguro é servir a pasta:

```bash
cd frontend
python -m http.server 5500
```

e acessar `http://localhost:5500`. Se a API estiver em outro endereço, dá para apontar pela URL, sem editar código: `http://localhost:5500/?api=http://192.168.0.10:8000`.

### Variáveis de ambiente (opcionais)

| Variável | Padrão | Função |
|---|---|---|
| `REDIS_HOST` | `localhost` | Host do Redis |
| `REDIS_PORT` | `6379` | Porta do Redis |
| `REDIS_DB` | `0` | Número do banco |

---

## 2. Modelagem da base de dados (Redis)

O Redis não tem tabelas nem `SELECT`, então a modelagem é feita com estruturas de dados. Este projeto usa três chaves:

| Chave | Tipo | Função |
|---|---|---|
| `livro:{id}` | Hash | Guarda os campos de um livro |
| `livros:ids` | Set | Guarda todos os IDs cadastrados |
| `livro:seq` | String (contador) | Gera os IDs com `INCR` |

### Campos do hash `livro:{id}`

| Campo | Tipo lógico | Obrigatório | Regra de validação |
|---|---|---|---|
| `id` | Inteiro | gerado pela API | Sequencial, via `INCR livro:seq` |
| `titulo` | Texto | sim | 1 a 200 caracteres, não pode ser só espaços |
| `autor` | Texto | sim | 1 a 120 caracteres |
| `categoria` | Texto | sim | 1 a 60 caracteres |
| `ano_publicacao` | Inteiro | sim | Entre 1450 e o ano atual + 1 |
| `quantidade_disponivel` | Inteiro | sim | Entre 0 e 100.000 |
| `status` | Texto | derivado | `Disponivel` se a quantidade > 0, senão `Emprestado` |

No Redis todo valor de hash é string; a API converte os números na leitura e na escrita.

### Por que um Set de IDs

Para listar todos os livros seria preciso varrer o banco com `SCAN livro:*`, que é uma operação de custo imprevisível. Mantendo os IDs em `livros:ids`, a listagem é um `SMEMBERS` seguido de um *pipeline* de `HGETALL` — uma única ida ao servidor. O cadastro e a exclusão atualizam o hash e o set juntos, dentro de um pipeline, para não deixar ID órfão.

### Decisão sobre o campo `status`

O enunciado pede o campo `status`, mas ele é informação redundante: um livro com zero exemplares não pode estar disponível. Por isso a API calcula o `status` a partir de `quantidade_disponivel` em vez de aceitá-lo do cliente. Isso elimina a possibilidade de um registro inconsistente (`quantidade = 0` com `status = Disponivel`).

### Exemplo do que fica gravado

```
> HGETALL livro:1
1) "id"                     2) "1"
3) "titulo"                 4) "Dom Casmurro"
5) "autor"                  6) "Machado de Assis"
7) "categoria"              8) "Romance"
9) "ano_publicacao"        10) "1899"
11) "quantidade_disponivel" 12) "3"
13) "status"                14) "Disponivel"

> SMEMBERS livros:ids
1) "1"

> GET livro:seq
"1"
```

### Comandos Redis usados por operação

| Operação da API | Comandos |
|---|---|
| Cadastrar | `INCR livro:seq`, `HSET livro:{id}`, `SADD livros:ids` |
| Listar | `SMEMBERS livros:ids`, `HGETALL livro:{id}` (em pipeline) |
| Consultar por ID | `HGETALL livro:{id}` |
| Atualizar (PUT/PATCH) | `HGETALL livro:{id}`, `HSET livro:{id}` |
| Excluir | `DEL livro:{id}`, `SREM livros:ids` |

---

## 3. Documentação dos endpoints

Base: `http://localhost:8000`. A documentação viva (Swagger) fica em `/docs` e o schema OpenAPI em `/openapi.json`.

| Método | Rota | O que faz | Sucesso |
|---|---|---|---|
| `GET` | `/` | Informações da API | 200 |
| `GET` | `/health` | Verifica a conexão com o Redis | 200 |
| `POST` | `/livros` | Cadastra um livro | 201 |
| `GET` | `/livros` | Lista os livros, com filtros opcionais | 200 |
| `GET` | `/livros/{id}` | Consulta um livro por ID | 200 |
| `PUT` | `/livros/{id}` | Atualiza todos os campos | 200 |
| `PATCH` | `/livros/{id}` | Atualiza apenas os campos enviados | 200 |
| `DELETE` | `/livros/{id}` | Exclui um livro | 200 |

Filtros aceitos em `GET /livros`: `categoria` (igualdade, ignora maiúsculas), `autor` (busca parcial) e `status` (`Disponivel` ou `Emprestado`). Exemplo: `GET /livros?categoria=romance&status=Disponivel`.

### Formato das respostas

Toda resposta de sucesso usa o mesmo envelope, o que simplifica o tratamento no frontend:

```json
{
  "sucesso": true,
  "mensagem": "Livro cadastrado com sucesso (ID 1).",
  "dados": {
    "titulo": "Dom Casmurro",
    "autor": "Machado de Assis",
    "categoria": "Romance",
    "ano_publicacao": 1899,
    "quantidade_disponivel": 3,
    "id": 1,
    "status": "Disponivel"
  }
}
```

Em `GET /livros`, `dados` é uma lista de livros. Em `DELETE`, é `null`.

### Corpo esperado no POST e no PUT

```json
{
  "titulo": "Dom Casmurro",
  "autor": "Machado de Assis",
  "categoria": "Romance",
  "ano_publicacao": 1899,
  "quantidade_disponivel": 3
}
```

No `PATCH`, qualquer subconjunto desses campos. O `status` nunca é enviado pelo cliente.

---

## 4. Tratamento de erros

| Código | Quando acontece | Corpo |
|---|---|---|
| 400 | `PATCH` sem nenhum campo | `{"sucesso": false, "mensagem": "Envie ao menos um campo para atualizar."}` |
| 404 | ID não existe no `GET`, `PUT`, `PATCH` ou `DELETE` | `{"sucesso": false, "mensagem": "Livro 99 nao encontrado."}` |
| 422 | Validação de campo (tipo errado, campo faltando, ano ou quantidade fora da faixa) | ver abaixo |
| 503 | Redis fora do ar ou inacessível | `{"sucesso": false, "mensagem": "Banco de dados indisponivel...", "erro": "..."}` |

O 422 vem com a lista de campos problemáticos, pronta para ser exibida no frontend:

```json
{
  "sucesso": false,
  "mensagem": "Dados invalidos. Corrija os campos indicados.",
  "erros": [
    { "campo": "ano_publicacao", "erro": "Value error, deve estar entre 1450 e 2027" },
    { "campo": "quantidade_disponivel", "erro": "Input should be greater than or equal to 0" }
  ]
}
```

O CORS está liberado para qualquer origem (`allow_origins=["*"]`), de modo que o frontend pode rodar em outra porta sem configuração extra. Em um sistema real, essa lista seria restrita ao domínio do frontend.

---

## 5. Frontend (HTML + CSS + JavaScript)

O frontend está na pasta `frontend/` e é feito só com HTML, CSS e JavaScript puro — sem framework, sem build, sem dependência para instalar. Ele conversa com a API apenas por `fetch`.

| Arquivo | Conteúdo |
|---|---|
| `frontend/index.html` | Estrutura das quatro telas |
| `frontend/style.css` | Estilos |
| `frontend/app.js` | Chamadas à API, validações e mensagens |

### Telas

A navegação é por abas, e cada aba cobre uma operação do CRUD. Em **Consultar** ficam a busca por ID, que mostra a ficha do livro, e a listagem completa, com filtros de categoria, autor e status; cada linha da tabela tem os botões *Editar* e *Excluir*, que levam direto para a tela correspondente com o ID já preenchido. **Cadastrar** traz o formulário do novo livro, com as categorias já usadas sugeridas no campo. **Atualizar** funciona em dois passos: carrega o livro pelo ID e depois salva, com `PATCH` (só o que mudou) ou `PUT` (todos os campos). **Excluir** também é em dois passos: localiza o livro, mostra a ficha dele e só então pede a confirmação.

### Mensagens de sucesso e erro

Toda resposta da API vira um aviso no canto da tela: verde para sucesso, vermelho para erro, sempre com a mensagem que o backend devolveu. Nos erros 422, cada problema aparece também embaixo do campo correspondente, com a borda do input em vermelho — é para isso que serve a lista `erros` do backend.

O cabeçalho tem um indicador que consulta `/health` a cada 30 segundos e mostra três estados: API e Redis online, Redis indisponível (503) ou API offline. Isso evita a confusão clássica de achar que o frontend quebrou quando, na verdade, o backend não está rodando.

### Validação em dois níveis

O `app.js` valida os campos antes de enviar (obrigatórios, tamanho máximo, ano entre 1450 e o ano atual + 1, quantidade inteira não negativa), o que dá retorno imediato ao usuário. Mesmo assim, a API valida tudo de novo — validação de frontend é conveniência, não segurança, já que qualquer um pode chamar a API direto.

---

## 6. Arquivos do projeto

| Arquivo | Conteúdo |
|---|---|
| `main.py` | A API inteira: modelos, validações, acesso ao Redis, rotas e handlers de erro |
| `teste_api.py` | Teste automatizado dos endpoints |
| `requirements.txt` | Dependências Python |
| `docker-compose.yml` | Sobe o Redis |
| `frontend/` | Interface em HTML, CSS e JavaScript |
| `README.md` | Este documento |

### Organização de `main.py`

O arquivo é dividido em seções comentadas: configuração e chaves do Redis, conexão (pool e dependência do FastAPI), modelos Pydantic (`LivroCreate`, `LivroReplace`, `LivroPatch`, `Livro`), a camada de conversão entre o hash do Redis e os modelos, os handlers de erro e, por último, as rotas.

---

## 7. Sobre a entrega

Já estão cobertos: o código-fonte do backend e do frontend, a modelagem da base (seção 2, que serve de base caso o professor queira o documento em arquivo separado), o manual de execução (seção 1), a documentação dos endpoints (seção 3 mais o Swagger em `/docs`), a validação de entradas, as mensagens de sucesso e erro e o tratamento de erros.

Falta o vídeo ou apresentação de demonstração.
