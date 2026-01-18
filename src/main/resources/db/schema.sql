CREATE TYPE user_role AS ENUM ('ADMIN', 'MANAGER', 'EMPLOYEE');

CREATE TYPE stock_operation_type AS ENUM ('RECEIPT', 'SALE', 'RETURN', 'ADJUSTMENT', 'WRITE_OFF');

CREATE TYPE user_status AS ENUM ('ACTIVE', 'BLOCKED', 'DELETED');

CREATE TABLE categories (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    description     TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE units (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(50) NOT NULL UNIQUE,
    short_name      VARCHAR(10) NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id              SERIAL PRIMARY KEY,
    login           VARCHAR(50) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            user_role NOT NULL,
    status          user_status NOT NULL DEFAULT 'ACTIVE',
    full_name       VARCHAR(150) NOT NULL,
    email           VARCHAR(100),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login      TIMESTAMP
);

CREATE INDEX idx_users_login ON users(login);
CREATE INDEX idx_users_status ON users(status);

CREATE TABLE products (
    id              SERIAL PRIMARY KEY,
    sku             VARCHAR(50) NOT NULL UNIQUE,       
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    category_id     INTEGER REFERENCES categories(id) ON DELETE SET NULL,
    unit_id         INTEGER REFERENCES units(id) ON DELETE SET NULL,
    purchase_price  DECIMAL(12, 2) NOT NULL DEFAULT 0,
    selling_price   DECIMAL(12, 2) NOT NULL,           
    min_stock_level INTEGER DEFAULT 0,                  
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_active ON products(is_active);

CREATE TABLE inventory (
    id              SERIAL PRIMARY KEY,
    product_id      INTEGER NOT NULL UNIQUE REFERENCES products(id) ON DELETE CASCADE,
    quantity        INTEGER NOT NULL DEFAULT 0,
    reserved        INTEGER NOT NULL DEFAULT 0,        
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT chk_reserved_non_negative CHECK (reserved >= 0),
    CONSTRAINT chk_available CHECK (quantity >= reserved)
);

CREATE INDEX idx_inventory_product ON inventory(product_id);

CREATE TABLE sales (
    id              SERIAL PRIMARY KEY,
    sale_number     VARCHAR(20) NOT NULL UNIQUE,       
    employee_id     INTEGER NOT NULL REFERENCES users(id),
    sale_date       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount    DECIMAL(12, 2) NOT NULL DEFAULT 0,
    discount        DECIMAL(12, 2) DEFAULT 0,
    final_amount    DECIMAL(12, 2) NOT NULL DEFAULT 0,
    is_returned     BOOLEAN DEFAULT FALSE,             
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sales_date ON sales(sale_date);
CREATE INDEX idx_sales_employee ON sales(employee_id);
CREATE INDEX idx_sales_number ON sales(sale_number);

CREATE TABLE sale_items (
    id              SERIAL PRIMARY KEY,
    sale_id         INTEGER NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    product_id      INTEGER NOT NULL REFERENCES products(id),
    quantity        INTEGER NOT NULL,
    price_at_sale   DECIMAL(12, 2) NOT NULL,           
    line_total      DECIMAL(12, 2) NOT NULL,
    returned_qty    INTEGER DEFAULT 0,                 
    
    CONSTRAINT chk_sale_item_qty_positive CHECK (quantity > 0),
    CONSTRAINT chk_returned_qty CHECK (returned_qty >= 0 AND returned_qty <= quantity)
);

CREATE INDEX idx_sale_items_sale ON sale_items(sale_id);
CREATE INDEX idx_sale_items_product ON sale_items(product_id);

CREATE TABLE receipts (
    id              SERIAL PRIMARY KEY,
    receipt_number  VARCHAR(20) NOT NULL UNIQUE,
    supplier_info   VARCHAR(200),                      
    manager_id      INTEGER NOT NULL REFERENCES users(id),
    receipt_date    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount    DECIMAL(12, 2) DEFAULT 0,
    notes           TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_receipts_date ON receipts(receipt_date);

CREATE TABLE receipt_items (
    id              SERIAL PRIMARY KEY,
    receipt_id      INTEGER NOT NULL REFERENCES receipts(id) ON DELETE CASCADE,
    product_id      INTEGER NOT NULL REFERENCES products(id),
    quantity        INTEGER NOT NULL,
    purchase_price  DECIMAL(12, 2) NOT NULL,
    line_total      DECIMAL(12, 2) NOT NULL,
    
    CONSTRAINT chk_receipt_item_qty_positive CHECK (quantity > 0)
);

CREATE INDEX idx_receipt_items_receipt ON receipt_items(receipt_id);

CREATE TABLE stock_log (
    id              SERIAL PRIMARY KEY,
    product_id      INTEGER NOT NULL REFERENCES products(id),
    operation_type  stock_operation_type NOT NULL,
    quantity_change INTEGER NOT NULL,                  
    quantity_before INTEGER NOT NULL,                  
    quantity_after  INTEGER NOT NULL,                 
    reference_id    INTEGER,                           
    reference_type  VARCHAR(50),                       
    user_id         INTEGER NOT NULL REFERENCES users(id),
    notes           TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stock_log_product ON stock_log(product_id);
CREATE INDEX idx_stock_log_date ON stock_log(created_at);
CREATE INDEX idx_stock_log_type ON stock_log(operation_type);
CREATE INDEX idx_stock_log_user ON stock_log(user_id);

CREATE TABLE security_log (
    id              SERIAL PRIMARY KEY,
    user_id         INTEGER REFERENCES users(id),
    action          VARCHAR(100) NOT NULL,
    details         TEXT,
    ip_address      VARCHAR(45),
    success         BOOLEAN NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_security_log_date ON security_log(created_at);
CREATE INDEX idx_security_log_user ON security_log(user_id);
CREATE INDEX idx_security_log_action ON security_log(action);

CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_inventory_updated_at
    BEFORE UPDATE ON inventory
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_categories_updated_at
    BEFORE UPDATE ON categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE OR REPLACE FUNCTION generate_sale_number()
RETURNS VARCHAR(20) AS $$
DECLARE
    new_number VARCHAR(20);
BEGIN
    new_number := 'S' || TO_CHAR(CURRENT_DATE, 'YYMMDD') || '-' || 
                  LPAD(NEXTVAL('sale_number_seq')::TEXT, 5, '0');
    RETURN new_number;
END;
$$ LANGUAGE plpgsql;

CREATE SEQUENCE IF NOT EXISTS sale_number_seq START WITH 1;

CREATE OR REPLACE FUNCTION generate_receipt_number()
RETURNS VARCHAR(20) AS $$
DECLARE
    new_number VARCHAR(20);
BEGIN
    new_number := 'R' || TO_CHAR(CURRENT_DATE, 'YYMMDD') || '-' || 
                  LPAD(NEXTVAL('receipt_number_seq')::TEXT, 5, '0');
    RETURN new_number;
END;
$$ LANGUAGE plpgsql;

CREATE SEQUENCE IF NOT EXISTS receipt_number_seq START WITH 1;

INSERT INTO units (name, short_name) VALUES
    ('Штука', 'шт'),
    ('Килограмм', 'кг'),
    ('Литр', 'л'),
    ('Метр', 'м'),
    ('Упаковка', 'уп');

INSERT INTO categories (name, description) VALUES
    ('Электроника', 'Электронные устройства и гаджеты'),
    ('Продукты питания', 'Продовольственные товары'),
    ('Одежда', 'Одежда и аксессуары'),
    ('Бытовая химия', 'Моющие и чистящие средства'),
    ('Канцелярия', 'Канцелярские товары');

INSERT INTO users (login, password_hash, role, status, full_name, email) VALUES
    ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjRGYWG8XMm8J9g2YNqy8EKJZQhKLy', 'ADMIN', 'ACTIVE', 'Администратор Системы', 'admin@retail.local');

CREATE OR REPLACE VIEW v_products_with_stock AS
SELECT 
    p.id,
    p.sku,
    p.name,
    p.selling_price,
    p.purchase_price,
    c.name AS category_name,
    u.short_name AS unit_name,
    COALESCE(i.quantity, 0) AS stock_quantity,
    p.min_stock_level,
    CASE WHEN COALESCE(i.quantity, 0) <= p.min_stock_level THEN TRUE ELSE FALSE END AS is_low_stock,
    p.is_active
FROM products p
LEFT JOIN categories c ON p.category_id = c.id
LEFT JOIN units u ON p.unit_id = u.id
LEFT JOIN inventory i ON p.id = i.product_id;

CREATE OR REPLACE VIEW v_sales_summary AS
SELECT 
    s.id,
    s.sale_number,
    s.sale_date,
    u.full_name AS employee_name,
    s.total_amount,
    s.discount,
    s.final_amount,
    s.is_returned
FROM sales s
JOIN users u ON s.employee_id = u.id;

CREATE OR REPLACE VIEW v_top_selling_products AS
SELECT 
    p.id,
    p.sku,
    p.name,
    SUM(si.quantity - si.returned_qty) AS total_sold,
    SUM(si.line_total) AS total_revenue
FROM products p
JOIN sale_items si ON p.id = si.product_id
JOIN sales s ON si.sale_id = s.id
WHERE s.is_returned = FALSE
GROUP BY p.id, p.sku, p.name
ORDER BY total_sold DESC;

COMMENT ON TABLE users IS 'Пользователи системы (сотрудники, менеджеры, администраторы)';
COMMENT ON TABLE products IS 'Справочник товаров (номенклатура)';
COMMENT ON TABLE inventory IS 'Складские остатки товаров';
COMMENT ON TABLE sales IS 'Заголовки чеков (продажи)';
COMMENT ON TABLE sale_items IS 'Позиции чеков';
COMMENT ON TABLE receipts IS 'Документы поступления товаров';
COMMENT ON TABLE stock_log IS 'История всех движений товаров на складе';
COMMENT ON TABLE security_log IS 'Журнал безопасности и аудита действий';
COMMENT ON CONSTRAINT chk_quantity_non_negative ON inventory IS 'Запрет отрицательного остатка на складе';
