package de.tellerstatttonne.backend.member;

public enum MemberRole {
    BOTSCHAFTER("Botschafter"),
    FOODSAVER("Foodsaver"),
    NEW_MEMBER("Neues Mitglied");

    private final String label;

    MemberRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
