/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.seata.rm.datasource.undo.sqlserver;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.util.ColumnUtils;
import io.seata.sqlparser.util.JdbcConstants;

/**
 */
public class SqlServerUndoDeleteExecutor extends BaseSqlServerUndoExecutor {
    /**
     * Instantiates a new sql server delete undo executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public SqlServerUndoDeleteExecutor(SQLUndoLog sqlUndoLog) {
        super(sqlUndoLog);
    }

    @Override
    protected String buildUndoSQL() {
        TableRecords beforeImage = sqlUndoLog.getBeforeImage();
        List<Row> beforeImageRows = beforeImage.getRows();
        if (CollectionUtils.isEmpty(beforeImageRows)) {
            throw new ShouldNeverHappenException("Invalid UNDO LOG");
        }
        Row row = beforeImageRows.get(0);
        List<Field> fields = new ArrayList<>(row.nonPrimaryKeys());
        fields.addAll(getOrderedPkList(beforeImage, row, JdbcConstants.SQLSERVER));

        // delete sql undo log before image all field come from table meta, need add escape.
        // see BaseTransactionalExecutor#buildTableRecords
        String insertColumns = fields.stream()
                .map(field -> ColumnUtils.addEscape(field.getName(), JdbcConstants.SQLSERVER))
                .collect(Collectors.joining(", "));
        String insertValues = fields.stream().map(field -> "?")
                .collect(Collectors.joining(", "));

        return "SET IDENTITY_INSERT " +
                sqlUndoLog.getTableName() +
                " ON; INSERT INTO " +
                sqlUndoLog.getTableName() +
                " (" +
                insertColumns +
                ") VALUES (" +
                insertValues +
                "); SET IDENTITY_INSERT " +
                sqlUndoLog.getTableName() +
                " OFF;";
    }

    @Override
    protected TableRecords getUndoRows() {
        return sqlUndoLog.getBeforeImage();
    }
}
