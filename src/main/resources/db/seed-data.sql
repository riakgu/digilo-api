-- =====================================================
-- DIGILO API - SEED DATA FOR FRONTEND DEVELOPMENT
-- =====================================================
-- 
-- USAGE:
--   1. Run clear-data.sql first (if re-seeding)
--   2. Run this script:
--      docker exec -i digilo-db psql -U $POSTGRES_USER -d digilo < src/main/resources/db/seed-data.sql
--
-- REGENERATE ENCRYPTED CREDENTIALS (if needed):
--   1. Update ENCRYPTION_PASSWORD & ENCRYPTION_SALT in CredentialGenerator.java to match your .env
--   2. Run: mvn exec:java -D exec.mainClass="com.riakgu.digilo.util.CredentialGenerator" -D exec.classpathScope="test"
--   3. Copy the generated INSERT statements to replace section 7 below
--
-- =====================================================

-- =====================================================
-- 1. USERS
-- =====================================================
-- Password for all users: "password123" (BCrypt hashed)
-- BCrypt hash: $2a$12$G6jCZRyc2H.z0ZPiuPbzZOJ/rWpljfB7NHx1Brmh.Q6bK3PKr9SoG

INSERT INTO users (id, email, password, name, phone, role, status, email_verified, phone_verified, created_at, updated_at) VALUES
(1, 'admin@digilo.com', '$2a$12$G6jCZRyc2H.z0ZPiuPbzZOJ/rWpljfB7NHx1Brmh.Q6bK3PKr9SoG', 'Admin Digilo', '081234567890', 'ADMIN', 'ACTIVE', true, true, NOW(), NOW()),
(2, 'john.doe@email.com', '$2a$12$G6jCZRyc2H.z0ZPiuPbzZOJ/rWpljfB7NHx1Brmh.Q6bK3PKr9SoG', 'John Doe', '081234567891', 'USER', 'ACTIVE', true, true, NOW(), NOW()),
(3, 'jane.smith@email.com', '$2a$12$G6jCZRyc2H.z0ZPiuPbzZOJ/rWpljfB7NHx1Brmh.Q6bK3PKr9SoG', 'Jane Smith', '081234567892', 'USER', 'ACTIVE', true, false, NOW(), NOW()),
(4, 'test.user@email.com', '$2a$12$G6jCZRyc2H.z0ZPiuPbzZOJ/rWpljfB7NHx1Brmh.Q6bK3PKr9SoG', 'Test User', NULL, 'USER', 'ACTIVE', false, false, NOW(), NOW());

SELECT setval('users_id_seq', 100);

-- =====================================================
-- 2. CATEGORIES
-- =====================================================
INSERT INTO categories (id, name, slug, description, is_active, created_at, updated_at) VALUES
(1, 'Streaming', 'streaming', 'Premium streaming service accounts for movies, music, and entertainment', true, NOW(), NOW()),
(2, 'Gaming', 'gaming', 'Game accounts, vouchers, and in-game currencies', true, NOW(), NOW()),
(3, 'Software', 'software', 'Software licenses and subscriptions', true, NOW(), NOW()),
(4, 'E-Wallet', 'e-wallet', 'E-wallet top-up vouchers and credits', true, NOW(), NOW()),
(5, 'VPN & Security', 'vpn-security', 'VPN subscriptions and security software', true, NOW(), NOW()),
(6, 'Cloud Storage', 'cloud-storage', 'Cloud storage subscriptions and space upgrades', true, NOW(), NOW());

SELECT setval('categories_id_seq', 100);

-- =====================================================
-- 3. PRODUCTS
-- =====================================================
INSERT INTO products (id, name, slug, description, is_active, is_featured, created_at, updated_at) VALUES
-- Streaming
(1, 'Netflix Premium', 'netflix-premium', 'Enjoy unlimited movies, TV shows, and Netflix Originals in Ultra HD quality. Stream on up to 4 devices simultaneously with downloads available on 6 devices.', true, true, NOW(), NOW()),
(2, 'Spotify Premium', 'spotify-premium', 'Ad-free music streaming with unlimited skips, offline listening, and high-quality audio. Access millions of songs and podcasts.', true, true, NOW(), NOW()),
(3, 'Disney+ Hotstar', 'disney-plus-hotstar', 'Stream Disney classics, Marvel, Star Wars, Pixar, National Geographic, and exclusive Indonesian content including live sports.', true, false, NOW(), NOW()),
(4, 'YouTube Premium', 'youtube-premium', 'Watch videos ad-free, download for offline viewing, play in background, and get YouTube Music Premium included.', true, false, NOW(), NOW()),
-- Gaming
(5, 'Steam Wallet', 'steam-wallet', 'Add funds to your Steam account to purchase games, DLC, and in-game items from the Steam store.', true, false, NOW(), NOW()),
(6, 'Mobile Legends Diamonds', 'mobile-legends-diamonds', 'Purchase diamonds for Mobile Legends: Bang Bang to unlock heroes, skins, and battle passes.', true, true, NOW(), NOW()),
(7, 'PUBG Mobile UC', 'pubg-mobile-uc', 'Unknown Cash (UC) for PUBG Mobile to purchase Royale Pass, outfits, weapon skins, and vehicle skins.', true, false, NOW(), NOW()),
(8, 'Valorant Points', 'valorant-points', 'VP for Valorant to unlock agents, weapon skins, battle passes, and exclusive bundles.', true, false, NOW(), NOW()),
-- Software
(9, 'Microsoft 365', 'microsoft-365', 'Full access to Word, Excel, PowerPoint, Outlook, and 1TB OneDrive storage. Includes premium features and regular updates.', true, false, NOW(), NOW()),
(10, 'Canva Pro', 'canva-pro', 'Professional design tools with premium templates, brand kit, background remover, resize magic, and unlimited storage.', true, false, NOW(), NOW()),
-- VPN
(11, 'NordVPN', 'nordvpn', 'Secure your internet connection with military-grade encryption. Access 5000+ servers in 60 countries with no-logs policy.', true, false, NOW(), NOW()),
-- Cloud
(12, 'Google One', 'google-one', 'Expand your Google storage for Drive, Gmail, and Photos. Get VPN access and exclusive member benefits.', true, false, NOW(), NOW());

SELECT setval('products_id_seq', 100);

-- =====================================================
-- 4. PRODUCT CATEGORIES (Many-to-Many)
-- =====================================================
INSERT INTO product_categories (product_id, category_id) VALUES
-- Streaming products
(1, 1), -- Netflix -> Streaming
(2, 1), -- Spotify -> Streaming
(3, 1), -- Disney+ -> Streaming
(4, 1), -- YouTube -> Streaming
-- Gaming products
(5, 2), -- Steam -> Gaming
(6, 2), -- ML Diamonds -> Gaming
(7, 2), -- PUBG UC -> Gaming
(8, 2), -- Valorant -> Gaming
-- Software products
(9, 3), -- M365 -> Software
(10, 3), -- Canva -> Software
-- VPN products
(11, 5), -- NordVPN -> VPN
-- Cloud products
(12, 6), -- Google One -> Cloud
(12, 3); -- Google One -> Software (multiple categories)

-- =====================================================
-- 5. PRODUCT IMAGES
-- =====================================================
-- Using placeholder image URLs (replace with actual Cloudflare R2 URLs)
INSERT INTO product_images (id, product_id, image_url, is_primary, display_order, created_at, updated_at) VALUES
(1, 1, 'https://placehold.co/800x600/E50914/FFFFFF?text=Netflix', true, 0, NOW(), NOW()),
(2, 2, 'https://placehold.co/800x600/1DB954/FFFFFF?text=Spotify', true, 0, NOW(), NOW()),
(3, 3, 'https://placehold.co/800x600/113CCF/FFFFFF?text=Disney%2B', true, 0, NOW(), NOW()),
(4, 4, 'https://placehold.co/800x600/FF0000/FFFFFF?text=YouTube', true, 0, NOW(), NOW()),
(5, 5, 'https://placehold.co/800x600/1B2838/FFFFFF?text=Steam', true, 0, NOW(), NOW()),
(6, 6, 'https://placehold.co/800x600/0D47A1/FFFFFF?text=Mobile+Legends', true, 0, NOW(), NOW()),
(7, 7, 'https://placehold.co/800x600/F7B633/000000?text=PUBG+Mobile', true, 0, NOW(), NOW()),
(8, 8, 'https://placehold.co/800x600/FA4454/FFFFFF?text=Valorant', true, 0, NOW(), NOW()),
(9, 9, 'https://placehold.co/800x600/0078D4/FFFFFF?text=Microsoft+365', true, 0, NOW(), NOW()),
(10, 10, 'https://placehold.co/800x600/00C4CC/FFFFFF?text=Canva+Pro', true, 0, NOW(), NOW()),
(11, 11, 'https://placehold.co/800x600/4687FF/FFFFFF?text=NordVPN', true, 0, NOW(), NOW()),
(12, 12, 'https://placehold.co/800x600/4285F4/FFFFFF?text=Google+One', true, 0, NOW(), NOW());

SELECT setval('product_images_id_seq', 100);

-- =====================================================
-- 6. PRODUCT VARIANTS
-- =====================================================
INSERT INTO product_variants (id, product_id, sku, name, price, delivery_type, duration_days, warranty_days, is_active, metadata, created_at, updated_at) VALUES
-- Netflix Premium Variants
(1, 1, 'NETFLIX-1M', '1 Month', 54000.00, 'AUTO', 30, 30, true, '{"region": "ID"}', NOW(), NOW()),
(2, 1, 'NETFLIX-3M', '3 Months', 149000.00, 'AUTO', 90, 30, true, '{"region": "ID"}', NOW(), NOW()),
(3, 1, 'NETFLIX-6M', '6 Months', 289000.00, 'AUTO', 180, 30, true, '{"region": "ID"}', NOW(), NOW()),
(4, 1, 'NETFLIX-12M', '1 Year', 549000.00, 'AUTO', 365, 30, true, '{"region": "ID"}', NOW(), NOW()),

-- Spotify Premium Variants
(5, 2, 'SPOTIFY-1M', '1 Month', 54990.00, 'AUTO', 30, 7, true, '{"type": "individual"}', NOW(), NOW()),
(6, 2, 'SPOTIFY-3M', '3 Months', 149000.00, 'AUTO', 90, 7, true, '{"type": "individual"}', NOW(), NOW()),
(7, 2, 'SPOTIFY-6M', '6 Months', 279000.00, 'AUTO', 180, 7, true, '{"type": "individual"}', NOW(), NOW()),
(8, 2, 'SPOTIFY-12M', '1 Year', 549000.00, 'AUTO', 365, 7, true, '{"type": "individual"}', NOW(), NOW()),

-- Disney+ Hotstar Variants
(9, 3, 'DISNEY-1M', '1 Month', 39000.00, 'AUTO', 30, 7, true, NULL, NOW(), NOW()),
(10, 3, 'DISNEY-3M', '3 Months', 99000.00, 'AUTO', 90, 7, true, NULL, NOW(), NOW()),
(11, 3, 'DISNEY-12M', '1 Year', 199000.00, 'AUTO', 365, 7, true, NULL, NOW(), NOW()),

-- YouTube Premium Variants
(12, 4, 'YT-1M', '1 Month', 59000.00, 'AUTO', 30, 7, true, NULL, NOW(), NOW()),
(13, 4, 'YT-3M', '3 Months', 169000.00, 'AUTO', 90, 7, true, NULL, NOW(), NOW()),
(14, 4, 'YT-12M', '1 Year', 649000.00, 'AUTO', 365, 7, true, NULL, NOW(), NOW()),

-- Steam Wallet Variants
(15, 5, 'STEAM-12K', 'IDR 12.000', 15000.00, 'AUTO', NULL, 1, true, '{"currency": "IDR"}', NOW(), NOW()),
(16, 5, 'STEAM-45K', 'IDR 45.000', 52000.00, 'AUTO', NULL, 1, true, '{"currency": "IDR"}', NOW(), NOW()),
(17, 5, 'STEAM-90K', 'IDR 90.000', 100000.00, 'AUTO', NULL, 1, true, '{"currency": "IDR"}', NOW(), NOW()),
(18, 5, 'STEAM-250K', 'IDR 250.000', 270000.00, 'AUTO', NULL, 1, true, '{"currency": "IDR"}', NOW(), NOW()),

-- Mobile Legends Diamonds Variants (large amounts use HYBRID)
(19, 6, 'ML-86', '86 Diamonds', 20000.00, 'AUTO', NULL, 1, true, NULL, NOW(), NOW()),
(20, 6, 'ML-172', '172 Diamonds', 38000.00, 'AUTO', NULL, 1, true, NULL, NOW(), NOW()),
(21, 6, 'ML-257', '257 Diamonds', 57000.00, 'AUTO', NULL, 1, true, NULL, NOW(), NOW()),
(22, 6, 'ML-706', '706 Diamonds', 150000.00, 'HYBRID', NULL, 1, true, NULL, NOW(), NOW()),

-- PUBG Mobile UC Variants
(23, 7, 'PUBG-60', '60 UC', 16000.00, 'AUTO', NULL, 1, true, NULL, NOW(), NOW()),
(24, 7, 'PUBG-325', '325 UC', 79000.00, 'AUTO', NULL, 1, true, NULL, NOW(), NOW()),
(25, 7, 'PUBG-660', '660 UC', 159000.00, 'AUTO', NULL, 1, true, NULL, NOW(), NOW()),
(26, 7, 'PUBG-1800', '1800 UC', 399000.00, 'AUTO', NULL, 1, true, NULL, NOW(), NOW()),

-- Valorant Points Variants (large amounts use MANUAL for verification)
(27, 8, 'VP-125', '125 VP', 15000.00, 'AUTO', NULL, 1, true, NULL, NOW(), NOW()),
(28, 8, 'VP-420', '420 VP', 50000.00, 'AUTO', NULL, 1, true, NULL, NOW(), NOW()),
(29, 8, 'VP-700', '700 VP', 80000.00, 'HYBRID', NULL, 1, true, NULL, NOW(), NOW()),
(30, 8, 'VP-1375', '1375 VP', 150000.00, 'MANUAL', NULL, 1, true, NULL, NOW(), NOW()),

-- Microsoft 365 Variants (HYBRID - credentials sent via email after verification)
(31, 9, 'M365-1M', '1 Month', 99000.00, 'HYBRID', 30, 7, true, '{"plan": "personal"}', NOW(), NOW()),
(32, 9, 'M365-12M', '1 Year', 949000.00, 'HYBRID', 365, 7, true, '{"plan": "personal"}', NOW(), NOW()),

-- Canva Pro Variants (MANUAL - admin sends credentials after payment)
(33, 10, 'CANVA-1M', '1 Month', 95000.00, 'MANUAL', 30, 7, true, NULL, NOW(), NOW()),
(34, 10, 'CANVA-12M', '1 Year', 949000.00, 'MANUAL', 365, 7, true, NULL, NOW(), NOW()),

-- NordVPN Variants
(35, 11, 'NORD-1M', '1 Month', 179000.00, 'AUTO', 30, 7, true, NULL, NOW(), NOW()),
(36, 11, 'NORD-12M', '1 Year', 799000.00, 'AUTO', 365, 7, true, NULL, NOW(), NOW()),
(37, 11, 'NORD-24M', '2 Years', 1199000.00, 'AUTO', 730, 7, true, NULL, NOW(), NOW()),

-- Google One Variants
(38, 12, 'GONE-100GB-1M', '100GB - 1 Month', 26900.00, 'AUTO', 30, 7, true, '{"storage": "100GB"}', NOW(), NOW()),
(39, 12, 'GONE-100GB-12M', '100GB - 1 Year', 269000.00, 'AUTO', 365, 7, true, '{"storage": "100GB"}', NOW(), NOW()),
(40, 12, 'GONE-200GB-12M', '200GB - 1 Year', 449000.00, 'AUTO', 365, 7, true, '{"storage": "200GB"}', NOW(), NOW());

SELECT setval('product_variants_id_seq', 100);

-- =====================================================
-- 7. PRODUCT INVENTORIES
-- =====================================================
-- Encrypted credentials generated by CredentialGenerator.java
-- To regenerate: mvn exec:java -D exec.mainClass="com.riakgu.digilo.util.CredentialGenerator" -D exec.classpathScope="test"

INSERT INTO product_inventories (id, variant_id, credential, status, created_at, updated_at) VALUES
(1, 1, '709c27d5a1b32f07d5a8604fca46d085eca7f129c6d78fc9893f678fc89950d7ed818133157548a501146da443c1576f2c150bb206576a937539c3fe0f22ef4b49578c7023b9b0c867b2be07566c038f2a87b67fe82a3df5e47b614c03ed5fe56125797b3d97c731c68156f6f203e68d', 'AVAILABLE', NOW(), NOW()),       
(2, 1, 'f387f6402507c9e22172375872badc65cb6dca8f2615bc276600d40432236e087f215fad959c1e49096105a3088dc9a4e0f4c7dbdb10b95ff77c90f3c50c7c918df96cf14ad16256f541ebf2f6964f58296ed0b6bb4539babe578fe7a388563abbcb8be7792101e91268815290faa669', 'AVAILABLE', NOW(), NOW()),       
(3, 1, '61e7115711524c047e67977767fe3329c19e199a2c2f3b0b1ffe35cb75eb00c32cb4bcb0ffeb0ff1f8120698e93a42d4718e8059c81481a85db6ab648eab1e55d6a3af8b3966d56ab10a83714303db8a6859b679fd95b5df353f7f12566ca3ff40335a1f13a69bf194499c2ad5e81051', 'AVAILABLE', NOW(), NOW()),       
(4, 1, 'fc486ac5cc58106e1508981b1bac02b56e6ffcfe7799a7ccc0af7763181db42811c90a9749d983d45fbc18caa5eb6f3389c21720aae202dcbae92476e91d87fe0b00d74399aa7d80cad4233c6bfb3d59722b115798c1718e5f5879f3538f1a57f43536c2bc1a32e69bc443ffde943d96', 'AVAILABLE', NOW(), NOW()),       
(5, 1, 'a9d6ac9c7f98ba02f49ad2d9d07f9abe9d6dd1edd8d6fc3fbae4e3215bf52f2b106682206f74eb77182e0adeeb1391ff218878c67e8bf0d2279a70449aa38e148ddcde0ea679665442de5b30e761b95f9c5340d0c9a5dcad29e0fdaf58aad6602c4e55b9acd4c1ef287cff60589d250d', 'AVAILABLE', NOW(), NOW()),       
(6, 2, 'db0cdcf217dfb6c5c179c705616fa89f93d3673cffdb5989436c89d74664c3bec4acf8358269473e708ac380218e7f7d7e0b85cebc2d6783343908c73eafd73e9665f7c8383bdfce2c28dcfa523daee704df9b36bc8ea5e84471db6b13896e002683c5e8654dd1358651f634e9f9213e', 'AVAILABLE', NOW(), NOW()),       
(7, 2, 'a3778a2fe833ce0338d98a2f232cc5f30f0ddc33420fad8a121110bbefad46df126d742e85bf2d50308671bd9b5ca5bc28cc6366a837f53a38af85331dbef2913e7093113955b0b2770036a4b4c16705cba71f9487e786e3501c55dab7e5144be0c711ac3067131784d05bf32dc122ce', 'AVAILABLE', NOW(), NOW()),       
(8, 2, '29da774bfd2fcfd1eaffd0a5d22bcb26f28f2aabed15c5d575c90f59c68ab2afbcb67bb7dccca6685b20c1cd3a542693032232d90fe278d33d474e3bae546cb2347b118e5d825387f66cc3aaceb02386fe696957b166a68971ff4bbab433e41de7c9cd7f794386f5b1dc366c757f4357', 'AVAILABLE', NOW(), NOW()),       
(9, 5, '6b969fcc1e5bba7c76c4d0bd8786b18381610180670daab91b85739215eee7572841548ac06f26f80001935e20918c47a742e6c53817deb2803af7d85dc6e1fa5a4a5d5a929b84ec955580bad834c35b8051406ceb98c71c75ec8be5130120c5a18afbe4c186bc91518cc0d479929ca6', 'AVAILABLE', NOW(), NOW()),       
(10, 5, '3ed7d36d03e97c4b6f2d5396d3539d24f937310dc08e55c0d3e6b471fe6798a2515162d36ed7b79c85809c12394b1a09c3e9c8305edbc8b4af67f424054b10fb582b9a5b955a8dca3658b97bcb5f926774ada0aa3edc71478ed05344e361a462d6d379801d21e8802bf89c90acfe9da2', 'AVAILABLE', NOW(), NOW()),      
(11, 5, 'd44e02ae628802959e173e4add9799ada38f622c9bfcb558159f3b37d30e72d9d885aac5727120852cdebf4db39d73194834433ed92033332d7ce7d2f933329173b4215660f47dcb0115504291427c17ad8dc8116c228b3999507a4801f6ecebd3e5f5e1a1422d0bac6817f083a43b33', 'AVAILABLE', NOW(), NOW()),      
(12, 5, '80e459361d489329eafe0927467dc7fd303e48edf1d84a89ca57c6f51de1eca5e05b7c4077013663f372995a4cd9ae66a6e94cfbf739068439fb7bb1d02598e9df978ebee1412ef3edd5e727a079762a693f90fdfca599b15065125b1283648e16203ac802e796edabd37456795ba51f', 'AVAILABLE', NOW(), NOW()),      
(13, 5, '6636ea378890058068a5718c826bb7d4c252cd591154bd35cd54528f10de44ce2f32a750a088a161c4af1e33bc28753513121b229e47a1376901c995ca23ee87c81bdf1e9c8ef29bc3db6bde8aefd9ed271de60e655b68ec10776ee53028c3fc20b6afba9709ab1debc7a25f205a6226', 'AVAILABLE', NOW(), NOW()),      
(14, 15, '7bcdc01b64de52a5a6ef833f1cdaaf1c65c2dd41b543367d76846d052b0dd827edfc83b956d59c23ccba0f11aa84ee3b9e4875d3117168362f5f4b637dc4e4f886ce779c9e7585532eca07b4f5cac108ff6c0ab643728164c974648ca52f7cd4', 'AVAILABLE', NOW(), NOW()),
(15, 15, 'ff74ce21e5bff2bba680d66cd57413bc4f3818b99dd62f13b3b0c9f8f90642283da4fc836728c1701084404973dc34c9902a95c8ba4179556aebd0e2b4ce5dcc2a2ffe4ea356fb7e22167c977c62a8bf23ad6fff40177accfa21a92dd780a93a', 'AVAILABLE', NOW(), NOW()),
(16, 15, '090d4a25fb1f8d97da518325b276f7d52796d8addf50c7e8ef132bd439b5278afbb140082ad74efc46d2e57aa8229088ce23fee8ade765b60af95d7e7ea147da132dfa37c90ea07bb47bfdde09c1e268e98ee50dd9047c2431dc761d4e3e9429', 'AVAILABLE', NOW(), NOW()),
(17, 15, '5c03244d92be344dd67d41bff8e86eab7a22eaddd19552741d222d2edeeafeff2cc5c8fd7046db0249665b4a1c80a9d98e6cebd706b9736a11dfd8d0bca6bbea634cc555f3899c45005a6c0b271b8ea2bc3c2fccf00dbbd544617e460021734a', 'AVAILABLE', NOW(), NOW()),
(18, 15, '76586fe6b23545608ccf7f9802fe1bfc4bc63923b3f15a7ce3c31fabb74e14d8c1c13f3508d884bb6c45b9309d23359bd4dfec32dd0c919b299b0481d332679c772dc21b316097e7ea1a3819a2b9517fedf88dd40c96bb3bdaa8d65d4a7355ee', 'AVAILABLE', NOW(), NOW()),
(19, 15, '213361e5149f73347985354ec021fdcfa27887c57fc140d2ef0bc82646f65af6e708504fcb12ccaa21610fdc910b83b8decf3ff6ec5e3ea0cbb0127d7b896f69b059b272bfeb7aa489393ea8afda8f7ecd4cb1499fda0a99fca612ef194dada2', 'AVAILABLE', NOW(), NOW()),
(20, 15, '4786df8fa301afc1b9a802948f202b3b07290b522d7a89ce6146bacb2eacb38040d776b5d611baa0c20389dedd925a5b0b01f40cf24661063472b23ff0323029043921c19d8e9efd5fd39c62cd138abf8b209f877f76d4b84b63bd2430e215ac', 'AVAILABLE', NOW(), NOW()),
(21, 15, 'b56402d7b05ce78a966c34e1291339def2e5943338ce06e6a17b1a62c2329bccad0b5cb1e83521d8ebf8e2f9f19144053ed525a4aa079d9387f7b39dd4f293006e38afede5cbd416ff77c811f741f3555d1d387025e11776eb610d8a4d9ec59f', 'AVAILABLE', NOW(), NOW()),
(22, 15, '160c8c1453541eb14fc5f9f2c5e3ad6c0cff55034dfda7883ae336d7ceafc1ce92b36ebf164c0b50e8514a3959aa650fccbf7eba9cd0f004eee060b258cc72942cc9475ea5ac36f786fed7aa2870a7413d83e65c353a7c8bb01e0003d4e1f7ca', 'AVAILABLE', NOW(), NOW()),
(23, 15, 'dc9f897d5dc9a1c0b8c5e5522d9ded1b4951f76a0ffd9f94edbad01239fc6949c0e9e1504fb1f3ac6541ddd2a63f578eaa8174a3611d88c7b7893735916a8a3228ed845ddfa81bb07ff4e7c40af577e6bd1bde2a8d752aa16b14af4bc3e99a79', 'AVAILABLE', NOW(), NOW()),
(24, 19, '7a1c7e0e6dc26db0bee95d6ce0aae15af66b86026eff32111c45fc7720fe261290cf470ca9821933b14a85af5edaac8a65baf3795fe4c8119c3a14b052ea3e64c1725cfd9adfbfd2cdf6358b5fc21af40014f1d23cf1d9b40746e47cd18f5214', 'AVAILABLE', NOW(), NOW()),
(25, 19, '95230b850554e96ac75697aa84d69b0e24b3b36ff851d167c824f0788df9fe094623d7e3f60616bc03c89248fd9a3f084d1b33e414e0f8568c3249a4dbdcba7692ab2331e37ebe15e987e2ae436af0b44c57cd839d297dcf1748bab185c5c9f1', 'AVAILABLE', NOW(), NOW()),
(26, 19, '3fe65e65024d20f67f385b5f1a77306445c1373d2e0e7ff0b40413bf73a7725ae4b7399ee6685e3975131f9ffbfd721fe30f521b45bcc249bf4bdf94323486a8fa6d48bda26ace01ff2c6b393c6c77189b2a535b5686510bbc3d52352b9dc78e', 'AVAILABLE', NOW(), NOW()),
(27, 19, 'ecd7dbc49bbddd718400b509d8df62613f1ee29218395d303cbab98d8f171e5be161867c84c87039c883488e7acf5216161e09e608c78c5f4ebb40e27f0e9bfb637ee39b1143626b33e0e87d5e5fe195f83ed6143f86234ec6c00ee764546fc3', 'AVAILABLE', NOW(), NOW()),
(28, 19, '4646f1a2a3360e6a326d3305e4d57983386c7dbbab89d9b3c30be2d15bf9dd46479747f3968aabcd9fafd058d68df44fc4c5046e4bac2f3ab5c84079e05412ca1799953b76ed9bf8926cbe6ca7ac5c6006262216d9b4ed57ca8164cc466b73de', 'AVAILABLE', NOW(), NOW()),
(29, 19, 'c35d4943985de03b479778be346efc4ca1f42f42c0afc24b938d8e51d858ace8dc32ca17213df55b542b6f249f21d3e15b37727514834518d05feda63a70b5309930acbc30d6e6f24f8244dc22f0c380662603798f03fe006f143857c8423149', 'AVAILABLE', NOW(), NOW()),
(30, 19, 'b756c8484fa6ed4fc6a76aaf795231eba791fae1f59b36cd3b7c027cc5d8bb86afbfd9cc9645e0a576a1a89f1d4bc52b1e1659df761cd07385050b5aeaa553a2917659712bd491fed73ad2b7636c2f32b5c8a6d20175d55bb0621bfc6cb4a32c', 'AVAILABLE', NOW(), NOW()),
(31, 19, 'e48d7db94970534b3ee69290c5aa7914517c7680c2a3cf2d40e0453e4db0b7aae270626de43290d88d39120156ebda487abaa50c73fcee5b4e89be99ae850d787606400a4aa7d3167f5562c92efeeda0e63b87b21984b2738998ee900d8c3d1d', 'AVAILABLE', NOW(), NOW()),
(32, 19, 'baad7dcd32317ee95d8c16146f2a344336ce1e58bb20de4d3a428527c5d37cd66bf766ad63872ef631fa8a4eb0dd97f333838f31167f9519f092b06d9c2bb4d565610c3bad937a6400b861851034fd8ffb0faa3a24812b8d99eb05eab126ba4e', 'AVAILABLE', NOW(), NOW()),
(33, 19, 'b640eac96538336a5832d300f56ab65506c0538492eadcea62b4f49f0dcb97f6996ca8b70b3487f11138adc452abcfe57911b0991fdf4fd204834ee37674be22b8560d5cdae641334f4ae748a6a5c120c73a114622abaf91bfdd3ed49e87db5d', 'AVAILABLE', NOW(), NOW()),
(34, 3, 'eef439595dae58ccc417dc5c0976bff87a86cb4bffc5c576f23aa9cd7ab35a2fa9d70d74a1eca64cd64aafe3a9774690c57204e280fb6884f78a0af105de3f54fba5e9e77e0131833e035ee890fa020e781d2d87ef7e5cb6e1d2cf52f069afad1d20dcb57932fef0d7871bf2b0600615', 'AVAILABLE', NOW(), NOW()),      
(35, 4, '5f8bb186bdba834e8ecc8aff6a4dde96c25e7b0396eae7a65c94294279a5de8784eb52cc9ca25091bbae479f625b8f40deddace35f57545740b6f838999ca03711a93d8bb414ffed7992bea544b7519eeb2848762b2fea77fa84e343a25cc33e2269652e90dc19935fe461e60a24013d', 'AVAILABLE', NOW(), NOW()),      
(36, 6, '36d07e3b542b4a0d15ed20611650dcfe159080f3a89ab4492f24c9c3fff45dc96d3cf1a22f2f1615193dbf603f26b1bd20a33afc4931dd24cadf5ed8f8aa9d25a5eab815eff93550649c5acd18179e96ed631042fb153170b672a993ac09514a7f5faccef9eacfb78697a5f07c1612de', 'AVAILABLE', NOW(), NOW()),      
(37, 7, 'fc253ea37a3ffb62d96c2ef9b3f15a3590efc8e00085e8622f92bae1b7f8021cf2c2cea8063ee81d18d2b6aa7d5e533336a6a8cebe73955bc4f5c3488c70ba051a794101532f164fa0b1414bb399a1143408cb7479dc5bffe54f416bb1910ee6e3a8e34b5012bfb313fd66b847bcf942', 'AVAILABLE', NOW(), NOW()),      
(38, 8, '51168bc09ed209f56c43c39a29bcead8273b24c010eae991d05c17b9af0b6c0f824d9c3ecb3b9dac1bc9fa9f462b50703b5355ba0f05862327ee52c472e20db725fa1db8d5c81a1fb2056451e77f714d2ca4e32dc7ef55e223c2d72cac673bf90aa7e84130f35f96602fba0ed4e11de0', 'AVAILABLE', NOW(), NOW()),      
(39, 9, 'f3ce21c0daf36e9bb805bc65a1f2ea8d4e9a1bff72d41697ac1cc98e9f0806784d69cad7fc024b495864284bdd5629d5c44b4403f978931afea12146199856cbf8f4d5a686b2e23ced685eeccdbbfdbfea715aad7b7de81bd6e4bcaa1c4e6e01b71b28e5d1925d84e51639978a170408', 'AVAILABLE', NOW(), NOW()),      
(40, 10, '2f5c8cd666eacb54cb15a442d7bf6f9794ffe6da92040676aa4ee358a63805be669694e775c7e272b81ac5bd77aee588e1c8f3a4ae51fdde851540867158108f6eaf702b8106c459ae5d053a2c260f6ae41e332386072c560b46f0acd95109b29f7077af28d84c9607fcd1817c027239', 'AVAILABLE', NOW(), NOW());

SELECT setval('product_inventories_id_seq', 100);

-- =====================================================
-- 8. PROMO CODES
-- =====================================================
INSERT INTO promos (id, code, name, description, discount_type, discount_value, max_discount, min_order_amount, max_total_usage, max_usage_per_user, used_count, starts_at, expires_at, is_active, created_at, updated_at) VALUES
(1, 'WELCOME10', 'Welcome Discount 10%', 'Get 10% off on your first order!', 'PERCENT', 10.00, 50000.00, 50000.00, 1000, 1, 0, NOW(), NOW() + INTERVAL '1 year', true, NOW(), NOW()),
(2, 'SAVE20K', 'Save IDR 20,000', 'Flat IDR 20,000 discount for orders above IDR 100,000', 'FIXED', 20000.00, NULL, 100000.00, 500, 2, 0, NOW(), NOW() + INTERVAL '6 months', true, NOW(), NOW()),
(3, 'GAMING15', 'Gaming Special 15%', '15% off for all gaming products', 'PERCENT', 15.00, 75000.00, 75000.00, 200, 1, 0, NOW(), NOW() + INTERVAL '3 months', true, NOW(), NOW()),
(4, 'STREAM25', 'Streaming Discount 25%', '25% off for streaming subscriptions', 'PERCENT', 25.00, 100000.00, 100000.00, 100, 1, 0, NOW(), NOW() + INTERVAL '1 month', true, NOW(), NOW()),
(5, 'EXPIRED50', 'Expired Promo', 'This is an expired promo for testing', 'PERCENT', 50.00, 200000.00, 50000.00, 100, 1, 10, NOW() - INTERVAL '60 days', NOW() - INTERVAL '30 days', true, NOW(), NOW()),
(6, 'INACTIVE20', 'Inactive Promo', 'This promo is deactivated', 'PERCENT', 20.00, 50000.00, 50000.00, 100, 1, 0, NOW(), NOW() + INTERVAL '1 year', false, NOW(), NOW());

SELECT setval('promos_id_seq', 100);

-- =====================================================
-- 9. CARTS (for test users)
-- =====================================================
INSERT INTO carts (id, user_id, created_at, updated_at) VALUES
(1, 2, NOW(), NOW()),
(2, 3, NOW(), NOW()),
(3, 4, NOW(), NOW());

SELECT setval('carts_id_seq', 100);

-- =====================================================
-- 10. SAMPLE CART ITEMS (for user 2 - John Doe)
-- =====================================================
INSERT INTO cart_items (id, cart_id, variant_id, quantity, created_at, updated_at) VALUES
(1, 1, 1, 1, NOW(), NOW()),  -- Netflix 1M
(2, 1, 5, 2, NOW(), NOW());  -- Spotify 1M x2

SELECT setval('cart_items_id_seq', 100);

-- =====================================================
-- 11. SAMPLE ORDERS (historical data)
-- =====================================================
INSERT INTO orders (id, order_number, user_id, promo_id, status, subtotal, discount_amount, total_amount, notes, created_at, updated_at) VALUES
(1, 'ORD-20260201-0001', 2, NULL, 'COMPLETED', 54000.00, 0.00, 54000.00, NULL, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
(2, 'ORD-20260202-0001', 2, 1, 'COMPLETED', 149000.00, 14900.00, 134100.00, 'Used WELCOME10 promo', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
(3, 'ORD-20260203-0001', 3, NULL, 'PAID', 100000.00, 0.00, 100000.00, NULL, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
(4, 'ORD-20260204-0001', 2, NULL, 'PENDING', 79000.00, 0.00, 79000.00, NULL, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
(5, 'ORD-20260205-0001', 3, 2, 'CANCELLED', 150000.00, 20000.00, 130000.00, 'User cancelled', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days');

SELECT setval('orders_id_seq', 100);

-- =====================================================
-- 12. ORDER ITEMS
-- =====================================================
INSERT INTO order_items (id, order_id, variant_id, variant_name, product_name, product_image_url, price, quantity, created_at, updated_at) VALUES
(1, 1, 1, '1 Month', 'Netflix Premium', 'https://placehold.co/800x600/E50914/FFFFFF?text=Netflix', 54000.00, 1, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
(2, 2, 2, '3 Months', 'Netflix Premium', 'https://placehold.co/800x600/E50914/FFFFFF?text=Netflix', 149000.00, 1, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
(3, 3, 17, 'IDR 90.000', 'Steam Wallet', 'https://placehold.co/800x600/1B2838/FFFFFF?text=Steam', 100000.00, 1, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
(4, 4, 24, '325 UC', 'PUBG Mobile UC', 'https://placehold.co/800x600/F7B633/000000?text=PUBG+Mobile', 79000.00, 1, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
(5, 5, 22, '706 Diamonds', 'Mobile Legends Diamonds', 'https://placehold.co/800x600/0D47A1/FFFFFF?text=Mobile+Legends', 150000.00, 1, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days');

SELECT setval('order_items_id_seq', 100);

-- =====================================================
-- 13. PAYMENTS
-- =====================================================
INSERT INTO payments (id, order_id, provider, payment_type, provider_order_id, provider_transaction_id, amount, currency, status, paid_at, expired_at, qr_code_url, created_at, updated_at) VALUES
(1, 1, 'MIDTRANS', 'qris', 'DIGILO-ORD-20260201-0001', 'TXN-001', 54000.00, 'IDR', 'SUCCESS', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days' + INTERVAL '15 minutes', NULL, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
(2, 2, 'MIDTRANS', 'qris', 'DIGILO-ORD-20260202-0001', 'TXN-002', 134100.00, 'IDR', 'SUCCESS', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days' + INTERVAL '15 minutes', NULL, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
(3, 3, 'MIDTRANS', 'qris', 'DIGILO-ORD-20260203-0001', 'TXN-003', 100000.00, 'IDR', 'SUCCESS', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days' + INTERVAL '15 minutes', NULL, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
(4, 4, 'MIDTRANS', 'qris', 'DIGILO-ORD-20260204-0001', NULL, 79000.00, 'IDR', 'PENDING', NULL, NOW() + INTERVAL '15 minutes', 'https://api.sandbox.midtrans.com/v2/qris/sample', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
(5, 5, 'MIDTRANS', 'qris', 'DIGILO-ORD-20260205-0001', NULL, 130000.00, 'IDR', 'EXPIRED', NULL, NOW() - INTERVAL '2 days' + INTERVAL '15 minutes', NULL, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days');

SELECT setval('payments_id_seq', 100);

-- =====================================================
-- 14. NOTIFICATIONS
-- =====================================================
INSERT INTO notifications (id, user_id, type, title, message, reference_type, reference_id, is_read, read_at, created_at, updated_at) VALUES
(1, 2, 'ORDER_COMPLETED', 'Order Completed', 'Your order ORD-20260201-0001 has been completed. Check your email for credentials.', 'ORDER', 1, true, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
(2, 2, 'PAYMENT_SUCCESS', 'Payment Successful', 'Payment of IDR 134,100 for order ORD-20260202-0001 was successful.', 'PAYMENT', 2, true, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
(3, 2, 'ORDER_COMPLETED', 'Order Completed', 'Your order ORD-20260202-0001 has been completed. Check your email for credentials.', 'ORDER', 2, false, NULL, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
(4, 3, 'PAYMENT_SUCCESS', 'Payment Successful', 'Payment of IDR 100,000 for order ORD-20260203-0001 was successful.', 'PAYMENT', 3, false, NULL, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
(5, 2, 'ORDER_CREATED', 'Order Created', 'Your order ORD-20260204-0001 is pending payment. Complete payment within 15 minutes.', 'ORDER', 4, false, NULL, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day');

SELECT setval('notifications_id_seq', 100);

-- =====================================================
-- COMPLETED!
-- =====================================================
-- Summary:
--   - 4 Users (1 admin, 3 regular)
--   - 6 Categories
--   - 12 Products
--   - 40 Product Variants
--   - 40 Inventory Items (placeholder credentials)
--   - 6 Promo Codes (4 active, 1 expired, 1 inactive)
--   - 3 Carts with sample items
--   - 5 Sample Orders with various statuses
--   - 5 Notifications
-- =====================================================
