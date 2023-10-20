-- IDEMPIERE-5662 Replenish Report incl. Production -> not working
SELECT register_migration_script('202304102245_IDEMPIERE-5662.sql') FROM dual;

SET SQLBLANKLINES ON
SET DEFINE OFF

-- Apr 10, 2023, 10:45:55 PM CEST
UPDATE AD_Process SET IsActive='N',Updated=TO_TIMESTAMP('2023-04-10 22:45:55','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Process_ID=125
;

-- Apr 10, 2023, 10:45:55 PM CEST
UPDATE AD_Menu SET Name='Replenish Report', Description='Inventory Replenish Report', IsActive='N',Updated=TO_TIMESTAMP('2023-04-10 22:45:55','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Menu_ID=196
;

-- Apr 10, 2023, 10:47:03 PM CEST
UPDATE AD_Menu SET Name='Replenish Report',Updated=TO_TIMESTAMP('2023-04-10 22:47:03','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Menu_ID=53354
;

-- Apr 10, 2023, 10:48:53 PM CEST
UPDATE AD_TreeNodeMM SET Parent_ID=53296, SeqNo=9,Updated=TO_TIMESTAMP('2023-04-10 22:48:53','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Tree_ID=10 AND Node_ID=228
;

-- Apr 10, 2023, 10:48:53 PM CEST
UPDATE AD_TreeNodeMM SET Parent_ID=53296, SeqNo=10,Updated=TO_TIMESTAMP('2023-04-10 22:48:53','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Tree_ID=10 AND Node_ID=53297
;

-- Apr 10, 2023, 10:48:53 PM CEST
UPDATE AD_TreeNodeMM SET Parent_ID=183, SeqNo=13,Updated=TO_TIMESTAMP('2023-04-10 22:48:53','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Tree_ID=10 AND Node_ID=53354
;

-- Apr 10, 2023, 10:48:53 PM CEST
UPDATE AD_TreeNodeMM SET Parent_ID=183, SeqNo=14,Updated=TO_TIMESTAMP('2023-04-10 22:48:53','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Tree_ID=10 AND Node_ID=479
;

-- Apr 10, 2023, 10:48:53 PM CEST
UPDATE AD_TreeNodeMM SET Parent_ID=183, SeqNo=15,Updated=TO_TIMESTAMP('2023-04-10 22:48:53','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Tree_ID=10 AND Node_ID=482
;

-- Apr 10, 2023, 10:48:53 PM CEST
UPDATE AD_TreeNodeMM SET Parent_ID=183, SeqNo=16,Updated=TO_TIMESTAMP('2023-04-10 22:48:53','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Tree_ID=10 AND Node_ID=481
;

-- Apr 10, 2023, 10:48:53 PM CEST
UPDATE AD_TreeNodeMM SET Parent_ID=183, SeqNo=17,Updated=TO_TIMESTAMP('2023-04-10 22:48:53','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Tree_ID=10 AND Node_ID=411
;

-- Apr 10, 2023, 10:48:53 PM CEST
UPDATE AD_TreeNodeMM SET Parent_ID=183, SeqNo=18,Updated=TO_TIMESTAMP('2023-04-10 22:48:53','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Tree_ID=10 AND Node_ID=53253
;

-- Apr 10, 2023, 10:48:53 PM CEST
UPDATE AD_TreeNodeMM SET Parent_ID=183, SeqNo=19,Updated=TO_TIMESTAMP('2023-04-10 22:48:53','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Tree_ID=10 AND Node_ID=426
;

-- Apr 10, 2023, 10:48:53 PM CEST
UPDATE AD_TreeNodeMM SET Parent_ID=183, SeqNo=20,Updated=TO_TIMESTAMP('2023-04-10 22:48:53','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Tree_ID=10 AND Node_ID=537
;

-- Apr 10, 2023, 10:48:53 PM CEST
UPDATE AD_TreeNodeMM SET Parent_ID=183, SeqNo=21,Updated=TO_TIMESTAMP('2023-04-10 22:48:53','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Tree_ID=10 AND Node_ID=200166
;

-- Apr 10, 2023, 10:48:53 PM CEST
UPDATE AD_TreeNodeMM SET Parent_ID=183, SeqNo=22,Updated=TO_TIMESTAMP('2023-04-10 22:48:53','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Tree_ID=10 AND Node_ID=311
;

-- Apr 10, 2023, 10:48:53 PM CEST
UPDATE AD_TreeNodeMM SET Parent_ID=183, SeqNo=23,Updated=TO_TIMESTAMP('2023-04-10 22:48:53','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Tree_ID=10 AND Node_ID=292
;

-- Apr 10, 2023, 10:48:53 PM CEST
UPDATE AD_TreeNodeMM SET Parent_ID=183, SeqNo=24,Updated=TO_TIMESTAMP('2023-04-10 22:48:53','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Tree_ID=10 AND Node_ID=504
;

-- Apr 10, 2023, 10:48:53 PM CEST
UPDATE AD_TreeNodeMM SET Parent_ID=183, SeqNo=25,Updated=TO_TIMESTAMP('2023-04-10 22:48:53','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Tree_ID=10 AND Node_ID=515
;

-- Apr 10, 2023, 10:49:19 PM CEST
UPDATE AD_Process SET Name='Replenish Report (deprecated)',Updated=TO_TIMESTAMP('2023-04-10 22:49:19','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Process_ID=125
;

-- Apr 10, 2023, 10:49:19 PM CEST
UPDATE AD_Menu SET Name='Replenish Report (deprecated)', Description='Inventory Replenish Report', IsActive='N',Updated=TO_TIMESTAMP('2023-04-10 22:49:19','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Menu_ID=196
;

-- Apr 10, 2023, 10:49:47 PM CEST
UPDATE AD_Process SET Name='Replenish Report',Updated=TO_TIMESTAMP('2023-04-10 22:49:47','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Process_ID=53267
;

-- Apr 10, 2023, 10:50:36 PM CEST
UPDATE AD_Process_Para SET DefaultValue=NULL,Updated=TO_TIMESTAMP('2023-04-10 22:50:36','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Process_Para_ID=53526
;

-- Apr 10, 2023, 10:51:07 PM CEST
UPDATE AD_Ref_List SET Name='Requisition',Updated=TO_TIMESTAMP('2023-04-10 22:51:07','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Ref_List_ID=53711
;

-- Apr 10, 2023, 10:51:46 PM CEST
UPDATE AD_Ref_List SET IsActive='N',Updated=TO_TIMESTAMP('2023-04-10 22:51:46','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Ref_List_ID=53708
;

-- Apr 10, 2023, 11:36:16 PM CEST
UPDATE AD_Process_Para SET DisplayLogic='@ReplenishmentCreate@!'''' & @ReplenishmentCreate@!''PRD''', MandatoryLogic='@ReplenishmentCreate@!'''' & @ReplenishmentCreate@!''PRD''',Updated=TO_TIMESTAMP('2023-04-10 23:36:16','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Process_Para_ID=53528
;

