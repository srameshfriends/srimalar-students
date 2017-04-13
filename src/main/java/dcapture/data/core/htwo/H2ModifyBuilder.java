package dcapture.data.core.htwo;

import dcapture.data.core.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * H2 Modify Builder
 */
public class H2ModifyBuilder implements SqlModifyBuilder {
    private final SqlProcessor sqlProcessor;
    private String updateTable, deleteTable, insertTable;
    private List<String> updateColumns, insertColumns;
    private WhereQuery whereQuery;
    private List<Object> updateParameters, insertParameters;
    private StringBuilder joinBuilder;

    H2ModifyBuilder(SqlProcessor sqlProcessor) {
        this.sqlProcessor = sqlProcessor;
    }

    @Override
    public String getSchema() {
        return sqlProcessor.getSchema();
    }

    @Override
    public H2ModifyBuilder updateColumn(String column, Object object) {
        getUpdateColumns().add(column);
        getUpdateParameters().add(object);
        return H2ModifyBuilder.this;
    }

    @Override
    public H2ModifyBuilder update(String tableName) {
        this.updateTable = tableName;
        return H2ModifyBuilder.this;
    }

    @Override
    public SqlModifyBuilder update(Class<?> tableClass) {
        SqlTable sqlTable = sqlProcessor.getSqlTable(tableClass);
        return deleteFrom(sqlTable.getName());
    }

    @Override
    public H2ModifyBuilder deleteFrom(String table) {
        deleteTable = table;
        return H2ModifyBuilder.this;
    }

    @Override
    public SqlModifyBuilder deleteFrom(Class<?> tableClass) {
        SqlTable sqlTable = sqlProcessor.getSqlTable(tableClass);
        return deleteFrom(sqlTable.getName());
    }

    @Override
    public H2ModifyBuilder insertInto(String tableName) {
        this.insertTable = tableName;
        return H2ModifyBuilder.this;
    }

    @Override
    public H2ModifyBuilder insertColumns(String column, Object object) {
        getInsertColumns().add(column);
        getInsertParameters().add(object);
        return H2ModifyBuilder.this;
    }

    @Override
    public H2ModifyBuilder join(String joinQuery) {
        getJoinBuilder().append(joinQuery);
        return H2ModifyBuilder.this;
    }

    @Override
    public H2ModifyBuilder where(WhereQuery whereQuery) {
        this.whereQuery = whereQuery;
        return H2ModifyBuilder.this;
    }

    @Override
    public SqlQuery getSqlQuery() {
        SqlQuery sqlQuery = new SqlQuery();
        if (updateTable != null) {
            sqlQuery = buildUpdateQuery();
        } else if (insertTable != null) {
            sqlQuery = buildInsertQuery();
        } else if (deleteTable != null) {
            sqlQuery = buildDeleteQuery();
        }
        return sqlQuery;
    }

    private String buildJoinSQ() {
        StringBuilder sb = new StringBuilder(" ");
        if (joinBuilder != null) {
            sb.append(" ").append(joinBuilder.toString());
        }
        return sb.toString();
    }

    private String buildWhereSQ(LinkedList<Object> parameters) {
        if (whereQuery != null) {
            parameters.addAll(whereQuery.getParameterList());
            return " where " + whereQuery.toString();
        }
        return "";
    }

    private SqlQuery buildUpdateQuery() {
        LinkedList<Object> parameters = new LinkedList<>();
        parameters.addAll(getUpdateParameters());
        StringBuilder sb = new StringBuilder();
        sb.append("update ").append(getSchema()).append(".").append(updateTable).append(" set ");
        for (String upd : updateColumns) {
            sb.append(upd).append(" = ?,");
        }
        sb.replace(sb.toString().length() - 1, sb.toString().length(), " ");
        if (joinBuilder != null) {
            sb.append(" ").append(joinBuilder.toString());
        }
        if (whereQuery != null) {
            sb.append(" where ").append(whereQuery.toString());
            parameters.addAll(whereQuery.getParameterList());
        }
        sb.append(";");
        SqlQuery sqlQuery = new SqlQuery(sb.toString());
        sqlQuery.setParameterList(parameters);
        return sqlQuery;
    }

    private SqlQuery buildInsertQuery() {
        LinkedList<Object> parameters = new LinkedList<>();
        parameters.addAll(getInsertParameters());
        StringBuilder sb = new StringBuilder();
        sb.append("insert into ").append(getSchema()).append(".").append(insertTable).append(" (");
        StringBuilder pss = new StringBuilder("(");
        for (String ic : insertColumns) {
            sb.append(ic).append(",");
            pss.append("?,");
        }
        sb.replace(sb.toString().length() - 1, sb.toString().length(), ")");
        pss.replace(pss.toString().length() - 1, pss.toString().length(), ")");
        sb.append(" values ").append(pss.toString()).append(";");
        SqlQuery sqlQuery = new SqlQuery(sb.toString());
        sqlQuery.setParameterList(parameters);
        return sqlQuery;
    }

    private SqlQuery buildDeleteQuery() {
        LinkedList<Object> parameters = new LinkedList<>();
        StringBuilder sb = new StringBuilder();
        sb.append("delete from ").append(getSchema()).append(".").append(deleteTable);
        if (joinBuilder != null) {
            sb.append(" ").append(joinBuilder.toString());
        }
        if (whereQuery != null) {
            sb.append(" where ").append(whereQuery.toString());
            parameters.addAll(whereQuery.getParameterList());
        }
        sb.append(";");
        SqlQuery sqlQuery = new SqlQuery(sb.toString());
        sqlQuery.setParameterList(parameters);
        return sqlQuery;
    }

    private List<String> getUpdateColumns() {
        if (updateColumns == null) {
            updateColumns = new ArrayList<>();
        }
        return updateColumns;
    }

    private List<Object> getUpdateParameters() {
        if (updateParameters == null) {
            updateParameters = new ArrayList<>();
        }
        return updateParameters;
    }

    private List<String> getInsertColumns() {
        if (insertColumns == null) {
            insertColumns = new ArrayList<>();
        }
        return insertColumns;
    }

    private List<Object> getInsertParameters() {
        if (insertParameters == null) {
            insertParameters = new ArrayList<>();
        }
        return insertParameters;
    }

    private StringBuilder getJoinBuilder() {
        if (joinBuilder == null) {
            joinBuilder = new StringBuilder();
        }
        return joinBuilder;
    }

    private WhereQuery getWhereQuery() {
        if (whereQuery == null) {
            whereQuery = new WhereQuery();
        }
        return whereQuery;
    }
}
