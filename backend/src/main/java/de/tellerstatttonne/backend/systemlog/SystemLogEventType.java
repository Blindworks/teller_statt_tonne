package de.tellerstatttonne.backend.systemlog;

public enum SystemLogEventType {

    // --- AUTH ---
    LOGIN_SUCCESS(SystemLogCategory.AUTH, SystemLogSeverity.INFO),
    LOGIN_FAILED(SystemLogCategory.AUTH, SystemLogSeverity.WARN),
    LOGOUT(SystemLogCategory.AUTH, SystemLogSeverity.INFO),
    PASSWORD_RESET_REQUESTED(SystemLogCategory.AUTH, SystemLogSeverity.INFO),
    PASSWORD_RESET_COMPLETED(SystemLogCategory.AUTH, SystemLogSeverity.INFO),
    PASSWORD_CHANGED(SystemLogCategory.AUTH, SystemLogSeverity.INFO),

    // --- USER_MGMT ---
    USER_CREATED(SystemLogCategory.USER_MGMT, SystemLogSeverity.INFO),
    USER_UPDATED(SystemLogCategory.USER_MGMT, SystemLogSeverity.INFO),
    USER_DELETED(SystemLogCategory.USER_MGMT, SystemLogSeverity.WARN),
    USER_ROLES_CHANGED(SystemLogCategory.USER_MGMT, SystemLogSeverity.INFO),
    USER_STATUS_CHANGED(SystemLogCategory.USER_MGMT, SystemLogSeverity.INFO),
    USER_INVITATION_SENT(SystemLogCategory.USER_MGMT, SystemLogSeverity.INFO),
    ROLE_CREATED(SystemLogCategory.USER_MGMT, SystemLogSeverity.INFO),
    ROLE_UPDATED(SystemLogCategory.USER_MGMT, SystemLogSeverity.INFO),
    ROLE_DELETED(SystemLogCategory.USER_MGMT, SystemLogSeverity.WARN),

    // --- ADMIN_ACTION ---
    HYGIENE_CERTIFICATE_APPROVED(SystemLogCategory.ADMIN_ACTION, SystemLogSeverity.INFO),
    HYGIENE_CERTIFICATE_REJECTED(SystemLogCategory.ADMIN_ACTION, SystemLogSeverity.INFO),
    PARTNER_APPLICATION_APPROVED(SystemLogCategory.ADMIN_ACTION, SystemLogSeverity.INFO),
    PARTNER_APPLICATION_REJECTED(SystemLogCategory.ADMIN_ACTION, SystemLogSeverity.INFO),
    STORE_DELETED(SystemLogCategory.ADMIN_ACTION, SystemLogSeverity.WARN),
    STORE_RESTORED(SystemLogCategory.ADMIN_ACTION, SystemLogSeverity.INFO),
    STORE_MEMBER_ASSIGNED(SystemLogCategory.ADMIN_ACTION, SystemLogSeverity.INFO),
    STORE_COORDINATOR_ASSIGNED(SystemLogCategory.ADMIN_ACTION, SystemLogSeverity.INFO),
    STORE_COORDINATOR_UNASSIGNED(SystemLogCategory.ADMIN_ACTION, SystemLogSeverity.INFO),
    DISTRIBUTION_POINT_CREATED(SystemLogCategory.ADMIN_ACTION, SystemLogSeverity.INFO),
    DISTRIBUTION_POINT_UPDATED(SystemLogCategory.ADMIN_ACTION, SystemLogSeverity.INFO),
    DISTRIBUTION_POINT_DELETED(SystemLogCategory.ADMIN_ACTION, SystemLogSeverity.WARN),
    SPECIAL_PICKUP_CREATED(SystemLogCategory.ADMIN_ACTION, SystemLogSeverity.INFO),
    SPECIAL_PICKUP_UPDATED(SystemLogCategory.ADMIN_ACTION, SystemLogSeverity.INFO),
    SPECIAL_PICKUP_DELETED(SystemLogCategory.ADMIN_ACTION, SystemLogSeverity.WARN),

    // --- SYSTEM ---
    UNHANDLED_EXCEPTION(SystemLogCategory.SYSTEM, SystemLogSeverity.ERROR),
    MAIL_DELIVERY_FAILED(SystemLogCategory.SYSTEM, SystemLogSeverity.ERROR);

    private final SystemLogCategory category;
    private final SystemLogSeverity defaultSeverity;

    SystemLogEventType(SystemLogCategory category, SystemLogSeverity defaultSeverity) {
        this.category = category;
        this.defaultSeverity = defaultSeverity;
    }

    public SystemLogCategory getCategory() {
        return category;
    }

    public SystemLogSeverity getDefaultSeverity() {
        return defaultSeverity;
    }
}
