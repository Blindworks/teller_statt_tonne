package de.tellerstatttonne.backend.dbmigration;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * H2-only: drops every UNIQUE constraint on hygiene_certificate(user_id) regardless
 * of the (possibly auto-generated) constraint name. We need this because the original
 * constraint was created via column-inline syntax and H2 sometimes uses an internal
 * name like UQ_..._INDEX_6 which dropUniqueConstraint cannot target.
 */
public class DropHygieneUserUniqueChange implements CustomTaskChange {

    @Override
    public void execute(Database database) throws CustomChangeException {
        JdbcConnection conn = (JdbcConnection) database.getConnection();
        try (Statement stmt = conn.createStatement()) {
            // Schritt 1: FK auf user_id ablegen (Namen aus 015-Migration), damit der
            //            zugrundeliegende Unique-Index freigegeben wird.
            try (Statement drop = conn.createStatement()) {
                drop.execute("ALTER TABLE hygiene_certificate DROP CONSTRAINT IF EXISTS fk_hygiene_certificate_user");
            }
            try (Statement drop = conn.createStatement()) {
                drop.execute("ALTER TABLE hygiene_certificate DROP CONSTRAINT IF EXISTS FK_HYGIENE_CERTIFICATE_USER");
            }

            // Schritt 2: UNIQUE-Constraints ablegen.
            List<String> uniqueNames = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery(
                "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                "WHERE UPPER(TABLE_NAME) = 'HYGIENE_CERTIFICATE' AND CONSTRAINT_TYPE = 'UNIQUE'")) {
                while (rs.next()) {
                    uniqueNames.add(rs.getString(1));
                }
            }
            for (String name : uniqueNames) {
                try (Statement drop = conn.createStatement()) {
                    drop.execute("ALTER TABLE hygiene_certificate DROP CONSTRAINT IF EXISTS \"" + name + "\"");
                }
            }

            // Schritt 3: ggf. verwaiste UNIQUE-Indizes auf user_id loeschen.
            List<String> indexNames = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery(
                "SELECT DISTINCT i.INDEX_NAME " +
                "FROM INFORMATION_SCHEMA.INDEXES i " +
                "JOIN INFORMATION_SCHEMA.INDEX_COLUMNS ic " +
                "  ON ic.INDEX_NAME = i.INDEX_NAME AND ic.INDEX_SCHEMA = i.INDEX_SCHEMA " +
                "WHERE UPPER(i.TABLE_NAME) = 'HYGIENE_CERTIFICATE' " +
                "  AND i.INDEX_TYPE_NAME LIKE '%UNIQUE%' " +
                "  AND UPPER(ic.COLUMN_NAME) = 'USER_ID'")) {
                while (rs.next()) {
                    indexNames.add(rs.getString(1));
                }
            }
            for (String name : indexNames) {
                try (Statement drop = conn.createStatement()) {
                    drop.execute("DROP INDEX IF EXISTS \"" + name + "\"");
                }
            }

            // Schritt 4: FK wieder anlegen.
            try (Statement add = conn.createStatement()) {
                add.execute("ALTER TABLE hygiene_certificate " +
                    "ADD CONSTRAINT fk_hygiene_certificate_user " +
                    "FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE");
            }
        } catch (Exception e) {
            throw new CustomChangeException("Konnte UNIQUE-Constraint auf hygiene_certificate(user_id) nicht entfernen", e);
        }
    }

    @Override public String getConfirmationMessage() { return "UNIQUE auf hygiene_certificate(user_id) entfernt"; }
    @Override public void setUp() throws SetupException { }
    @Override public void setFileOpener(ResourceAccessor resourceAccessor) { }
    @Override public ValidationErrors validate(Database database) { return new ValidationErrors(); }
}
