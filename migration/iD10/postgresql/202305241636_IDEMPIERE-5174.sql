-- IDEMPIERE-5174 Disable System User
SELECT register_migration_script('202305241636_IDEMPIERE-5174.sql') FROM dual;

-- May 24, 2023, 4:36:01 PM CEST
UPDATE AD_Column SET ReadOnlyLogic=NULL,Updated=TO_TIMESTAMP('2023-05-24 16:36:01','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Column_ID=417
;

