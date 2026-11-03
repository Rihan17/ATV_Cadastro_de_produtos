# ⚔ Cadastro de Produtos — Inventário do Reino

Aplicação web de cadastro de produtos desenvolvida com **Spring Boot**, **Thymeleaf** e **Spring Data JPA**, com interface temática medieval. Permite registrar mercadorias, listar o inventário, buscar por nome ou categoria e excluir itens.

Projeto desenvolvido para a disciplina de **Técnicas de Programação II** (FATEC).

## Funcionalidades

- **Cadastrar** produto com nome, preço, estoque e categoria
- **Listar** todo o inventário em tabela
- **Buscar** por nome ou categoria (parcial e sem diferenciar maiúsculas/minúsculas)
- **Excluir** produto, com confirmação antes de remover
- **Totais** exibidos abaixo da tabela: quantidade de produtos e soma das unidades em estoque
- **Validação** no servidor: preço deve ser maior que 0 e estoque não pode ser negativo
- Mensagens de sucesso e erro após as operações (via flash attributes)

## Tecnologias

| Tecnologia | Uso |
|---|---|
| Java 17 | Linguagem |
| Spring Boot 4.1.2 | Framework base |
| Spring Web MVC | Controllers e rotas |
| Spring Data JPA | Persistência |
| Thymeleaf | Templates HTML |
| H2 (em memória) | Banco de dados |
| Bootstrap 4.6.2 (CDN) | Layout e componentes |
| Google Fonts (Cinzel, MedievalSharp) | Tipografia do tema |
| Maven (wrapper incluso) | Build e dependências |

## Pré-requisitos

- **JDK 17** ou superior
- Conexão com a internet (Bootstrap e fontes são carregados via CDN)

Não é necessário instalar o Maven: o projeto inclui o Maven Wrapper (`mvnw` / `mvnw.cmd`).

## Como executar

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Depois, acesse **http://localhost:8080**. A raiz (`/`) redireciona automaticamente para a tela de cadastro.

Para gerar o `.jar` e executá-lo:

```bash
./mvnw clean package
java -jar target/cadastro-produto-0.0.1-SNAPSHOT.jar
```

Para rodar os testes:

```bash
./mvnw test
```

## Rotas

| Método | Rota | Descrição |
|---|---|---|
| GET | `/` | Redireciona para `/cadastrar` |
| GET | `/cadastrar` | Exibe o formulário de cadastro |
| POST | `/cadastrar` | Valida e salva o produto, depois redireciona para `/lista` |
| GET | `/lista` | Lista os produtos. Aceita `?busca=termo` |
| POST | `/excluir/{id}` | Exclui o produto e redireciona para `/lista` |

## Modelo de dados

Entidade `Produto`:

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | `Long` | Chave primária, gerada automaticamente |
| `nome` | `String` | Nome do produto |
| `preco` | `double` | Preço (deve ser > 0) |
| `categoria` | `String` | Categoria do produto (ex.: Armas, Armaduras, Poções) |
| `estoque` | `int` | Quantidade em estoque (deve ser ≥ 0) |

## Banco de dados e console H2

O projeto usa o H2 em **memória**, então **os dados são perdidos ao reiniciar a aplicação**. A configuração está em `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:cadastro
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.h2.console.enabled=true
```

Com a aplicação rodando, o console do banco fica em **http://localhost:8080/h2-console**. Use a JDBC URL `jdbc:h2:mem:cadastro`, usuário `sa` e senha em branco.

## Estrutura do projeto

```
cadastro-produto/
├── pom.xml
├── mvnw / mvnw.cmd
└── src/
    ├── main/
    │   ├── java/com/example/cadastro_produto/
    │   │   ├── CadastroProdutoApplication.java   # Classe principal
    │   │   ├── controller/ProdutoController.java # Rotas e validação
    │   │   ├── service/ProdutoService.java       # Regras de negócio
    │   │   ├── repository/ProdutoRepository.java # Acesso a dados (JPA)
    │   │   └── model/Produto.java                # Entidade
    │   └── resources/
    │       ├── application.properties
    │       ├── static/css/medieval.css           # Tema medieval
    │       └── templates/
    │           ├── cadastrar.html                # Formulário
    │           └── lista.html                    # Inventário
    └── test/java/.../CadastroProdutoApplicationTests.java
```

A aplicação segue a arquitetura em camadas **Controller → Service → Repository → Model**.

## Autor

**Rihan Amorim** — [GitHub](https://github.com/Rihan17) · [LinkedIn](https://linkedin.com/in/rihan-amorim)
