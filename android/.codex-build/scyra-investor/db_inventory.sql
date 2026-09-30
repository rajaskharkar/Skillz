.headers on
.mode column
SELECT 'pearls' AS table_name, COUNT(*) AS rows FROM pearl_ledger
UNION ALL SELECT 'finds', COUNT(*) FROM user_shell_find_instance
UNION ALL SELECT 'badges', COUNT(*) FROM user_badge;
SELECT * FROM user_shell_find_instance;
SELECT * FROM user_badge;
