-- IDEMPIERE-5487
SELECT register_migration_script('202211230918_IDEMPIERE-5487.sql') FROM dual;

-- Nov 23, 2022, 9:18:11 AM CET
UPDATE AD_Val_Rule SET Code='AD_Column.AD_Table_ID=@AD_Table_ID@ AND AD_Column.AD_Column_ID NOT IN (SELECT AD_Column_ID FROM WS_WebServiceFieldInput WHERE WS_WebServiceType_ID = @WS_WebServiceType_ID@ AND AD_Column_ID IS NOT NULL)',Updated=TO_TIMESTAMP('2022-11-23 09:18:11','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Val_Rule_ID=200138
;

