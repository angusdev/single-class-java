/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
import java.io.IOException;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Command line SQL Plus like tools requires JDBC driver only.
 *
 * @author http://twitter.com/angusdev
 * @version 1.0
 */
public class JSQLPlus {
    private static final String[][] KNOWN_DRIVER = { { "oracle.jdbc.OracleDriver", "oracle.jdbc.driver.OracleDriver" },
            { "org.h2.Driver" } };

    private static void printUsage() {
        System.out.println("java -jar select.jar <driver class> <jdbc url> <sql>");
    }

    // command line
    // <driver class> <jdbc url> <sql>
    public static void main(String args[]) throws SQLException, IOException {
        List<String> noSwitchArg = new ArrayList<String>();
        Map<String, String> switchMap = new LinkedHashMap<String, String>();
        String driverClass = null;
        String jdbcURL = null;
        String sql = null;
        for (String arg : args) {
            if (arg.startsWith("-")) {
                int pos = arg.indexOf('=');
                if (pos >= 0) {
                    switchMap.put(arg.substring(1, pos), arg.substring(pos + 1));
                }
                else {
                    switchMap.put(arg.substring(1), null);
                }
            }
            else {
                noSwitchArg.add(arg);
            }
        }

        for (String s : noSwitchArg) {
            if (s.matches("([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]*")) {
                if (driverClass != null) {
                    printUsage();
                    System.exit(1);
                }
                driverClass = noSwitchArg.remove(0);
            }
            else if (s.startsWith("jdbc:")) {
                if (jdbcURL != null) {
                    printUsage();
                    System.exit(1);
                }
                jdbcURL = s;
            }
            else {
                if (sql != null) {
                    printUsage();
                    System.exit(1);
                }
                sql = s;
            }
        }

        if (driverClass != null) {
            try {
                Class.forName(driverClass);
                System.out.println("Loaded driver " + driverClass);
            }
            catch (ClassNotFoundException ex) {
                System.out.println("Cannot load driver " + driverClass);
                System.exit(1);
            }
        }
        else {
            // initialize known driver
            for (String[] p : KNOWN_DRIVER) {
                for (String s : p) {
                    try {
                        Class.forName(s);
                        System.out.println("Loaded driver " + s);
                        break;
                    }
                    catch (ClassNotFoundException ex) {
                        ;
                    }
                }
            }
        }
        final int tableWidth = switchMap.get("w") != null ? Integer.valueOf(switchMap.get("w")) : -1;

        String user, pass;
        user = readEntry("Userid  : ");
        pass = readEntry("Password: ");
        Connection conn = null;
        Statement stmt = null;
        try {
            System.out.println("Welcome to the SQL Interpreter\n");
            conn = DriverManager.getConnection(jdbcURL, user, pass);
            stmt = conn.createStatement();
            System.out.print("SQL> ");
            do {
                String query = readQuery();
                if (query.equals("exit")) {
                    break;
                }
                ResultSet rs;
                try {
                    rs = stmt.executeQuery(query);
                }
                catch (SQLException ex) {
                    System.out.println(ex.getMessage());
                    continue;
                }
                ResultSetMetaData rsmd = rs.getMetaData();
                int nCols = rsmd.getColumnCount();
                TextTable table = new TextTable(nCols);
                for (int i = 1; i <= nCols; i++) {
                    table.add(rsmd.getColumnName(i));
                }
                while (rs.next()) {
                    for (int i = 1; i <= nCols; i++) {
                        if (rsmd.getColumnType(i) == Types.CLOB) {
                            Clob clob = rs.getClob(i);
                            table.add(clob.getSubString(1, (int) Math.min(clob.length(), 1000)));
                        }
                        else {
                            table.add(rs.getObject(i));
                        }
                    }
                }
                table.registerFormatter(Date.class, new TextTable.Formatter() {
                    private SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.ssss");
                    private SimpleDateFormat dfDateOnly = new SimpleDateFormat("dd-MMM-yyyy");

                    public String format(Object obj) {
                        Calendar c = Calendar.getInstance();
                        c.setTime((Date) obj);
                        if (c.get(Calendar.HOUR_OF_DAY) == 0 && c.get(Calendar.MINUTE) == 0
                                && c.get(Calendar.SECOND) == 0) {
                            return dfDateOnly.format(obj);
                        }
                        else {
                            return df.format(obj);
                        }
                    }

                });
                table.setMaxWidth(tableWidth).setDefaultCellStyle(new TextTable.CellStyle().setNullText("<null>"))
                        .render();
            } while (true);
        }
        catch (Exception ex) {
            ;
        }
        finally {
            if (stmt != null) {
                try {
                    stmt.close();
                }
                catch (SQLException ex) {
                    ;
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                }
                catch (SQLException ex) {
                    ;
                }
            }
        }
        System.out.println("Thank you for using the SQL Interpreter\n");
    }

    // readEntry function -- to read input string
    static String readQuery() {
        try {
            StringBuffer buffer = new StringBuffer();
            System.out.flush();
            int c = System.in.read();
            while (c != ';' && c != -1) {
                if (c != '\n')
                    buffer.append((char) c);
                else {
                    buffer.append(" ");
                    System.out.print("SQL> ");
                    System.out.flush();
                }
                c = System.in.read();
            }
            return buffer.toString().trim();
        }
        catch (IOException e) {
            return "";
        }
    }

    // readEntry function -- to read input string
    static String readEntry(String prompt) {
        try {
            StringBuffer buffer = new StringBuffer();
            System.out.print(prompt);
            System.out.flush();
            int c = System.in.read();
            while (c != '\n' && c != -1) {
                buffer.append((char) c);
                c = System.in.read();
            }
            return buffer.toString().trim();
        }
        catch (IOException e) {
            return "";
        }
    }

}