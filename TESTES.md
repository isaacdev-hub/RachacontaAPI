### Cobertura (JaCoCo)
Try:
|
Pacote	Instruções	Branches	Linhas cobertas
application.service	45%	55%	84 de 208
infrastructure.security	47%	12%	30 de 64
web.controller	9%	n/a	1 de 14
web.exception	78%	100%	18 de 24
domain.enums	100%	n/a	9 de 9
domain.model	100%	n/a	1 de 1
Total	48%	52%	143 de 320 (≈45%)
Análise: os testes focam nas regras de negócio mais críticas (cálculo de liquidação e cadastro de usuário). As principais lacunas são ExpenseService, GroupService, o endpoint de login e os controllers de grupo, despesa e liquidação, que ainda não possuem testes.


Tirei as linhas cobertas das colunas Missed do seu print (total menos o que não rodou). Se o professor pedir por classe, clique no pacote no relatório para ver os números de cada uma.

### Para aumentar a cobertura (se quiser)

Os testes que mais sobem a porcentagem, na ordem:

1. **Login no `AuthControllerTest`**: login com senha correta (200 + token) e com senha errada. Cobre o login e o caminho do JWT.
2. **Endpoint protegido com token válido**: registrar, pegar o token e chamar `POST /api/groups`. Sobe bem os branches de `security`.
3. **Testes unitários de `GroupService` e `ExpenseService`**: é o maior bloco de código ainda sem teste.
