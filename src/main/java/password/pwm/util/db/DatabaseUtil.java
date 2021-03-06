/*
 * Password Management Servlets (PWM)
 * http://www.pwm-project.org
 *
 * Copyright (c) 2006-2009 Novell, Inc.
 * Copyright (c) 2009-2017 The PWM Project
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package password.pwm.util.db;

import lombok.AllArgsConstructor;
import lombok.Getter;
import password.pwm.error.ErrorInformation;
import password.pwm.error.PwmError;
import password.pwm.util.logging.PwmLogger;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

class DatabaseUtil {
    private static final AtomicInteger OP_COUNTER = new AtomicInteger();

    private static final PwmLogger LOGGER = PwmLogger.forClass(DatabaseUtil.class);

    private DatabaseUtil()
    {
    }


    static void close(final Statement statement) throws DatabaseException
    {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                LOGGER.error("unexpected error during close statement object " + e.getMessage(), e);
                throw new DatabaseException(new ErrorInformation(PwmError.ERROR_DB_UNAVAILABLE, "statement close failure: " + e.getMessage()));
            }
        }
    }

    static void close(final ResultSet resultSet) throws DatabaseException
    {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                LOGGER.error("unexpected error during close resultSet object " + e.getMessage(), e);
                throw new DatabaseException(new ErrorInformation(PwmError.ERROR_DB_UNAVAILABLE, "resultset close failure: " + e.getMessage()));
            }
        }
    }

    static void commit(final Connection connection)
            throws DatabaseException
    {
        try {
            connection.commit();
        } catch (SQLException e) {
            LOGGER.warn("database commit failed: " + e.getMessage());
            throw new DatabaseException(new ErrorInformation(PwmError.ERROR_DB_UNAVAILABLE, "commit failure: " + e.getMessage()));
        }
    }

    static DatabaseException convertSqlException(
            final DebugInfo debugInfo,
            final SQLException e
    )
    {
        final String errorMsg = debugInfo.getOpName() + " operation opId=" + debugInfo.getOpId() + " failed, error: " + e.getMessage();
        final ErrorInformation errorInformation = new ErrorInformation(PwmError.ERROR_DB_UNAVAILABLE, errorMsg);
        return new DatabaseException(errorInformation);
    }

    static void initTable(
            final Connection connection,
            final DatabaseTable table,
            final DBConfiguration dbConfiguration
    )
            throws DatabaseException
    {
        boolean tableExists = false;
        try {
            checkIfTableExists(connection, table);
            LOGGER.trace("table " + table + " appears to exist");
            tableExists = true;
        } catch (DatabaseException e) { // assume error was due to table missing;
            LOGGER.trace("error while checking for table: " + e.getMessage() + ", assuming due to table non-existence");
        }

        if (!tableExists) {
            createTable(connection, table, dbConfiguration);
        }
    }

    private static void createTable(
            final Connection connection,
            final DatabaseTable table,
            final DBConfiguration dbConfiguration
    )
            throws DatabaseException
    {
        {
            final StringBuilder sqlString = new StringBuilder();
            sqlString.append("CREATE table ").append(table.toString()).append(" (").append("\n");
            sqlString.append("  " + DatabaseService.KEY_COLUMN + " ").append(dbConfiguration.getColumnTypeKey()).append("(").append(
                    DatabaseService.KEY_COLUMN_LENGTH).append(") NOT NULL PRIMARY KEY,").append("\n");
            sqlString.append("  " + DatabaseService.VALUE_COLUMN + " ").append(dbConfiguration.getColumnTypeValue()).append(" ");
            sqlString.append("\n");
            sqlString.append(")").append("\n");

            LOGGER.trace("attempting to execute the following sql statement:\n " + sqlString.toString());

            Statement statement = null;
            try {
                statement = connection.createStatement();
                statement.execute(sqlString.toString());
                connection.commit();
                LOGGER.debug("created table " + table.toString());
            } catch (SQLException ex) {
                final String errorMsg = "error creating new table " + table.toString() + ": " + ex.getMessage();
                final ErrorInformation errorInformation = new ErrorInformation(PwmError.ERROR_DB_UNAVAILABLE, errorMsg);
                throw new DatabaseException(errorInformation);
            } finally {
                DatabaseUtil.close(statement);
            }
        }

        {
            final String indexName = table.toString() + "_IDX";
            final StringBuilder sqlString = new StringBuilder();
            sqlString.append("CREATE index ").append(indexName);
            sqlString.append(" ON ").append(table.toString());
            sqlString.append(" (").append(DatabaseService.KEY_COLUMN).append(")");
            Statement statement = null;

            LOGGER.trace("attempting to execute the following sql statement:\n " + sqlString.toString());

            try {
                statement = connection.createStatement();
                statement.execute(sqlString.toString());
                connection.commit();
                LOGGER.debug("created index " + indexName);
            } catch (SQLException ex) {
                final String errorMsg = "error creating new index " + indexName + ": " + ex.getMessage();
                final ErrorInformation errorInformation = new ErrorInformation(PwmError.ERROR_DB_UNAVAILABLE, errorMsg);
                throw new DatabaseException(errorInformation);
            } finally {
                DatabaseUtil.close(statement);
            }
        }
    }

    private static void checkIfTableExists(
            final Connection connection,
            final DatabaseTable table
    )
            throws DatabaseException
    {
        final DatabaseUtil.DebugInfo debugInfo = DatabaseUtil.DebugInfo.create("checkIfTableExists",null,null,null);
        final StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM  ").append(table.toString()).append(" WHERE " + DatabaseService.KEY_COLUMN + " = '0'");
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sb.toString());
        } catch (SQLException e) {
            throw DatabaseUtil.convertSqlException(debugInfo, e);
        } finally {
            DatabaseUtil.close(statement);
            DatabaseUtil.close(resultSet);
        }
    }


    @Getter
    @AllArgsConstructor
    static class DebugInfo implements Serializable {
        private final Instant startTime = Instant.now();
        private final int opId = OP_COUNTER.incrementAndGet();
        private final String opName;
        private final DatabaseTable table;
        private final String key;
        private final String value;

        static DebugInfo create(
                final String opName,
                final DatabaseTable table,
                final String key,
                final String value
        ) {
            return new DebugInfo(
                    opName,
                    table,
                    key,
                    value
            );
        }
    }
}
