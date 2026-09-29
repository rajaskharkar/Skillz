BEGIN IMMEDIATE;

INSERT OR REPLACE INTO pearl_ledger
    (id, delta, reason, sourceType, sourceId, createdAt, note)
VALUES
    ('codex-investor-pearls', 640, 'INVESTOR_DEMO', 'DEMO', 'investor-deck', CAST(strftime('%s','now') AS INTEGER) * 1000, 'Reversible demo state for investor screenshots');

INSERT OR REPLACE INTO stillwater_ledger
    (id, units, sourceType, sourceId, createdAt)
VALUES
    ('codex-investor-drops', 60000, 'DEMO', 'investor-deck', CAST(strftime('%s','now') AS INTEGER) * 1000);

INSERT OR REPLACE INTO user_shell_find_instance
    (instanceId, findId, acquiredAt, sourceType, sourceId, currentUpgradeStageId, customName, isNew, isArchivedInChest, viewedAt, animalLevel, creatureStatus, creatureSource, flowTimeValueMinutes, lastActivityAt)
VALUES
    ('demo-minnow-1', 'focus_minnow', CAST(strftime('%s','now') AS INTEGER) * 1000 - 700000, 'FLOW', 'demo-flow-1', 'focus_minnow_bright', 'Spark', 0, 0, CAST(strftime('%s','now') AS INTEGER) * 1000, 7, 'ACTIVE', 'FLOW_EARNED', 10, CAST(strftime('%s','now') AS INTEGER) * 1000),
    ('demo-minnow-2', 'focus_minnow', CAST(strftime('%s','now') AS INTEGER) * 1000 - 650000, 'FLOW', 'demo-flow-2', 'focus_minnow_base', NULL, 0, 0, CAST(strftime('%s','now') AS INTEGER) * 1000, 3, 'ACTIVE', 'FLOW_EARNED', 10, CAST(strftime('%s','now') AS INTEGER) * 1000),
    ('demo-clownfish', 'creature_clownfish', CAST(strftime('%s','now') AS INTEGER) * 1000 - 600000, 'BEYOND_BLUE', 'demo-encounter-1', NULL, 'Ember', 0, 0, CAST(strftime('%s','now') AS INTEGER) * 1000, 5, 'ACTIVE', 'BEYOND_BLUE', 20, CAST(strftime('%s','now') AS INTEGER) * 1000),
    ('demo-turtle', 'creature_sea_turtle', CAST(strftime('%s','now') AS INTEGER) * 1000 - 550000, 'BEYOND_BLUE', 'demo-encounter-2', NULL, 'Drift', 0, 0, CAST(strftime('%s','now') AS INTEGER) * 1000, 12, 'ACTIVE', 'BEYOND_BLUE', 30, CAST(strftime('%s','now') AS INTEGER) * 1000),
    ('demo-seahorse', 'focus_seahorse', CAST(strftime('%s','now') AS INTEGER) * 1000 - 500000, 'FLOW', 'demo-flow-3', 'focus_seahorse_bright', 'Crown', 0, 0, CAST(strftime('%s','now') AS INTEGER) * 1000, 9, 'ACTIVE', 'FLOW_EARNED', 25, CAST(strftime('%s','now') AS INTEGER) * 1000),
    ('demo-manta', 'focus_manta', CAST(strftime('%s','now') AS INTEGER) * 1000 - 450000, 'FLOW', 'demo-flow-4', 'focus_manta_moonlit', 'Lumen', 0, 0, CAST(strftime('%s','now') AS INTEGER) * 1000, 18, 'ACTIVE', 'FLOW_EARNED', 45, CAST(strftime('%s','now') AS INTEGER) * 1000),
    ('demo-whale', 'focus_whale', CAST(strftime('%s','now') AS INTEGER) * 1000 - 400000, 'FLOW', 'demo-flow-5', 'focus_whale_base', 'North', 0, 0, CAST(strftime('%s','now') AS INTEGER) * 1000, 24, 'ACTIVE', 'FLOW_EARNED', 90, CAST(strftime('%s','now') AS INTEGER) * 1000),
    ('demo-shrimp', 'stillwater_shrimp', CAST(strftime('%s','now') AS INTEGER) * 1000 - 350000, 'STILLWATER', 'demo-vessel-1', NULL, NULL, 0, 0, CAST(strftime('%s','now') AS INTEGER) * 1000, 4, 'ACTIVE', 'STILLWATER', 15, CAST(strftime('%s','now') AS INTEGER) * 1000);

INSERT OR REPLACE INTO creature_discovery
    (speciesId, firstDiscoveredAt, acquisitionSource, firstCreatureId, updatedAt)
VALUES
    ('focus_minnow', CAST(strftime('%s','now') AS INTEGER) * 1000 - 700000, 'FLOW_EARNED', 'demo-minnow-1', CAST(strftime('%s','now') AS INTEGER) * 1000),
    ('creature_clownfish', CAST(strftime('%s','now') AS INTEGER) * 1000 - 600000, 'BEYOND_BLUE', 'demo-clownfish', CAST(strftime('%s','now') AS INTEGER) * 1000),
    ('creature_sea_turtle', CAST(strftime('%s','now') AS INTEGER) * 1000 - 550000, 'BEYOND_BLUE', 'demo-turtle', CAST(strftime('%s','now') AS INTEGER) * 1000),
    ('focus_seahorse', CAST(strftime('%s','now') AS INTEGER) * 1000 - 500000, 'FLOW_EARNED', 'demo-seahorse', CAST(strftime('%s','now') AS INTEGER) * 1000),
    ('focus_manta', CAST(strftime('%s','now') AS INTEGER) * 1000 - 450000, 'FLOW_EARNED', 'demo-manta', CAST(strftime('%s','now') AS INTEGER) * 1000),
    ('focus_whale', CAST(strftime('%s','now') AS INTEGER) * 1000 - 400000, 'FLOW_EARNED', 'demo-whale', CAST(strftime('%s','now') AS INTEGER) * 1000),
    ('stillwater_shrimp', CAST(strftime('%s','now') AS INTEGER) * 1000 - 350000, 'STILLWATER', 'demo-shrimp', CAST(strftime('%s','now') AS INTEGER) * 1000);

INSERT OR REPLACE INTO user_badge
    (badgeId, count, firstEarnedAt, lastEarnedAt, isNew, viewedAt, timestampConfidence)
VALUES
    ('badge_flow_10_min', 8, CAST(strftime('%s','now') AS INTEGER) * 1000 - 900000, CAST(strftime('%s','now') AS INTEGER) * 1000 - 400000, 0, CAST(strftime('%s','now') AS INTEGER) * 1000, 'EXACT'),
    ('badge_flow_30_min', 5, CAST(strftime('%s','now') AS INTEGER) * 1000 - 850000, CAST(strftime('%s','now') AS INTEGER) * 1000 - 300000, 0, CAST(strftime('%s','now') AS INTEGER) * 1000, 'EXACT'),
    ('badge_flow_60_min', 2, CAST(strftime('%s','now') AS INTEGER) * 1000 - 800000, CAST(strftime('%s','now') AS INTEGER) * 1000 - 200000, 0, CAST(strftime('%s','now') AS INTEGER) * 1000, 'EXACT'),
    ('variety_collector', 7, CAST(strftime('%s','now') AS INTEGER) * 1000 - 750000, CAST(strftime('%s','now') AS INTEGER) * 1000 - 100000, 0, CAST(strftime('%s','now') AS INTEGER) * 1000, 'EXACT'),
    ('stillwater_first_catch', 1, CAST(strftime('%s','now') AS INTEGER) * 1000 - 350000, CAST(strftime('%s','now') AS INTEGER) * 1000 - 350000, 0, CAST(strftime('%s','now') AS INTEGER) * 1000, 'EXACT');

INSERT OR REPLACE INTO badge_pin (badgeId, pinOrder, pinnedAt)
VALUES
    ('badge_flow_60_min', 0, CAST(strftime('%s','now') AS INTEGER) * 1000),
    ('variety_collector', 1, CAST(strftime('%s','now') AS INTEGER) * 1000),
    ('stillwater_first_catch', 2, CAST(strftime('%s','now') AS INTEGER) * 1000);

INSERT OR REPLACE INTO badge_tracking (badgeId, trackedAt)
VALUES ('badge_flow_120_min', CAST(strftime('%s','now') AS INTEGER) * 1000);

COMMIT;
