-- Usuarios de desenvolvimento/teste. Senhas em texto claro documentadas em
-- docs/auth/README.md. NAO usar estes valores em produção.
INSERT INTO users (email, password_hash, nome, role, telefone) VALUES
  ('admin@distribuidor.com', '$2a$10$zd86Z1jcRetxQceQYU.8dOaT2UvKpXY7uMuWRsZ1.2BDPbKSqA0QK', 'Admin Master', 'admin', '11999999999'),
  ('gerente@distribuidor.com', '$2a$10$a5hCN0cvEXPf7qGOFSsC3.DY9GXxDDDrMBt.Wh69gfuQLmvuS01um', 'Gerente Estoque', 'gerente_estoque', '11988888888'),
  ('vendedor1@distribuidor.com', '$2a$10$BSVLmLFaaGUVUrwQfGW1GOGpqGHy8aYpWMt9YXYrfEJEQX/5v2ixW', 'João Vendedor', 'vendedor', '11987654321');
