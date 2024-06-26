// ============================================================================
//
// Copyright (C) 2006-2021 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.core.model.metadata.builder.database.manager.dbs;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.ICoreService;
import org.talend.core.database.EDatabaseTypeName;
import org.talend.core.model.metadata.IMetadataConnection;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.metadata.MappingTypeRetriever;
import org.talend.core.model.metadata.MetadataTalendType;
import org.talend.core.model.metadata.MetadataToolHelper;
import org.talend.core.model.metadata.builder.connection.DatabaseConnection;
import org.talend.core.model.metadata.builder.connection.MetadataColumn;
import org.talend.core.model.metadata.builder.database.ExtractMetaDataFromDataBase;
import org.talend.core.model.metadata.builder.database.ExtractMetaDataFromDataBase.ETableTypes;
import org.talend.core.model.metadata.builder.database.ExtractMetaDataUtils;
import org.talend.core.model.metadata.builder.database.TableInfoParameters;
import org.talend.core.model.metadata.builder.database.manager.ExtractManager;
import org.talend.cwm.helper.ColumnHelper;
import org.talend.cwm.helper.ConnectionHelper;
import org.talend.cwm.relational.TdColumn;
import org.talend.metadata.managment.utils.ManagementTextUtils;
import org.talend.utils.sql.metadata.constants.GetColumn;

import orgomg.cwm.resource.relational.NamedColumnSet;
import orgomg.cwm.resource.relational.Schema;

/**
 * DOC ggu class global comment. Detailled comment
 */
public class OracleExtractManager extends ExtractManager {

    public static final String DATABASE_PRODUCT_NAME = "Oracle";

    public static final String ORACLE_10G_RECBIN_SQL = "select object_name from recyclebin"; //$NON-NLS-1$

    public static final String GET_ALL_SYNONYMS = "select distinct synonym_name\n" + //$NON-NLS-1$
            "from all_synonyms, all_tab_columns\n" + //$NON-NLS-1$
            "where all_synonyms.table_name = all_tab_columns.table_name"; //$NON-NLS-1$

    private static Logger log = Logger.getLogger(OracleExtractManager.class);

    public OracleExtractManager(EDatabaseTypeName dbType) {
        super(dbType);
    }

    @Override
    protected List<String> getTablesToFilter(IMetadataConnection metadataConnection) {

        List<String> tablesToFilter = new ArrayList<String>();
        PreparedStatement stmt = null;
        ExtractMetaDataUtils extractMeta = ExtractMetaDataUtils.getInstance();
        try {
            if (EDatabaseTypeName.ORACLEFORSID.getDisplayName().equals(metadataConnection.getDbType())
                    || EDatabaseTypeName.ORACLESN.getDisplayName().equals(metadataConnection.getDbType())
                    || EDatabaseTypeName.ORACLE_CUSTOM.getDisplayName().equals(metadataConnection.getDbType())
                    || EDatabaseTypeName.ORACLE_OCI.getDisplayName().equals(metadataConnection.getDbType())) {
                stmt = extractMeta.getConn().prepareStatement(ORACLE_10G_RECBIN_SQL);
                extractMeta.setQueryStatementTimeout(stmt);
                ResultSet rsTables = stmt.executeQuery();
                tablesToFilter = ExtractMetaDataFromDataBase.getTableNamesFromQuery(rsTables, extractMeta.getConn());
                rsTables.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            ExceptionHandler.process(e);
        }
        return tablesToFilter;
    }

    @Override
    public List<IMetadataTable> extractTablesFromDB(DatabaseMetaData dbMetaData, IMetadataConnection metadataConnection,
            int... limit) {
        List<IMetadataTable> medataTables = new ArrayList<IMetadataTable>();
        ExtractMetaDataUtils extractMeta = ExtractMetaDataUtils.getInstance();
        if (dbMetaData == null || metadataConnection == null) {
            return medataTables;
        }
        if (extractMeta.isUseAllSynonyms()) { // seems only for oracle and can be use all synonyms
            List<String> tablesToFilter = getTablesToFilter(metadataConnection);

            try {
                PreparedStatement stmt = extractMeta.getConn().prepareStatement(GET_ALL_SYNONYMS);
                extractMeta.setQueryStatementTimeout(stmt);
                ResultSet rsTables = stmt.executeQuery();
                getMetadataTables(medataTables, rsTables, dbMetaData.supportsSchemasInTableDefinitions(), tablesToFilter, limit);
                rsTables.close();
                stmt.close();
            } catch (SQLException e) {
                ExceptionHandler.process(e);
                log.error(e.toString());
                throw new RuntimeException(e);
            } catch (Exception e) {
                ExceptionHandler.process(e);
                log.error(e.toString());
                throw new RuntimeException(e);
            }
            return medataTables;
        } else {
            return super.extractTablesFromDB(dbMetaData, metadataConnection, limit);
        }
    }

    @Override
    public String getTableNameBySynonyms(Connection conn, String tableName) {
        // bug TDI-19382
        PreparedStatement sta = null;
        ResultSet resultSet = null;

        try {
            if (conn != null && conn.getMetaData().getDatabaseProductName().equals(DATABASE_PRODUCT_NAME)) {
                String sql = "select TABLE_NAME from ALL_SYNONYMS where SYNONYM_NAME = ?"; //$NON-NLS-1$ //$NON-NLS-2$
                // String sql = "select * from all_tab_columns where upper(table_name)='" + name +
                // "' order by column_id";
                // Statement sta;
                sta = conn.prepareStatement(sql);
                sta.setString(1, tableName);

                ExtractMetaDataUtils.getInstance().setQueryStatementTimeout(sta);
                resultSet = sta.executeQuery();
                while (resultSet.next()) {
                    return resultSet.getString("TABLE_NAME"); //$NON-NLS-1$
                }
            }
        } catch (SQLException e) {
            log.error(e.toString());
            throw new RuntimeException(e);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (sta != null) {
                    sta.close();
                }
            } catch (SQLException e) {
                log.equals(e.toString());
            }
        }
        return null;

    }

    @Override
    protected void fillSynonyms(IMetadataConnection metadataConnection, List<TdColumn> metadataColumns, NamedColumnSet table,
            String tableName, String synonymName, DatabaseMetaData dbMetaData) throws SQLException {
        if (metadataConnection == null || dbMetaData == null) {
            return;
        }
        if (dbMetaData.getDatabaseProductName().equals(OracleExtractManager.DATABASE_PRODUCT_NAME)) {
            ExtractMetaDataUtils extractMeta = ExtractMetaDataUtils.getInstance();
            // need to retrieve columns of synonym by useing sql rather than get them from jdbc metadata
            String synSQL = "SELECT all_tab_columns.*\n" + "FROM all_tab_columns\n" + "LEFT OUTER JOIN all_synonyms\n"
                    + "ON all_tab_columns.TABLE_NAME = all_synonyms.TABLE_NAME\n"
                    + "AND ALL_SYNONYMS.TABLE_OWNER = all_tab_columns.OWNER\n" + "WHERE all_synonyms.SYNONYM_NAME  =?\n";
            // bug TDI-19382
            if (!("").equals(metadataConnection.getSchema())) {
                synSQL += "and all_synonyms.OWNER =?";
            } else if (table.eContainer() instanceof Schema) {
                synSQL += "and all_synonyms.OWNER =?";
            }
            
            //TUP-39239, caused not same ordering of the columns between table schema and synonym schema
//            synSQL += " ORDER BY all_tab_columns.COLUMN_NAME"; //$NON-NLS-1$
            PreparedStatement sta = extractMeta.getConn().prepareStatement(synSQL);
            sta.setString(1, synonymName);
            int idx = 2;
            if (!("").equals(metadataConnection.getSchema())) {
                sta.setString(idx, metadataConnection.getSchema());
            } else if (table.eContainer() instanceof Schema) {
                Schema schema = (Schema) table.eContainer();
                sta.setString(idx, schema.getName());
            }
            extractMeta.setQueryStatementTimeout(sta);
            ResultSet columns = sta.executeQuery();
            String typeName = null;
            int index = 0;
            List<String> columnLabels = new ArrayList<String>();
            try {
                while (columns.next()) {
                    String columnName = columns.getString(GetColumn.COLUMN_NAME.name());
                    TdColumn column = ColumnHelper.createTdColumn(columnName);

                    String label = column.getLabel();
                    label = ManagementTextUtils.filterSpecialChar(label);
                    String label2 = label;
                    final ICoreService coreService = getCoreService();
                    if (coreService != null && coreService.isKeyword(label)) {
                        label = "_" + label; //$NON-NLS-1$
                    }

                    label = MetadataToolHelper.validateColumnName(label, index, columnLabels);
                    column.setLabel(label);
                    column.setOriginalField(label2);

                    // no need for oracle
                    // if (!ExtractMetaDataUtils.needFakeDatabaseMetaData(metadataConnection.getDbType(),
                    // metadataConnection.isSqlMode())) {
                    // dataType = columns.getInt(GetColumn.DATA_TYPE.name());
                    typeName = columns.getString(GetColumn.DATA_TYPE.name());
                    // }
                    setLengthAndPrecision(column, columns, typeName);

                    DatabaseConnection dbConnection = (DatabaseConnection) ConnectionHelper.getConnection(table);
                    String dbmsId = dbConnection == null ? null : dbConnection.getDbmsId();
                    if (dbmsId != null) {
                        MappingTypeRetriever mappingTypeRetriever = MetadataTalendType.getMappingTypeRetriever(dbmsId);
                        String talendType = mappingTypeRetriever.getDefaultSelectedTalendType(typeName, (int) column.getLength(),
                                (int) column.getPrecision());
                        column.setTalendType(talendType);
                        String defaultSelectedDbType = MetadataTalendType.getMappingTypeRetriever(dbConnection.getDbmsId())
                                .getDefaultSelectedDbType(talendType);
                        column.setSourceType(defaultSelectedDbType);
                    }
                    try {
                        column.setNullable("Y".equals(columns.getString(GetColumn.NULLABLE.name()))); //$NON-NLS-1$
                    } catch (Exception e) {
                        log.error(e);
                    }
                    metadataColumns.add(column);
                    columnLabels.add(column.getLabel());
                    index++;

                }
            } finally {
                columns.close();
            }
        }

    }

    @Override
    protected ResultSet getColumnsResultSet(DatabaseMetaData dbMetaData, String catalogName, String schemaName, String tableName)
            throws SQLException {
        ExtractMetaDataUtils extractMeta = ExtractMetaDataUtils.getInstance();
        if (extractMeta.isUseAllSynonyms()) {
            String sql = "select * from all_tab_columns where table_name=? ORDER BY all_tab_columns.COLUMN_NAME"; //$NON-NLS-1$ //$NON-NLS-2$
            PreparedStatement stmt = extractMeta.getConn().prepareStatement(sql);
            stmt.setString(1, tableName);
            extractMeta.setQueryStatementTimeout(stmt);
            return stmt.executeQuery();
        } else {
            return super.getColumnsResultSet(dbMetaData, catalogName, schemaName, tableName);
        }
    }

    @Override
    public void synchroViewStructure(String catalogName, String schemaName, String tableName) throws SQLException {
        ExtractMetaDataUtils extractMeta = ExtractMetaDataUtils.getInstance();
        ResultSet results = null;
        PreparedStatement stmt = null;
        String sql = null;
        if (extractMeta.isUseAllSynonyms()) {
            sql = "select * from all_tab_columns where table_name=?"; //$NON-NLS-1$
        } else {
            StringBuffer sqlBuffer = new StringBuffer();
            sqlBuffer.append("SELECT * FROM ");
            if (!StringUtils.isEmpty(schemaName)) {
                sqlBuffer.append(schemaName).append(".");
            }
            sqlBuffer.append(tableName);
            sql = sqlBuffer.toString();
        }
        try {
            stmt = extractMeta.getConn().prepareStatement(sql);
            if (extractMeta.isUseAllSynonyms()) {
                stmt.setString(1, tableName);
            }
            extractMeta.setQueryStatementTimeout(stmt);
            results = stmt.executeQuery();
        } finally {
            if (results != null) {
                results.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    @Override
    protected void checkTypeForTimestamp(IMetadataConnection metadataConnection, TdColumn metadataColumn, String dbType) {
        // for bug 6919, oracle driver doesn't give correctly the length for timestamp
        if (dbType.equals("TIMESTAMP")) { //$NON-NLS-1$
            metadataColumn.setLength(metadataColumn.getPrecision());
            metadataColumn.setPrecision(-1);
        }
    }

    @Override
    protected void checkComments(IMetadataConnection metadataConnection, String tableName, List<TdColumn> metadataColumns) {
        ResultSet keys = null;
        PreparedStatement statement = null;
        ExtractMetaDataUtils extractMeta = ExtractMetaDataUtils.getInstance();
        try {
            statement = extractMeta.getConn().prepareStatement("SELECT COMMENTS FROM USER_COL_COMMENTS WHERE TABLE_NAME=?"); //$NON-NLS-1$
            statement.setString(1, tableName);
            extractMeta.setQueryStatementTimeout(statement);
            if (statement.execute()) {
                keys = statement.getResultSet();
                int i = 0;
                while (keys.next()) {
                    MetadataColumn metadataColumn = metadataColumns.get(i++);
                    metadataColumn.setComment(ManagementTextUtils.filterSpecialChar(keys.getString("COMMENTS"))); //$NON-NLS-1$
                }
            }

        } catch (Exception e) {
            log.error(e.toString());
        } finally {
            try {
                if (keys != null) {
                    keys.close();
                }
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                log.error(e.toString());
            }
        }
    }

    @Override
    protected void filterTablesFromRecycleBin(IMetadataConnection metadataConnection, List<String> itemTablesName) {
        // filter tables or viewer from the recyclebin in the Oracle 10g.
        if (metadataConnection.getDbVersionString() != null) {
            ExtractMetaDataUtils extractMeta = ExtractMetaDataUtils.getInstance();
            try {
                PreparedStatement stmt =
                        extractMeta.getConn().prepareStatement(TableInfoParameters.ORACLE_10G_RECBIN_SQL);
                extractMeta.setQueryStatementTimeout(stmt);
                ResultSet rsTables = stmt.executeQuery();
                itemTablesName
                        .removeAll(ExtractMetaDataFromDataBase.getTableNamesFromQuery(rsTables, extractMeta.getConn()));
                rsTables.close();
                stmt.close();
            } catch (SQLException e) {
                // do nothing.
            }
        }
    }

    @Override
    protected List<String> retrieveItemTables(IMetadataConnection metadataConnection, TableInfoParameters tableInfoParameters,
            List<String> itemTablesName) throws SQLException, ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        ExtractMetaDataUtils extractMeta = ExtractMetaDataUtils.getInstance();
        Connection con = extractMeta.getConn();
        // if want to get all tables and synonyms,need to get the value of the public_synonym_checken botton
        if (con != null && con.toString().contains("oracle.jdbc.driver") //$NON-NLS-1$
                && extractMeta.isUseAllSynonyms()) {
            Set<String> nameFiters = tableInfoParameters.getNameFilters();

            StringBuffer filters = new StringBuffer();
            if (!nameFiters.isEmpty()) {
                filters.append(" and ("); //$NON-NLS-1$
                final String tStr = " all_synonyms.synonym_name like ?"; //$NON-NLS-1$
                for (int i = 0; i < nameFiters.size(); i++) {
                    if (i != 0) {
                        filters.append(" or "); //$NON-NLS-1$
                    }
                    filters.append(tStr);
                }
                filters.append(')');
            }
            PreparedStatement stmt = con.prepareStatement(GET_ALL_SYNONYMS + filters.toString());
            int i = 1;
            for (String s : nameFiters) {
                stmt.setString(i, s);
                i++;
            }
            extractMeta.setQueryStatementTimeout(stmt);
            ResultSet rsTables = stmt.executeQuery();
            itemTablesName = ExtractMetaDataFromDataBase.getTableNamesFromQuery(rsTables, extractMeta.getConn());
            rsTables.close();
            stmt.close();

            // tableTypeMap.clear();
            for (String synonymName : itemTablesName) {
                getTableTypeMap().put(synonymName, ETableTypes.TABLETYPE_SYNONYM.getName());
            }

        } else {
            itemTablesName = super.retrieveItemTables(metadataConnection, tableInfoParameters, itemTablesName);
        }
        return itemTablesName;
    }

    @Override
    public String getTableComment(IMetadataConnection metadataConnection, ResultSet resultSet, String nameKey)
            throws SQLException {
        return super.getTableComment(metadataConnection, resultSet, nameKey);
    }

}
