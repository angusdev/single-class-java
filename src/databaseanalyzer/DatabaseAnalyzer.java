import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class DatabaseAnalyzer {
    private String jdbcurl;
    private String jdbcuser;
    private String jdbcpass;
    private Set<String> tables;
    private Properties prop;
    private boolean quiet = true;

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: java DatabaseAnalyser jdbcclass jdbcurl jdbcuser jdbcpass tablelist");
            return;
        }

        String jdbcclass = args[0];
        String jdbcurl = args[1];
        String jdbcuser = args[2];
        String jdbcpass = args[3];
        Set<String> tables = null;
        if (args.length > 4) {
            tables = new HashSet<>();
            String[] t = args[4].split(",");
            for (String s : t) {
                tables.add(s.toUpperCase());
            }
        }

        Properties prop = new Properties();
        FileInputStream fs = null;
        try {
            fs = new FileInputStream("dban.properties");
            prop.load(fs);
        }
        catch (FileNotFoundException ex) {
            ;
        }
        finally {
            if (fs != null) {
                fs.close();
            }
        }

        Class.forName(jdbcclass);
        new DatabaseAnalyzer(jdbcurl, jdbcuser, jdbcpass, tables, prop).start();
    }

    public DatabaseAnalyzer(String jdbcurl, String jdbcuser, String jdbcpass, Set<String> tables, Properties prop) {
        this.jdbcurl = jdbcurl;
        this.jdbcuser = jdbcuser;
        this.jdbcpass = jdbcpass;
        this.tables = tables;
        this.prop = prop;
    }

    public void start() throws Exception {
        Connection conn = null;
        ResultSet rs = null;
        try {
            log("Welcome to the DatabaseAnalyser\n");
            log("Connecting to " + jdbcurl);
            conn = DriverManager.getConnection(jdbcurl, jdbcuser, jdbcpass);
            log("Connected");

            log("Getting table list ...");
            DatabaseMetaData md = conn.getMetaData();
            String[] types = { "TABLE" };
            rs = md.getTables(null, jdbcuser, "%", types);
            List<String[]> tableList = new ArrayList<>();
            while (rs.next()) {
                String catalog = rs.getString("TABLE_CAT") != null ? rs.getString("TABLE_CAT").toUpperCase() : null;
                String schema = rs.getString("TABLE_SCHEM") != null ? rs.getString("TABLE_SCHEM").toUpperCase() : null;
                String tableName = rs.getString("TABLE_NAME") != null ? rs.getString("TABLE_NAME").toUpperCase() : null;

                if (tables == null || tables.contains(tableName)) {
                    String[] tab = new String[] { catalog, schema, tableName };
                    tableList.add(tab);
                }
            }
            rs.close();
            log(tableList.size() + " tables");

            for (String[] s : tableList) {
                analysisTable(conn, s[0], s[1], s[2]);
            }
        }
        finally {
            if (rs != null) {
                rs.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
    }

    private void log(String s) {
        if (!quiet) {
            System.err.println(s);
        }
    }

    private void out(Object... strings) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object s : strings) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(s == null ? "" : s.toString());
        }
        System.out.println(sb.toString());
    }

    private void analysisTable(Connection conn, String catalog, String schema, String table) throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;

        log("Analysing " + table);
        String sql = prop.getProperty(schema + "." + table + ".sql");
        sql = sql == null ? "" : sql;
        try {

            DatabaseMetaData md = conn.getMetaData();
            rs = md.getColumns(catalog, schema, table, null);
            int totalCols = 0;
            while (rs.next()) {
                ++totalCols;
                String col = rs.getString("COLUMN_NAME").toUpperCase();
                String clause = prop.getProperty("global.exclude.sql.column." + col);
                if (clause != null) {
                    sql = (sql.length() > 0 ? sql + " and " : "") + "\"" + col + "\" " + clause;
                }
            }
            rs.close();
            rs = null;

            String whereSql = sql.length() > 0 ? " where " + sql : "";
            String andSql = sql.length() > 0 ? " and " + sql : "";

            stmt = conn.createStatement();
            rs = stmt.executeQuery("select count(*) from " + table + whereSql);
            rs.next();
            int totalRecords = rs.getInt(1);
            rs.close();
            rs = null;
            stmt.close();
            stmt = null;

            out(table);
            out(table, "Total Records", totalRecords);
            out(table, "Total Columns", totalCols);
            out(table, "Column", "PK", "Unique", "Unique %", "Null Count", "Null %", "Col", "", "", "Col", "", "",
                    "Col", "", "", "Col", "", "", "Col", "", "", "Col", "", "", "Col", "", "", "Col", "", "", "Col",
                    "", "", "Col", "", "");

            rs = md.getPrimaryKeys(catalog, schema, table);
            Set<String> pk = new HashSet<>();
            while (rs.next()) {
                pk.add(rs.getString("COLUMN_NAME").toUpperCase());
            }
            rs.close();
            rs = null;

            rs = md.getColumns(catalog, schema, table, null);
            while (rs.next()) {
                analysisColumn(conn, catalog, schema, table, rs.getString("COLUMN_NAME"), totalRecords, pk, whereSql,
                        andSql);
            }
            rs.close();
            rs = null;
        }
        finally {
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    private void analysisColumn(Connection conn, String catalog, String schema, String table, String col,
            int totalRecords, Set<String> pk, String whereSql, String andSql) throws SQLException {
        col = col.toUpperCase();

        Statement stmt = null;
        ResultSet rs = null;

        try {
            List<Object> colData = new ArrayList<>();
            colData.add(table);
            colData.add(col);
            colData.add(pk.contains(col) ? "TRUE" : "");

            if (!"true".equalsIgnoreCase(prop.getProperty("global.exclude.column." + col))) {
                stmt = conn.createStatement();
                rs = stmt.executeQuery("select count(distinct \"" + col + "\") from \"" + table + "\"" + whereSql);
                rs.next();
                int distinct = rs.getInt(1);
                rs.close();
                rs = null;
                stmt.close();
                stmt = null;

                colData.add(distinct);
                colData.add((Math.round(distinct * 1.0 / totalRecords * 10000) / 100.0) + "%");

                // list most frequent values
                stmt = conn.createStatement();
                rs = stmt.executeQuery("select count(*) from \"" + table + "\" where \"" + col + "\" is null" + andSql);
                rs.next();
                colData.add(rs.getLong(1));
                colData.add((Math.round(rs.getLong(1) * 1.0 / totalRecords * 10000) / 100.0) + "%");
                rs.close();
                rs = null;
                stmt.close();
                stmt = null;

                stmt = conn.createStatement();
                rs = stmt.executeQuery("select * from (select \"" + col + "\", count(*) from \"" + table + "\" "
                        + whereSql + " group by \"" + col + "\" order by 2 desc, 1) where rownum < 10");
                while (rs.next()) {
                    double pct = Math.round(rs.getLong(2) * 1.0 / totalRecords * 10000) / 100.0;
                    if (distinct < 100 || pct >= 0.1) {
                        colData.add(rs.getString(1) != null ? rs.getString(1) : "<NULL>");
                        colData.add(rs.getLong(2));
                        colData.add(pct + "%");
                    }
                }
                rs.close();
                rs = null;
                stmt.close();
                stmt = null;
            }

            out(colData.toArray());
        }
        finally {
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        }
    }
}
