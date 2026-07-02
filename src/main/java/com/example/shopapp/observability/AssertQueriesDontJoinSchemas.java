package com.example.shopapp.observability;

import com.p6spy.engine.common.PreparedStatementInformation;
import com.p6spy.engine.common.StatementInformation;
import com.p6spy.engine.event.SimpleJdbcEventListener;

import java.sql.SQLException;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Runtime guard (demo profile only) that fails fast when a single SQL statement joins tables
 * from two different module schemas — the database-level shape of a hidden cross-module coupling.
 *
 * Honest punchline: a single-table read from a foreign schema (the fulfillment module's
 * @Subselect "SELECT ... FROM catalog.products") references ONE schema, so it slips through.
 * That is intentional — schema-per-module + a read-only @Subselect is a deliberate, decoupled
 * choice (data-decoupling Level 3). What we forbid is a query that *joins* across owned schemas,
 * because that silently welds two modules together at the data layer.
 *
 * Detection is regex-based and deliberately scoped to a demo: it collects schema prefixes from
 * `schema.table` references, keeps only the schemas the modules own, and throws if more than one
 * distinct module schema appears in a single statement. It is not a production SQL parser.
 *
 * Engaged only under the demo profile, via the jdbc:p6spy: datasource URL declared in
 * application-demo.properties. In every other profile P6Spy is never initialised and this
 * listener never runs — tests included — so it cannot affect the normal build.
 */
public final class AssertQueriesDontJoinSchemas extends SimpleJdbcEventListener {

    /** The schemas the modules own. Everything else (public, pg_catalog, information_schema) is ignored. */
    private static final Set<String> MODULE_SCHEMAS = Set.of("catalog", "orders", "fulfillment", "fraud");

    /** Matches schema-qualified references such as  catalog.products  or  "orders"."orders". */
    private static final Pattern SCHEMA_QUALIFIED = Pattern.compile(
            "\"?([a-zA-Z_][a-zA-Z0-9_]*)\"?\\s*\\.\\s*\"?[a-zA-Z_][a-zA-Z0-9_]*\"?");

    // Hibernate executes prepared statements — the SQL lives on the statement info, not a parameter.
    @Override
    public void onAfterExecuteQuery(PreparedStatementInformation info, long timeNanos, SQLException e) {
        assertSingleSchema(info.getSql());
    }

    @Override
    public void onAfterExecuteUpdate(PreparedStatementInformation info, long timeNanos, int rowCount, SQLException e) {
        assertSingleSchema(info.getSql());
    }

    @Override
    public void onAfterExecute(PreparedStatementInformation info, long timeNanos, SQLException e) {
        assertSingleSchema(info.getSql());
    }

    // Plain (non-prepared) statements — DDL and SQL init scripts — carry the SQL as a parameter.
    @Override
    public void onAfterExecuteQuery(StatementInformation info, long timeNanos, String sql, SQLException e) {
        assertSingleSchema(sql);
    }

    @Override
    public void onAfterExecuteUpdate(StatementInformation info, long timeNanos, String sql, int rowCount, SQLException e) {
        assertSingleSchema(sql);
    }

    @Override
    public void onAfterExecute(StatementInformation info, long timeNanos, String sql, SQLException e) {
        assertSingleSchema(sql);
    }

    private void assertSingleSchema(String sql) {
        if (sql == null || sql.isBlank()) {
            return;
        }
        Set<String> schemas = SCHEMA_QUALIFIED.matcher(sql)
                .results()
                .map(match -> match.group(1).toLowerCase())
                .filter(MODULE_SCHEMAS::contains)
                .collect(Collectors.toSet());
        if (schemas.size() > 1) {
            throw new CrossSchemaJoinException(schemas, sql);
        }
    }

    /** Thrown when one statement references more than one module schema (a cross-schema join). */
    public static final class CrossSchemaJoinException extends RuntimeException {

        CrossSchemaJoinException(Set<String> schemas, String sql) {
            super(message(schemas, sql));
        }

        private static String message(Set<String> schemas, String sql) {
            String joined = schemas.stream()
                    .sorted()
                    .collect(Collectors.joining(", "));
            return "Cross-schema JOIN detected — one query touches module schemas " + joined
                    + ". That couples those modules at the data layer. Offending SQL: " + sql;
        }
    }
}
