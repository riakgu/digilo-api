-- =====================================================
-- DIGILO API - CLEAR DATA SCRIPT
-- =====================================================
-- Run this script to clear all data before re-seeding.
-- This preserves the schema but removes all rows.
-- =====================================================

-- Disable foreign key checks temporarily
SET session_replication_role = 'replica';

-- Clear tables in reverse order of dependencies
TRUNCATE TABLE notifications CASCADE;
TRUNCATE TABLE payments CASCADE;
TRUNCATE TABLE order_items CASCADE;
TRUNCATE TABLE orders CASCADE;
TRUNCATE TABLE cart_items CASCADE;
TRUNCATE TABLE carts CASCADE;
TRUNCATE TABLE product_inventories CASCADE;
TRUNCATE TABLE product_variants CASCADE;
TRUNCATE TABLE product_images CASCADE;
TRUNCATE TABLE product_categories CASCADE;
TRUNCATE TABLE products CASCADE;
TRUNCATE TABLE categories CASCADE;
TRUNCATE TABLE promos CASCADE;
TRUNCATE TABLE users CASCADE;

-- Re-enable foreign key checks
SET session_replication_role = 'origin';

-- Reset all sequences
ALTER SEQUENCE users_id_seq RESTART WITH 1;
ALTER SEQUENCE categories_id_seq RESTART WITH 1;
ALTER SEQUENCE products_id_seq RESTART WITH 1;
ALTER SEQUENCE product_images_id_seq RESTART WITH 1;
ALTER SEQUENCE product_variants_id_seq RESTART WITH 1;
ALTER SEQUENCE product_inventories_id_seq RESTART WITH 1;
ALTER SEQUENCE promos_id_seq RESTART WITH 1;
ALTER SEQUENCE carts_id_seq RESTART WITH 1;
ALTER SEQUENCE cart_items_id_seq RESTART WITH 1;
ALTER SEQUENCE orders_id_seq RESTART WITH 1;
ALTER SEQUENCE order_items_id_seq RESTART WITH 1;
ALTER SEQUENCE payments_id_seq RESTART WITH 1;
ALTER SEQUENCE notifications_id_seq RESTART WITH 1;

-- =====================================================
-- DATA CLEARED SUCCESSFULLY!
-- =====================================================
