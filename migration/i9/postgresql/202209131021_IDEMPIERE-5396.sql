-- IDEMPIERE-5396 Replace CreateFrom form with Info Window Process
SELECT register_migration_script('202209131021_IDEMPIERE-5396.sql') FROM dual;

-- Sep 13, 2022, 10:21:44 AM SGT
UPDATE AD_Process SET ShowHelp='S',Updated=TO_TIMESTAMP('2022-09-13 10:21:44','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Process_ID=200143
;

-- Sep 13, 2022, 10:21:52 AM SGT
UPDATE AD_Process SET ShowHelp='S',Updated=TO_TIMESTAMP('2022-09-13 10:21:52','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Process_ID=200141
;

-- Sep 13, 2022, 10:22:03 AM SGT
UPDATE AD_Process SET ShowHelp='S',Updated=TO_TIMESTAMP('2022-09-13 10:22:03','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Process_ID=200142
;

