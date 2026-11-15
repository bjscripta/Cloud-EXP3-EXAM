-- Ejecutar una vez con un usuario administrador de RDS.
-- No contiene contraseñas ni elimina tablas o datos existentes.
CREATE DATABASE IF NOT EXISTS p360_clientes CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS p360_productos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS p360_inventario CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS p360_pedidos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS p360_facturacion CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS p360_envios CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS p360_analitica CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- El usuario p360_app debe crearse por separado con una contraseña privada.
-- Después ejecutar database/rds-app-grants.sql.
