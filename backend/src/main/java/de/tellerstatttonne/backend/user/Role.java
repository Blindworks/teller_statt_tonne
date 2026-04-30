package de.tellerstatttonne.backend.user;

public enum Role {
    ADMINISTRATOR("Administrator"),
    BOTSCHAFTER("Botschafter"),
    RETTER("Retter"),
    NEW_MEMBER("Neues Mitglied");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
