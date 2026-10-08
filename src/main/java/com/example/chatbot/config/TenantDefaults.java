package com.example.chatbot.config;

/**
 * Phase 1 me ek hi organisation hai, phir bhi har chunk ke metadata me tenant_id aur bot_id
 * likhenge (SRS 4.7 Notes). Phase 2 me tenant aane par kisi document ko re-index nahi karna padega.
 */
public final class TenantDefaults {
    public static final String TENANT_ID = "default";
    public static final String BOT_ID = "default";

    private TenantDefaults() {}
}