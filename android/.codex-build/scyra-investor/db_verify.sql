.headers on
.mode column
PRAGMA integrity_check;
SELECT SUM(delta) AS pearl_balance FROM pearl_ledger;
SELECT SUM(units) AS stillwater_drops FROM stillwater_ledger;
SELECT COUNT(*) AS active_creatures FROM user_shell_find_instance WHERE creatureStatus = 'ACTIVE';
SELECT COUNT(*) AS earned_badges FROM user_badge;
