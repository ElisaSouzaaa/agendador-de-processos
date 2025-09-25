
---

# 📂 Sistema de Agendamento e Processamento de Arquivos

Este projeto é um sistema full-stack projetado para agendar, executar e monitorar o processamento de arquivos de texto, simulando um ambiente de retorno bancário.
A aplicação permite a criação de tarefas recorrentes (Jobs) baseadas em expressões CRON, que vigiam um diretório em busca de novos arquivos para processar.

O sistema conta com uma **API REST** para gerenciar os agendamentos e uma **interface em Angular** que permite o monitoramento em tempo real do status de cada tarefa, graças ao uso de **Server-Sent Events (SSE)**.

O seu sistema de agendamento é projetado para processar arquivos de texto que seguem o formato posicional, onde cada linha possui um tipo de registro diferente, identificado por um código de saída.

---

## ⚙️ Principais Funcionalidades

* **Gerenciamento de Jobs**: Crie, liste, edite e exclua agendamentos de tarefas.
* **Agendamento com CRON**: Defina a frequência de execução das tarefas usando expressões CRON.
* **Processamento de Arquivos**: O backend lê arquivos de texto de um diretório, processa seu conteúdo (cabeçalho e transações) e salva os resultados no banco de dados.

---

### 📝 Registro de Cabeçalho (Mestre)

* **Identificação**: A primeira linha do arquivo.
* **Finalidade**: É o registro "Mestre" que contém informações gerais sobre o lote de transações, como a data de geração, código da empresa e dados numéricos ou textuais globais (campos cabecalhoNumerico, cabecalhoTexto e cabecalhoCodigo na entidade ArquivoRetorno).
* **O que o sistema faz**: O backend salva essa linha como o registro principal (**ArquivoRetorno**) no banco de dados.

---

### 📑 Registros de Detalhe (Transações)

* **Identificação**: Linhas seguintes no arquivo.
* **Finalidade**: Cada linha representa uma transação individual, como pagamento, cobrança ou estorno. São os registros "Detalhe".
* **O que o sistema faz**: O backend lê cada linha e cria uma nova entidade **Transacao** com status `PENDENTE`, vinculando-a ao **ArquivoRetorno** criado a partir do cabeçalho.

---

### 🔢 Códigos de Saída (Status e Tipo de Transação)

Cada linha de transação possui um Código de Saída que determina o tipo e status da transação:

* **Código 000**: Transação processada e concluída com sucesso (**PROCESSADO**).
* **Código 001**: Transação com falha, requer atenção (**FALHA**).
* **Códigos mistos 000 e 001**: Interpretados como **CONCLUÍDOS COM ERROS**, indicando que o processamento foi parcial, com algumas falhas.

---

### 📡 Monitoramento em Tempo Real

Acompanhe as mudanças de status dos Jobs em tempo real na interface do usuário, sem a necessidade de recarregar a página.

---

## 🛠️ Tecnologias Utilizadas

### 🔙 Backend

* Java 21
* Spring Boot 3.x
* Spring Data JPA
* Quartz Scheduler
* Microsoft SQL Server
* Maven

### 🎨 Frontend

* Angular 17+
* Node.js & NPM

---

## 📋 Pré-requisitos

Antes de começar, garanta que você tenha as seguintes ferramentas instaladas em sua máquina:

* Git
* JDK 21 ou superior
* Maven 3.8+
* Microsoft SQL Server
* Node.js e NPM
* Angular CLI (`npm install -g @angular/cli`)
* Docker em caso de SO Linux/macOS

---

## 🚀 Como Executar o Projeto

Siga os passos abaixo para configurar e rodar a aplicação em seu ambiente local.

---

### 1️⃣ Clonar o Repositório

```bash
git clone <URL_DO_REPOSITORIO>
```

---

### 2️⃣ Configurar o Backend

#### a. Banco de Dados

* Abra seu Microsoft SQL Server, ou em caso de usuário linux/macOS usar o Docker para baixar uma imagem do SQL Server.
* Instalar Docker, rodar container com SQL Server, verificar se o container está rodando e  se não estiver, iniciar.


```bash
sudo snap install docker

docker run -e "ACCEPT_EULA=Y" -e "SA_PASSWORD=SuaSenhaForte123!" \
   -p 1433:1433 --name sqlserver -d mcr.microsoft.com/mssql/server:2022-latest

sudo docker ps

sudo docker start sqlserver
```

* No gerenciador de banco de dados (ex: **Azure Data Studio**), crie um novo banco de dados. Ex: `agendador_db`.
* As tabelas (**JOB, ARQUIVO_RETORNO, TRANSACAO**) serão criadas automaticamente pelo Hibernate na primeira vez que a aplicação iniciar.

#### b. Arquivo `application.properties`

* Abra a pasta do projeto na sua IDE de preferência
* Abra o arquivo `src/main/resources/application-dev.properties`.
* Adapte as configurações do banco de dados para a sua máquina (usuário, senha e nome do banco).
* Em seguida, no `src/main/resources/application.properties`, mude de **prod** para **dev**.
* Não esqueça de alterar o caminho do diretório para onde as pastas de arquivos serão criadas na sua máquina:

```bash
# Caminho base para o armazenamento dos arquivos de retorno
# Altere <NOME-DA-SUA-PASTA> para um nome de sua escolha, ex: 'Projetos'
diretorio.arquivos.base=${user.home}/<NOME-DA-SUA-PASTA>/RetornoArquivo
```


Exemplo `application-dev.properties`:

```properties
# Configuração do SQL Server
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=agendador_db;encrypt=true;trustServerCertificate=true;
spring.datasource.username=<SEU_USUARIO_SQL_SERVER>
spring.datasource.password=<SUA_SENHA_SQL_SERVER>
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver

# Configuração do Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServerDialect
```

---

### 3️⃣ Executar o Backend

Execute o arquivo AgendadorApplication.java. 
* O servidor do backend iniciará na porta `8080`.

* (Opcional) Visualizando o Banco de Dados
Para ver as tabelas (JOB, ARQUIVO_RETORNO, QRTZ_...) sendo criadas e populadas pela aplicação, você pode usar uma ferramenta de gerenciamento de banco de dados.
Ferramenta Recomendada: Azure Data Studio (gratuito e multiplataforma).
Como conectar:
Server: localhost
Authentication type: SQL Login
User name: sa
Password: SuaSenhaForte123!
Database: AGENDADOR_DB

Importante: A aplicação criará automaticamente a seguinte estrutura de diretórios na pasta escolhida do usuário para gerenciar os arquivos:

- `~/<PASTA-DEFINIDA-NO-PROPERTIES>/RetornoArquivo/`
  - **uploads/**: Onde o arquivo original chega.
  - **pendentes/**: Para onde uma cópia é feita para aguardar o processamento.
  - **processados/**: Destino final se todas as transações do arquivo tiverem sucesso.
  - **erros/**: Destino final se todas as transações falharem ou se ocorrer um erro crítico.
  - **com_erros/**: Destino final se houver uma mistura de transações com sucesso e com erro.


---

### 4️⃣ Configurar o Frontend

*Abra sua IDE de preferência
*No terminal, navegue até a pasta do frontend:

```bash
cd frontend/
```

* Instale as dependências:

```bash
npm install
```
*Abra os arquivos src/app/job-service.ts e src/app/arquivo-retorno-service.ts e garanta que a apiUrl está apontando para o backend:

```ts
apiUrl: 'http://localhost:8080/'
```
---

### 5️⃣ Executar o Frontend

Depois inicie o servidor do Angular:

```bash
ng serve
```

Abra no navegador: **[http://localhost:4200/](http://localhost:4200/)**

> ⚠️ O backend precisa estar rodando para que o frontend funcione corretamente!

---

## 🧪 Testando a API com o Postman

Você pode usar o **Postman** ou ferramenta similar para testar os endpoints.

---

### ➕ 1. Criando um Novo Job

* **Método**: POST
* **URL**: `http://localhost:8080/jobs`
* **Body**: raw > JSON

```json
{
    "nome": "Processamento Diário Teste",
    "cronExpression": "0 */1 * * * ?"
}
```

Resultado: Criará um Job que rodará a cada minuto. Status inicial: **AGENDADO**.

---

### 📤 2. Fazendo Upload de um Arquivo

* **Método**: POST
* **URL**: `http://localhost:8080/arquivos/upload/1` (substitua `1` pelo ID do Job criado).
* **Body**: form-data

  * Crie uma chave (**KEY**) chamada `file`.
  * Mude o tipo de Text para **File**.
  * Selecione o arquivo de texto que deseja enviar.

**Resultado**:
O status do Job será atualizado para **PROCESSANDO**.
Na próxima execução do CRON, o arquivo será processado.

---

### 📡 3. Monitorando em Tempo Real com SSE

* **Método**: Interface do front
*  **URL Visualização: `http://localhost:4200/`
* **URL de conexão**: `http://localhost:8080/api/sse/subscribe`

**Comportamento**:
No frontend, você verá as atualizações de status dos Jobs chegando em tempo real à medida que as ações ocorrem.
