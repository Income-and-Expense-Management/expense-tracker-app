generator client {
  provider = "prisma-client-js"
}

datasource db {
  provider = "sqlserver"
  url      = env("DATABASE_URL")
}

model User {
  id            String     @id(map: "PK__users__3213E83F99905A3D") @default(uuid()) @db.VarChar(36)
  full_name     String?    @db.NVarChar(255)
  email         String?    @unique(map: "UQ_users_email") @db.VarChar(255)
  avatar_url    String?    @db.NVarChar(Max)
  auth_provider String?    @db.VarChar(50)
  password      String?    @db.VarChar(255)
  created_at    DateTime   @default(dbgenerated("getutcdate()"), map: "DF_users_created_at")
  updated_at    DateTime   @default(dbgenerated("getutcdate()"), map: "DF_users_updated_at")

  wallets    Wallet[]
  categories Category[]

  @@map("users")
}

model Wallet {
  id              String    @id(map: "PK__wallets__3213E83F33DE05FA") @default(uuid()) @db.VarChar(36)
  user_id         String    @db.VarChar(36)
  name            String    @db.NVarChar(255)
  initial_balance BigInt?   @default(0, map: "DF_wallets_initial_balance")
  currency        String?   @default("VND", map: "DF_wallets_currency") @db.VarChar(10)
  icon_id         String?   @db.VarChar(255)
  created_at      DateTime  @default(dbgenerated("getutcdate()"), map: "DF_wallets_created_at")
  updated_at      DateTime  @default(dbgenerated("getutcdate()"), map: "DF_wallets_updated_at")
  deleted_at      DateTime?

  user         User          @relation(fields: [user_id], references: [id], onDelete: NoAction, onUpdate: NoAction)
  transactions Transaction[]
  budgets      Budget[]

  @@index([user_id], map: "idx_wallets_user")
  @@map("wallets")
}

model Category {
  id         String    @id(map: "PK__categori__3213E83FAB90E253") @default(uuid()) @db.VarChar(36)
  user_id    String?   @db.VarChar(36)
  name       String    @db.NVarChar(255)
  type       String    @db.VarChar(20)
  icon_name  String?   @db.VarChar(255)
  is_active  Boolean   @default(true, map: "DF_categories_is_active")
  created_at DateTime  @default(dbgenerated("getutcdate()"), map: "DF_categories_created_at")
  updated_at DateTime  @default(dbgenerated("getutcdate()"), map: "DF_categories_updated_at")
  deleted_at DateTime?

  user         User?         @relation(fields: [user_id], references: [id], onDelete: NoAction, onUpdate: NoAction)
  transactions Transaction[]
  budgets      Budget[]

  @@map("categories")
}

model Transaction {
  id               String    @id(map: "PK__transact__3213E83FEF37376C") @default(uuid()) @db.VarChar(36)
  wallet_id        String    @db.VarChar(36)
  category_id      String?   @db.VarChar(36)
  amount           BigInt
  transaction_date DateTime
  note             String?   @db.NVarChar(Max)
  created_at       DateTime  @default(dbgenerated("getutcdate()"), map: "DF_transactions_created_at")
  updated_at       DateTime  @default(dbgenerated("getutcdate()"), map: "DF_transactions_updated_at")
  deleted_at       DateTime?

  wallet   Wallet    @relation(fields: [wallet_id], references: [id], onDelete: NoAction, onUpdate: NoAction)
  category Category? @relation(fields: [category_id], references: [id], onDelete: NoAction, onUpdate: NoAction)

  @@index([category_id], map: "idx_transactions_category")
  @@index([transaction_date], map: "idx_transactions_date")
  @@index([wallet_id], map: "idx_transactions_wallet")
  @@index([updated_at], map: "idx_transactions_sync")
  @@map("transactions")
}

model Budget {
  id            String    @id(map: "PK__budgets__3213E83FD8E72D51") @default(uuid()) @db.VarChar(36)
  wallet_id     String    @db.VarChar(36)
  category_id   String    @db.VarChar(36)
  target_amount BigInt
  start_date    DateTime? @db.Date
  end_date      DateTime? @db.Date
  created_at    DateTime  @default(dbgenerated("getutcdate()"), map: "DF_budgets_created_at")
  updated_at    DateTime  @default(dbgenerated("getutcdate()"), map: "DF_budgets_updated_at")
  deleted_at    DateTime?

  wallet   Wallet   @relation(fields: [wallet_id], references: [id], onDelete: NoAction, onUpdate: NoAction)
  category Category @relation(fields: [category_id], references: [id], onDelete: NoAction, onUpdate: NoAction)

  @@index([wallet_id], map: "idx_budgets_wallet")
  @@map("budgets")
}
