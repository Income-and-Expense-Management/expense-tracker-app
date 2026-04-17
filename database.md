-- Chuyển đổi sang chuẩn Offline-First Synchronization
-- Sử dụng GETUTCDATE() thay vì GETDATE() để chuẩn hóa múi giờ toàn cầu

DROP TABLE IF EXISTS budgets;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS wallets;
DROP TABLE IF EXISTS users;

-- 1. BẢNG USERS
CREATE TABLE users(
id varchar(36) NOT NULL,
full_name nvarchar(255),
email varchar(255),
avatar_url nvarchar(max),
auth_provider varchar(50),
password varchar(255),
created_at datetime2 NOT NULL CONSTRAINT DF_users_created_at DEFAULT (GETUTCDATE()),
updated_at datetime2 NOT NULL CONSTRAINT DF_users_updated_at DEFAULT (GETUTCDATE()),
PRIMARY KEY(id),
CONSTRAINT UQ_users_email UNIQUE (email)
);

-- 2. BẢNG WALLETS
CREATE TABLE wallets(
id varchar(36) NOT NULL,
user_id varchar(36) NOT NULL,
name nvarchar(255) NOT NULL,
initial_balance bigint CONSTRAINT DF_wallets_initial_balance DEFAULT ((0)),
currency varchar(10) CONSTRAINT DF_wallets_currency DEFAULT 'VND',
icon_id varchar(255), -- Đổi từ icon_id sang icon_name
created_at datetime2 NOT NULL CONSTRAINT DF_wallets_created_at DEFAULT (GETUTCDATE()),
updated_at datetime2 NOT NULL CONSTRAINT DF_wallets_updated_at DEFAULT (GETUTCDATE()),
deleted_at datetime2 NULL, -- Thay thế cho is_deteled bit
PRIMARY KEY(id)
);
CREATE INDEX idx_wallets_user ON wallets("user_id");

-- 3. BẢNG CATEGORIES
CREATE TABLE categories(
id varchar(36) NOT NULL,
user_id varchar(36) NULL, -- Nếu NULL thì là danh mục mặc định của hệ thống
name nvarchar(255) NOT NULL,
"type" varchar(20) NOT NULL,
icon_name varchar(255),
is_active bit NOT NULL CONSTRAINT DF_categories_is_active DEFAULT ((1)), -- 1: Đang hiện, 0: Đã ẩn
created_at datetime2 NOT NULL CONSTRAINT DF_categories_created_at DEFAULT (GETUTCDATE()),
updated_at datetime2 NOT NULL CONSTRAINT DF_categories_updated_at DEFAULT (GETUTCDATE()),
deleted_at datetime2 NULL, -- Ngày xóa hoàn toàn
PRIMARY KEY(id),
CONSTRAINT CK_categories_type CHECK ([type]='EXPENSE' OR [type]='INCOME')
);

-- 4. BẢNG TRANSACTIONS
CREATE TABLE transactions(
id varchar(36) NOT NULL,
wallet_id varchar(36) NOT NULL,
category_id varchar(36) NOT NULL,
amount bigint NOT NULL,
transaction_date datetime2 NOT NULL,
note nvarchar(max),
-- Đã loại bỏ trường "type" và "icon_id" bị dư thừa
created_at datetime2 NOT NULL CONSTRAINT DF_transactions_created_at DEFAULT (GETUTCDATE()),
updated_at datetime2 NOT NULL CONSTRAINT DF_transactions_updated_at DEFAULT (GETUTCDATE()),
deleted_at datetime2 NULL,
PRIMARY KEY(id)
);
CREATE INDEX idx_transactions_wallet ON transactions("wallet_id");
CREATE INDEX idx_transactions_category ON transactions("category_id");
CREATE INDEX idx_transactions_date ON transactions("transaction_date");
CREATE INDEX idx_transactions_sync ON transactions("updated_at"); -- Index cực kỳ quan trọng cho Sync

-- 5. BẢNG BUDGETS
CREATE TABLE budgets(
id varchar(36) NOT NULL,
wallet_id varchar(36) NOT NULL,
category_id varchar(36) NOT NULL,
target_amount bigint NOT NULL,
start_date date,
end_date date,
created_at datetime2 NOT NULL CONSTRAINT DF_budgets_created_at DEFAULT (GETUTCDATE()),
updated_at datetime2 NOT NULL CONSTRAINT DF_budgets_updated_at DEFAULT (GETUTCDATE()),
deleted_at datetime2 NULL,
PRIMARY KEY(id)
);
CREATE INDEX idx_budgets_wallet ON budgets("wallet_id");