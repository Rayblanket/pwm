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

import password.pwm.AppProperty;
import password.pwm.config.Configuration;
import password.pwm.config.PwmSetting;
import password.pwm.config.value.FileValue;
import password.pwm.util.PasswordData;
import password.pwm.util.java.JavaHelper;
import password.pwm.util.java.StringUtil;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DBConfiguration implements Serializable {
    private final String driverClassname;
    private final String connectionString;
    private final String username;
    private final PasswordData password;
    private final String columnTypeKey;
    private final String columnTypeValue;
    private final byte[] jdbcDriver;
    private final List<JDBCDriverLoader.ClassLoaderStrategy> classLoaderStrategies;

    private DBConfiguration(
            final String driverClassname,
            final String connectionString,
            final String username,
            final PasswordData password,
            final String columnTypeKey,
            final String columnTypeValue,
            final byte[] jdbcDriver,
            final List<JDBCDriverLoader.ClassLoaderStrategy> classLoaderStrategies
    ) {
        this.driverClassname = driverClassname;
        this.connectionString = connectionString;
        this.username = username;
        this.password = password;
        this.columnTypeKey = columnTypeKey;
        this.columnTypeValue = columnTypeValue;
        this.jdbcDriver = jdbcDriver;
        this.classLoaderStrategies = classLoaderStrategies;
    }

    public String getDriverClassname() {
        return driverClassname;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public String getUsername() {
        return username;
    }

    public PasswordData getPassword() {
        return password;
    }

    public String getColumnTypeKey() {
        return columnTypeKey;
    }

    public String getColumnTypeValue() {
        return columnTypeValue;
    }


    public byte[] getJdbcDriver()
    {
        return jdbcDriver;
    }

    public List<JDBCDriverLoader.ClassLoaderStrategy> getClassLoaderStrategies() {
        return classLoaderStrategies;
    }

    public boolean isEnabled() {
        return
                !StringUtil.isEmpty(driverClassname)
                && !StringUtil.isEmpty(connectionString)
                && !StringUtil.isEmpty(username)
                && !(password == null);
    }

     static DBConfiguration fromConfiguration(final Configuration config) {
         final Map<FileValue.FileInformation, FileValue.FileContent> fileValue = config.readSettingAsFile(
                 PwmSetting.DATABASE_JDBC_DRIVER);
         final byte[] jdbcDriverBytes;
         if (fileValue != null && !fileValue.isEmpty()) {
             final FileValue.FileInformation fileInformation1 = fileValue.keySet().iterator().next();
             final FileValue.FileContent fileContent = fileValue.get(fileInformation1);
             jdbcDriverBytes = fileContent.getContents();
         } else {
             jdbcDriverBytes = null;
         }

         final String strategyList = config.readAppProperty(AppProperty.DB_JDBC_LOAD_STRATEGY);
         final List<JDBCDriverLoader.ClassLoaderStrategy> strategies = JavaHelper.readEnumListFromStringCollection(
                 JDBCDriverLoader.ClassLoaderStrategy.class,
                 Arrays.asList(strategyList.split(","))
         );


         return new DBConfiguration(
                 config.readSettingAsString(PwmSetting.DATABASE_CLASS),
                 config.readSettingAsString(PwmSetting.DATABASE_URL),
                 config.readSettingAsString(PwmSetting.DATABASE_USERNAME),
                 config.readSettingAsPassword(PwmSetting.DATABASE_PASSWORD),
                 config.readSettingAsString(PwmSetting.DATABASE_COLUMN_TYPE_KEY),
                 config.readSettingAsString(PwmSetting.DATABASE_COLUMN_TYPE_VALUE),
                 jdbcDriverBytes,
                 strategies
         );

     }
}
