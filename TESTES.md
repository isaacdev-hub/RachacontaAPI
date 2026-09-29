# Testes Automatizados — RachaConta API

Documentação dos testes automatizados da **RachaConta API**, uma API REST para divisão de despesas entre grupos de amigos.

- Repositório: https://github.com/isaacdev-hub/RachacontaAPI
- Total de testes: **32** (todos passando)
- Cobertura de instruções: **86%** | Cobertura de branches: **84%**

---

## 1. Tecnologias utilizadas
Try:
|
Ferramenta	Uso
JUnit 5	Framework de testes
Mockito	Simulação (mock) de dependências nos testes unitários
AssertJ	Asserções mais legíveis
Spring Boot Test + MockMvc	Testes de integração com requisições HTTP simuladas
Spring Security Test	Suporte a testes com autenticação
H2 Database	Banco em memória usado apenas nos testes
JaCoCo 0.8.12	Relatório de cobertura de código
2. Como executar
Pré-requisito: Java 21. Docker e PostgreSQL não são necessários, pois os testes usam o H2 em memória.

mvnw.cmd clean test          (Windows)
./mvnw clean test            (Linux/Mac)
Relatório de cobertura: target/site/jacoco/index.html

Atenção: o Maven precisa rodar com o JDK 21. Com o JDK 25, a versão do Lombok usada no projeto não processa as anotações e a compilação falha com erros de cannot find symbol. Confira com mvnw.cmd -v.

Configuração do ambiente de teste
O arquivo src/test/resources/application.properties substitui o PostgreSQL pelo H2:

spring.flyway.enabled=false
spring.jpa.hibernate.ddl-auto=create-drop
spring.datasource.url=jdbc:h2:mem:testdb;NON_KEYWORDS=GROUPS
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
jwt.secret=test-secret-key-minimo-32-caracteres-teste
jwt.expiration-ms=86400000
O Flyway é desativado e o Hibernate cria as tabelas a cada execução.
NON_KEYWORDS=GROUPS permite criar a tabela groups, porque GROUPS é palavra reservada no H2.
3. Estratégia de testes
Testes unitários
Testam a regra de negócio dos services de forma isolada. Os repositórios e demais dependências são substituídos por mocks do Mockito (@Mock / @InjectMocks), então nenhum banco é acessado. São rápidos e apontam exatamente onde a regra falhou.

Testes de integração
Sobem o contexto completo do Spring Boot (@SpringBootTest) com banco H2 e fazem requisições HTTP pelo MockMvc. Validam em conjunto controllers, validação de dados (@Valid), tratamento de exceções (GlobalExceptionHandler), persistência e segurança com JWT.

Técnicas aplicadas
Padrão AAA (Arrange, Act, Assert) em todos os testes.
Caminho feliz e caminhos de erro: cada regra é testada no cenário válido e nos cenários de exceção.
Análise de valor limite / arredondamento: divisão de valores que não dividem exatamente.
ArgumentCaptor: verifica o objeto que foi de fato salvo no repositório (ex.: papel ADMIN / MEMBER).
Verificação de efeitos colaterais: verify(..., never()) garante que nada é salvo quando a regra falha.
Dados isolados: os testes de integração geram emails únicos (UUID) para não interferir uns nos outros.
4. Estrutura
src/test/java/com/dev/rachacontaapi/
├── RachacontaapiApplicationTests.java
├── application/service/
│   ├── AuthServiceTest.java
│   ├── ExpenseServiceTest.java
│   ├── GroupServiceTest.java
│   └── SettlementServiceTest.java
└── web/controller/
    ├── AuthControllerTest.java
    └── GroupControllerTest.java
Resumo
Classe de teste	Tipo	Testes
SettlementServiceTest	Unitário	5
AuthServiceTest	Unitário	2
GroupServiceTest	Unitário	7
ExpenseServiceTest	Unitário	6
AuthControllerTest	Integração	6
GroupControllerTest	Integração	5
RachacontaapiApplicationTests	Contexto	1
Total		32
5. Casos de teste
5.1 SettlementServiceTest (unitário), 5 testes
#	Cenário	Resultado esperado
1	Ana paga R$ 90 dividido entre 3 pessoas	Gera 2 liquidações (Bruno e Carla pagam R$ 30 cada à Ana)
2	Grupo sem despesas	Nenhuma liquidação gerada
3	Grupo inexistente	BusinessException "Grupo não encontrado"
4	Usuário que não é o recebedor tenta confirmar	Lança exceção e não salva
5	Recebedor confirma a liquidação	Status muda para CONFIRMED
5.2 AuthServiceTest (unitário), 2 testes
#	Cenário	Resultado esperado
1	Cadastro com email já existente	BusinessException "Email já cadastrado"
2	Cadastro válido	Usuário salvo com senha criptografada (hash)
5.3 GroupServiceTest (unitário), 7 testes
#	Cenário	Resultado esperado
1	Criar grupo	Criador entra automaticamente como ADMIN
2	Listar meus grupos	Retorna só os grupos do usuário logado
3	Buscar grupo inexistente	BusinessException "Grupo não encontrado"
4	Usuário fora do grupo adiciona membro	BusinessException "Usuário não pertence ao grupo"
5	Membro comum adiciona membro	BusinessException "Apenas administradores podem adicionar membros", nada é salvo
6	Adicionar quem já é membro	BusinessException "Usuário já é membro do grupo"
7	Admin adiciona membro	Novo membro salvo com papel MEMBER
5.4 ExpenseServiceTest (unitário), 6 testes
#	Cenário	Resultado esperado
1	EQUAL: R$ 90 entre 3 membros	3 divisões de R$ 30,00
2	EQUAL: R$ 100 entre 3 membros	Cada divisão arredondada para R$ 33,33 (HALF_UP)
3	CUSTOM sem divisões	BusinessException "Splits customizados são obrigatórios para CUSTOM"
4	CUSTOM com soma diferente do total	BusinessException "A soma das divisões não bate com o valor total da despesa", nada é salvo
5	CUSTOM válido	Uma divisão salva por usuário
6	Despesa em grupo inexistente	BusinessException "Grupo não encontrado", despesa não é salva
5.5 AuthControllerTest (integração), 6 testes
#	Requisição	Cenário	Resultado esperado
1	POST /api/auth/register	Registro válido	201 Created
2	POST /api/auth/register	Email inválido	400 + details.email = "Email inválido"
3	POST /api/auth/register	Email duplicado	400 + "Email já cadastrado"
4	POST /api/groups	Endpoint protegido sem token	401 ou 403
5	POST /api/auth/login	Login com senha correta	200 + token JWT
6	POST /api/auth/login	Login com senha errada	401 ou 403
5.6 GroupControllerTest (integração com JWT), 5 testes
Cada teste registra um usuário novo, obtém o token JWT e o envia no cabeçalho Authorization: Bearer <token>.

#	Requisição	Cenário	Resultado esperado
1	POST /api/groups	Criar grupo com token válido	201 + id e name
2	POST /api/groups	Criar grupo sem nome	400 + "Nome do grupo é obrigatório"
3	GET /api/groups	Listar meus grupos	200 + 2 grupos
4	GET /api/groups/{id}	Buscar grupo por id	200 + nome correto
5	GET /api/groups/{id}	Buscar grupo inexistente	400 + "Grupo não encontrado"
5.7 RachacontaapiApplicationTests, 1 teste
#	Cenário	Resultado esperado
1	Carregar o contexto do Spring	A aplicação sobe sem erros
6. Resultado da execução
Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
7. Cobertura de código (JaCoCo)
Ficam fora da medição a classe principal (RachacontaapiApplication), os DTOs (application.dto) e as configurações (infrastructure.config), porque não contêm regra de negócio.

7.1 Cobertura por pacote
Pacote	Instruções	Branches	Linhas cobertas	Métodos cobertos
application.service	89%	88%	190 de 208	24 de 34
infrastructure.security	92%	62%	60 de 64	15 de 17
web.exception	78%	100%	18 de 24	5 de 8
web.controller	40%	n/a	5 de 14	5 de 12
domain.enums	100%	n/a	9 de 9	3 de 3
domain.model	100%	n/a	1 de 1	1 de 1
Total	86%	84%	283 de 320 (≈88%)	53 de 75 (≈71%)
Instruções: 1.155 de 1.333 cobertas. Branches: 39 de 46 cobertos. Classes: 17 de 20 executadas.

7.2 Evolução
Métrica	12 testes	32 testes
Instruções	48%	86%
Branches	52%	84%
Linhas cobertas	143 de 320	283 de 320
application.service	45%	89%
infrastructure.security (branches)	12%	62%
web.controller	9%	40%
7.3 Análise
application.service (89%): as 4 classes de regra de negócio (AuthService, GroupService, ExpenseService, SettlementService) têm testes, incluindo os caminhos de erro.
infrastructure.security (92% / 62% branches): os testes com token válido exercitam o filtro JWT. Os branches restantes correspondem a cenários como token inválido ou expirado.
web.controller (40%): AuthController e GroupController são testados. ExpenseController e SettlementController ainda não têm testes de integração, e são as 2 classes não executadas no relatório.
web.exception (78% / 100% branches): os handlers de validação e de regra de negócio são exercitados. Parte do restante corresponde a handlers ainda não acionados pelos testes.
8. Defeitos e observações encontrados pelos testes
8.1 Perda de centavos na divisão igualitária
O teste "EQUAL: R$ 100 entre 3 membros" mostra que o ExpenseService arredonda cada parte para R$ 33,33. A soma das divisões fica em R$ 99,99, e R$ 0,01 se perde em relação ao valor da despesa.

Sugestão de correção: atribuir a diferença do arredondamento a um dos participantes (ex.: o último fica com R$ 33,34).

8.2 Login com senha errada sem tratamento próprio
O GlobalExceptionHandler não trata BadCredentialsException. O status retornado (401 ou 403) depende do comportamento padrão do Spring Security, e o corpo da resposta não segue o mesmo formato de erro dos demais endpoints.

Sugestão de correção: adicionar um handler específico que retorne 401 com mensagem padronizada.

8.3 Status 400 para recurso inexistente
Buscar um grupo inexistente retorna 400 Bad Request ("Grupo não encontrado"), porque o serviço lança BusinessException. Pela semântica HTTP, o esperado seria 404 Not Found. O projeto já possui ResourceNotFoundException mapeada para 404, mas ela não é usada nesse caso.

9. Próximos passos
Testes de integração para ExpenseController e SettlementController.
Testes de token JWT inválido e expirado.
Teste de addMember e listMembers via HTTP.
Corrigir os defeitos da seção 8 e manter os testes como proteção contra regressão.

Tirei todos os números da seção 7 do seu print. As linhas e os métodos cobertos são o total menos a coluna **Missed**. Na seção 8, o 8.1 vem do teste que você rodou, e o 8.2 e o 8.3 vêm do código do repositório (`GlobalExceptionHandler` e `GroupService`).

Depois de salvar, envie para o GitHub:

```bat
git add TESTES.md
git commit -m "docs: documentação completa dos testes e cobertura JaCoCo"
git push
```

**Para o trabalho:** anexe também o print do relatório do JaCoCo. Com os 12 testes e depois com os 32, ele mostra bem a evolução da cobertura.
