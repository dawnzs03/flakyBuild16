alter table WEB_USER_SITE_INFO disable constraint WUSI_WUID_FK;
alter table WEB_USER_PERSONAL_INFO disable constraint PERSONAL_INFO_WEB_USER_ID_FK;
alter table WEB_USER_CONTACT_PERMISSION disable constraint CONTPERM_WBUSERID_FK;
alter table PXTSESSIONS disable constraint PXTSESSIONS_USER;
alter table RHNWEBCONTACTCHANGELOG disable constraint RHN_WCON_CL_WCON_ID_FK;
alter table RHNSAVEDSEARCH disable constraint RHN_SAVEDSEARCH_WCID_FK;
alter table RHNWEBCONTACTCHANGELOG disable constraint RHN_WCON_CL_WCON_FROM_ID_FK;
alter table RHNUSERSERVERPREFS disable constraint RHN_USERSERVERPREFS_UID_FK;
alter table RHNUSERSERVERPERMS disable constraint RHN_USPERMS_UID_FK;
alter table RHNUSERSERVERGROUPPERMS disable constraint RHN_USGP_USER_FK;
alter table RHNUSERINFOPANE disable constraint RHN_USR_INFO_PANE_UID_FK;
alter table RHNUSERINFO disable constraint RHN_USER_INFO_USER_FK;
alter table RHNUSERGROUPMEMBERS disable constraint RHN_UGMEMBERS_UID_FK;
alter table RHNUSERDEFAULTSYSTEMGROUPS disable constraint RHN_UDSG_UID_FK;
alter table RHNSSMOPERATION disable constraint RHN_SSMOP_USER_FK;
alter table RHNSET disable constraint RHN_SET_USER_FK;
alter table RHNREGTOKEN disable constraint RHN_REG_TOKEN_UID_FK;
alter table RHNGRAILCOMPONENTCHOICES disable constraint RHN_GRAIL_COMP_CH_USER_FK;
alter table RHNFILEDOWNLOAD disable constraint RHN_FILEDL_UID_FK;
alter table RHNEMAILADDRESSLOG disable constraint RHN_EADDRESSLOG_UID_FK;
alter table RHNEMAILADDRESS disable constraint RHN_EADDRESS_UID_FK;
alter table RHNCHANNELPERMISSION disable constraint RHN_CPERM_UID_FK;
alter table RHNAPPINSTALLSESSION disable constraint RHN_APPINST_SESSION_UID_FK;
alter table RHNKICKSTARTSESSION disable constraint RHN_KS_SESSION_SCHED_FK;
alter table RHNACTION disable constraint RHN_ACTION_SCHEDULER_FK;
alter table RHNSERVERLOCK disable constraint RHN_SERVER_LOCK_LID_FK;
alter table RHNSERVERCUSTOMDATAVALUE disable constraint RHN_SCDV_LMB_FK;
alter table RHNCUSTOMDATAKEY disable constraint RHN_CDATAKEY_LMB_FK;
alter table RHNSERVER disable constraint RHN_SERVER_CREATOR_FK;
alter table RHNSERVERNOTES disable constraint RHN_SERVERNOTES_CREATOR_FK;
alter table RHNSERVERGROUPNOTES disable constraint RHN_SERVERGRP_NOTE_CREATOR_FK;
alter table RHNSERVERCUSTOMDATAVALUE disable constraint RHN_SCDV_CB_FK;
alter table RHNCUSTOMDATAKEY disable constraint RHN_CDATAKEY_CB_FK;
alter table RHN_REDIRECTS disable constraint RHN_RDRCT_CNTCT_CONTACT_ID_FK;
alter table RHN_CONTACT_METHODS disable constraint RHN_CMETH_CONTACT_ID_FK;
alter table RHN_COMMAND_QUEUE_SESSIONS disable constraint RHN_CQSES_CNTCT_CONTACT_IDFK;
alter table RHNCONFIGREVISION disable constraint RHN_CONFREVISION_CID_FK;

alter table web_contact disable constraint WEB_CONTACT_PK;
drop index web_contact_id_oid_cust_luc;
alter table web_contact enable constraint WEB_CONTACT_PK;

alter table WEB_USER_SITE_INFO enable novalidate constraint WUSI_WUID_FK;
alter table WEB_USER_PERSONAL_INFO enable novalidate constraint PERSONAL_INFO_WEB_USER_ID_FK;
alter table WEB_USER_CONTACT_PERMISSION enable novalidate constraint CONTPERM_WBUSERID_FK;
alter table PXTSESSIONS enable novalidate constraint PXTSESSIONS_USER;
alter table RHNWEBCONTACTCHANGELOG enable novalidate constraint RHN_WCON_CL_WCON_ID_FK;
alter table RHNSAVEDSEARCH enable novalidate constraint RHN_SAVEDSEARCH_WCID_FK;
alter table RHNWEBCONTACTCHANGELOG enable novalidate constraint RHN_WCON_CL_WCON_FROM_ID_FK;
alter table RHNUSERSERVERPREFS enable novalidate constraint RHN_USERSERVERPREFS_UID_FK;
alter table RHNUSERSERVERPERMS enable novalidate constraint RHN_USPERMS_UID_FK;
alter table RHNUSERSERVERGROUPPERMS enable novalidate constraint RHN_USGP_USER_FK;
alter table RHNUSERINFOPANE enable novalidate constraint RHN_USR_INFO_PANE_UID_FK;
alter table RHNUSERINFO enable novalidate constraint RHN_USER_INFO_USER_FK;
alter table RHNUSERGROUPMEMBERS enable novalidate constraint RHN_UGMEMBERS_UID_FK;
alter table RHNUSERDEFAULTSYSTEMGROUPS enable novalidate constraint RHN_UDSG_UID_FK;
alter table RHNSSMOPERATION enable novalidate constraint RHN_SSMOP_USER_FK;
alter table RHNSET enable novalidate constraint RHN_SET_USER_FK;
alter table RHNREGTOKEN enable novalidate constraint RHN_REG_TOKEN_UID_FK;
alter table RHNGRAILCOMPONENTCHOICES enable novalidate constraint RHN_GRAIL_COMP_CH_USER_FK;
alter table RHNFILEDOWNLOAD enable novalidate constraint RHN_FILEDL_UID_FK;
alter table RHNEMAILADDRESSLOG enable novalidate constraint RHN_EADDRESSLOG_UID_FK;
alter table RHNEMAILADDRESS enable novalidate constraint RHN_EADDRESS_UID_FK;
alter table RHNCHANNELPERMISSION enable novalidate constraint RHN_CPERM_UID_FK;
alter table RHNAPPINSTALLSESSION enable novalidate constraint RHN_APPINST_SESSION_UID_FK;
alter table RHNKICKSTARTSESSION enable novalidate constraint RHN_KS_SESSION_SCHED_FK;
alter table RHNACTION enable novalidate constraint RHN_ACTION_SCHEDULER_FK;
alter table RHNSERVERLOCK enable novalidate constraint RHN_SERVER_LOCK_LID_FK;
alter table RHNSERVERCUSTOMDATAVALUE enable novalidate constraint RHN_SCDV_LMB_FK;
alter table RHNCUSTOMDATAKEY enable novalidate constraint RHN_CDATAKEY_LMB_FK;
alter table RHNSERVER enable novalidate constraint RHN_SERVER_CREATOR_FK;
alter table RHNSERVERNOTES enable novalidate constraint RHN_SERVERNOTES_CREATOR_FK;
alter table RHNSERVERGROUPNOTES enable novalidate constraint RHN_SERVERGRP_NOTE_CREATOR_FK;
alter table RHNSERVERCUSTOMDATAVALUE enable novalidate constraint RHN_SCDV_CB_FK;
alter table RHNCUSTOMDATAKEY enable novalidate constraint RHN_CDATAKEY_CB_FK;
alter table RHN_REDIRECTS enable novalidate constraint RHN_RDRCT_CNTCT_CONTACT_ID_FK;
alter table RHN_CONTACT_METHODS enable novalidate constraint RHN_CMETH_CONTACT_ID_FK;
alter table RHN_COMMAND_QUEUE_SESSIONS enable novalidate constraint RHN_CQSES_CNTCT_CONTACT_IDFK;
alter table RHNCONFIGREVISION enable novalidate constraint RHN_CONFREVISION_CID_FK;
