-- Database Client 8.4.5
-- Host: 127.0.0.1 Port: 1433 Database: dbo Schema: dbo
-- Dump is still an early version, please use the dumped SQL with caution

DROP TABLE IF EXISTS budgets;
CREATE TABLE budgets(
    id varchar(36) NOT NULL,
    wallet_id varchar(36) NOT NULL,
    category_id varchar(36) NOT NULL,
    target_amount bigint NOT NULL,
    start_date date,
    end_date date,
    created_at datetime2 NOT NULL,
    updated_at datetime2 NOT NULL,
    deleted_at datetime2,
    PRIMARY KEY(id),
    CONSTRAINT budgets_wallet_id_fkey FOREIGN key(wallet_id) REFERENCES wallets(id),
    CONSTRAINT budgets_category_id_fkey FOREIGN key(category_id) REFERENCES categories(id)
);
CREATE INDEX idx_budgets_wallet ON budgets("wallet_id");
ALTER TABLE "dbo"."budgets" ADD CONSTRAINT DF_budgets_created_at DEFAULT '(getutcdate())' FOR created_at;
ALTER TABLE "dbo"."budgets" ADD CONSTRAINT DF_budgets_updated_at DEFAULT '(getutcdate())' FOR updated_at;

DROP TABLE IF EXISTS categories;
CREATE TABLE categories(
    id varchar(36) NOT NULL,
    user_id varchar(36),
    name nvarchar(255) NOT NULL,
    "type" varchar(20) NOT NULL,
    icon_name varchar(255),
    is_active bit NOT NULL,
    created_at datetime2 NOT NULL,
    updated_at datetime2 NOT NULL,
    deleted_at datetime2,
    PRIMARY KEY(id),
    CONSTRAINT categories_user_id_fkey FOREIGN key(user_id) REFERENCES users(id),
    CONSTRAINT CK_categories_type CHECK ([type]='EXPENSE' OR [type]='INCOME')
);
ALTER TABLE "dbo"."categories" ADD CONSTRAINT DF_categories_is_active DEFAULT '((1))' FOR is_active;
ALTER TABLE "dbo"."categories" ADD CONSTRAINT DF_categories_created_at DEFAULT '(getutcdate())' FOR created_at;
ALTER TABLE "dbo"."categories" ADD CONSTRAINT DF_categories_updated_at DEFAULT '(getutcdate())' FOR updated_at;

DROP TABLE IF EXISTS transactions;
CREATE TABLE transactions(
    id varchar(36) NOT NULL,
    wallet_id varchar(36) NOT NULL,
    category_id varchar(36),
    amount bigint NOT NULL,
    transaction_date datetime2 NOT NULL,
    note nvarchar(max),
    created_at datetime2 NOT NULL,
    updated_at datetime2 NOT NULL,
    deleted_at datetime2,
    PRIMARY KEY(id),
    CONSTRAINT transactions_wallet_id_fkey FOREIGN key(wallet_id) REFERENCES wallets(id),
    CONSTRAINT transactions_category_id_fkey FOREIGN key(category_id) REFERENCES categories(id)
);
CREATE INDEX idx_transactions_wallet ON transactions("wallet_id");
CREATE INDEX idx_transactions_category ON transactions("category_id");
CREATE INDEX idx_transactions_date ON transactions("transaction_date");
CREATE INDEX idx_transactions_sync ON transactions("updated_at");
ALTER TABLE "dbo"."transactions" ADD CONSTRAINT DF_transactions_created_at DEFAULT '(getutcdate())' FOR created_at;
ALTER TABLE "dbo"."transactions" ADD CONSTRAINT DF_transactions_updated_at DEFAULT '(getutcdate())' FOR updated_at;

DROP TABLE IF EXISTS users;
CREATE TABLE users(
    id varchar(36) NOT NULL,
    full_name nvarchar(255),
    email varchar(255),
    avatar_url nvarchar(max),
    auth_provider varchar(50),
    password varchar(255),
    created_at datetime2 NOT NULL,
    updated_at datetime2 NOT NULL,
    PRIMARY KEY(id)
);
CREATE UNIQUE INDEX UQ_users_email ON users("email");
ALTER TABLE "dbo"."users" ADD CONSTRAINT DF_users_created_at DEFAULT '(getutcdate())' FOR created_at;
ALTER TABLE "dbo"."users" ADD CONSTRAINT DF_users_updated_at DEFAULT '(getutcdate())' FOR updated_at;

DROP TABLE IF EXISTS wallets;
CREATE TABLE wallets(
    id varchar(36) NOT NULL,
    user_id varchar(36) NOT NULL,
    name nvarchar(255) NOT NULL,
    initial_balance bigint,
    currency varchar(10),
    icon_id varchar(255),
    created_at datetime2 NOT NULL,
    updated_at datetime2 NOT NULL,
    deleted_at datetime2,
    PRIMARY KEY(id),
    CONSTRAINT wallets_user_id_fkey FOREIGN key(user_id) REFERENCES users(id)
);
CREATE INDEX idx_wallets_user ON wallets("user_id");
ALTER TABLE "dbo"."wallets" ADD CONSTRAINT DF_wallets_initial_balance DEFAULT ((0)) FOR initial_balance;
ALTER TABLE "dbo"."wallets" ADD CONSTRAINT DF_wallets_currency DEFAULT 'VND' FOR currency;
ALTER TABLE "dbo"."wallets" ADD CONSTRAINT DF_wallets_created_at DEFAULT '(getutcdate())' FOR created_at;
ALTER TABLE "dbo"."wallets" ADD CONSTRAINT DF_wallets_updated_at DEFAULT '(getutcdate())' FOR updated_at;