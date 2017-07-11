package dcapture.data.postgres;

import dcapture.data.core.*;
import dcapture.data.htwo.H2SelectBuilder;
import org.postgresql.ds.PGConnectionPoolDataSource;

import javax.persistence.TemporalType;
import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * H2 Processor
 */
public final class PostgresProcessor extends SqlFactory {
    private PGConnectionPoolDataSource connectionPool;
    private PostgresReader reader;
    private PostgresTransaction transaction;

    public void initialize(File persistenceFile, PGConnectionPoolDataSource pool, String schema) {
        initialize(schema, persistenceFile);
        this.connectionPool = pool;
        reader = new PostgresReader(this);
        transaction = new PostgresTransaction(this);
    }

    PGConnectionPoolDataSource getConnectionPool() throws SQLException {
        return connectionPool;
    }

    public SqlModifyBuilder createQueryBuilder() {
        return new PostgresModifyBuilder(this);
    }

    @Override
    public SqlTable getSqlTable(Class<?> tableClass) {
        return getSqlTableMap().getSqlTable(tableClass);
    }

    @Override
    public SqlReader getSqlReader() {
        return reader;
    }

    @Override
    public SqlSelectBuilder createSqlSelectBuilder() {
        return new H2SelectBuilder(this);
    }

    @Override
    public SqlColumn getReferenceSqlColumn(Class<?> tableClass, String column) {
        SqlTable sqlTable = getSqlTable(tableClass);
        column = column.toLowerCase();
        for (SqlColumn sqlColumn : sqlTable.getSqlColumnList()) {
            if (column.equals(sqlColumn.getName().toLowerCase())) {
                return sqlColumn;
            }
        }
        return null;
    }

    @Override
    public List<SqlReference> getSqlReference(Class<?> entityClass) {
        return null;
    }

    @Override
    public SqlQuery createSchemaQuery() {
        SqlQuery query = new SqlQuery();
        String builder = "create schema if not exists ".concat(getSchema()).concat(" authorization ")
                .concat(connectionPool.getUser()).concat(";");
        query.setQuery(builder);
        return query;
    }

    @Override
    public List<SqlQuery> createTableQueries() {
        List<SqlQuery> queryList = new ArrayList<>();
        for (SqlTable table : getSqlTableMap().getSqlTableList()) {
            SqlQuery sqlQuery = new SqlQuery();
            sqlQuery.setQuery(createTableQuery(table));
            queryList.add(sqlQuery);
        }
        return queryList;
    }

    @Override
    public List<SqlQuery> alterTableQueries() {
        List<SqlQuery> queryList = new ArrayList<>();
        for (SqlTable table : getSqlTableMap().getSqlTableList()) {
            List<String> alterList = alterTableQuery(table);
            for (String alter : alterList) {
                SqlQuery query = new SqlQuery();
                query.setQuery(alter);
                queryList.add(query);
            }
        }
        return queryList;
    }

    @Override
    public SqlTransaction getSqlTransaction() {
        return transaction;
    }

    private String createTableQuery(SqlTable sqlTable) {
        String table = sqlTable.getName();
        SqlColumn primaryColumn = sqlTable.getPrimaryColumn();
        List<SqlColumn> columnList = transaction.getColumnListWithoutPK(sqlTable);
        StringBuilder builder = new StringBuilder("create table if not exists ");
        builder.append(getSchema()).append('.').append(table).append("(");
        builder.append(primaryColumn.getName()).append(getPrimaryType(primaryColumn)).append(" , ");
        for (SqlColumn column : columnList) {
            builder.append(column.getName()).append(" ").append(getDataType(column)).append(", ");
        }
        builder.replace(builder.length() - 2, builder.length(), " ");
        builder.append(");");
        return builder.toString();
    }

    private List<String> alterTableQuery(SqlTable sqlTable) {
        List<String> referenceList = new ArrayList<>();
        for (SqlColumn column : sqlTable.getSqlColumnList()) {
            if (column.getJoinTable() != null) {
                StringBuilder builder = new StringBuilder("alter table ");
                builder.append(getSchema()).append('.').append(sqlTable.getName()).append(" add foreign key ");
                builder.append("(").append(column.getName()).append(") ");
                builder.append(" references ");
                SqlTable joinTable = column.getJoinTable();
                builder.append(getSchema()).append(".").append(joinTable.getName()).append("(")
                        .append(joinTable.getPrimaryColumn().getName()).append(");");
                referenceList.add(builder.toString());
            }
        }
        return referenceList;
    }

    private int getMaxTextLength() {
        return 516;
    }

    private int getEnumLength() {
        return 16;
    }

    private String getPrimaryType(final SqlColumn column) {
        if (String.class.equals(column.getType())) {
            return " varchar(" + column.getLength() + ")  primary key ";
        } else if (int.class.equals(column.getType())) {
            return " serial primary key ";
        }
        return " bigserial primary key ";
    }

    private String getDataType(final SqlColumn column) {
        final Class<?> type = column.getType();
        if (String.class.equals(type)) {
            String suffix = column.isNullable() ? "" : " not null";
            if (getMaxTextLength() < column.getLength()) {
                return "text".concat(suffix);
            }
            return "varchar(" + column.getLength() + ")" + suffix;
        } else if (LocalDate.class.equals(type)) {
            return "date";
        } else if (LocalDateTime.class.equals(type)) {
            return "timestamp";
        } else if (BigDecimal.class.equals(type) || Double.class.equals(type) || double.class.equals(type)) {
            return "decimal";
        } else if (int.class.equals(type)) {
            return "integer";
        } else if (boolean.class.equals(type)) {
            return "boolean";
        } else if (Enum.class.isAssignableFrom(type)) {
            return "varchar(" + getEnumLength() + ")";
        } else if (long.class.equals(type)) {
            return "bigint";
        } else if (Short.class.equals(type)) {
            return "smallint";
        } else if (Byte.class.equals(type)) {
            return "binary";
        } else if (Integer.class.equals(type)) {
            return "integer";
        } else if (Boolean.class.equals(type)) {
            return "boolean";
        } else if (Long.class.equals(type)) {
            return "bigint";
        } else if (Date.class.equals(type)) {
            if (column.getTemporalType() != null && TemporalType.TIMESTAMP.equals(column.getTemporalType())) {
                return "timestamp";
            }
            return "date";
        }
        throw new IllegalArgumentException("Unknown data type " + column.getName());
    }
}