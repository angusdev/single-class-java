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
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is to render fixed character table with support of cell wrapping, alignment, padding, border style, column
 * width. The target to output table-based information in console program nicely.
 * <p>
 * Below are some examples:
 * </p>
 * 
 * <pre>
 * +-----------------------------------------------------------------------------------------------------+
 * | Name     | Email         | Country       | City      | Area |     Points | Join Date   | Login Time | 
 * |=====================================================================================================|
 * | Jimmy    | jimmy@gmai... | Hong Kong     | Hong Kong | 7x9  |  76,348.48 | 09-Aug-2012 |   17:37    |
 * |----------+---------------+---------------+-----------+------+------------+-------------+------------|
 * | Johnny   |               | United States | New York  | 5x9  |  13,905.24 | 17-Jun-2013 |   18:19    |
 * |----------+---------------+---------------+-----------+------+------------+-------------+------------|
 * | Jason    | jason@hotm... | Switzerland   | Zurich    | 3x8  | 620,912.23 | 25-Jan-2012 |   18:08    |
 * +-----------------------------------------------------------------------------------------------------+
 * 
 * +--------------------------------------------------------------------+
 * | Song                | Lyrics                                       |
 * |====================================================================|
 * | Greensleeves        | Alas, my love, you do me wrong, To  cast  me |
 * |                     | off discourteously. For  I  have  loved  you |
 * |                     | well and long, Delighting in  your  company. |
 * |                     | Greensleeves was all my joy Greensleeves was |
 * |                     | my delight, Greensleeves  was  my  heart  of |
 * |                     | gold, And who but my lady greensleeves.      |
 * |---------------------+----------------------------------------------|
 * | What Child Is This? | What child is this, who, laid  to  rest,  On |
 * |                     | Mary' lap is  sleeping,  Whom  angels  greet |
 * |                     | with anthems sweet.  While  shepherds  watch |
 * |                     | are keeping? This, this is Christ the  King, |
 * |                     | Whom shepherds guard and angels sing; Haste, |
 * |                     | haste to bring Him laud, The babe,  the  son |
 * |                     | of Mary!                                     |
 * +--------------------------------------------------------------------+
 * </pre>
 * 
 * This project is inspired by http://sourceforge.net/projects/texttablefmt/
 * <p>
 * <i>required Java 1.5+</i>
 * </p>
 * 
 * @author http://twitter.com/angusdev
 * @version 1.0
 */
public class TextTable {
    /** The text alignment of the cell */
    public enum Align {
    /** Align left */
    LEFT,
    /** Align center */
    CENTER,
    /** Align right */
    RIGHT,
    /** Justify (but not on and align left if no space or at last line */
    JUSTIFY_LEFT,
    /** Justify and align right if no space or at last line */
    JUSTIFY_RIGHT,
    /** Justify and align center if no space or at last line */
    JUSTIFY_CENTER,
    /** Justify even on last line and align left if no space */
    JUSTIFY_LEFT_ALWAYS,
    /** Justify even on last line and align right if no space */
    JUSTIFY_RIGHT_ALWAYS,
    /** Justify even on last line and align center if no space */
    JUSTIFY_CENTER_ALWAYS
    }

    /** The behaviour when the text overflow */
    public enum Wrap {
        /** Wrap word */
        WRAP_WORD,
        /** Wrap and sharp cut on edge */
        WRAP,
        /** Crop at edge */
        CROP,
        /**
         * Ellipsis. Use {@link #setEllipsis(String)} to set the ellipsis text.
         * 
         * @see #setEllipsis(String)
         */
        ELLIPSIS
    }

    private enum LineType {
        TOP, BOTTOM, HEADER, HEADER_BOTTOM, CONTENT, CONTENT_BOTTOM
    }

    // @formatter:off
    /**
     * The border style.
     * 
     * For the example
     * 
     * <pre>
     * Example:
     * 
     *             111111111122222222
     *   0123456789012345678901234567
     * 0 +--------+--------+--------+  <- t(op)
     * 1 |        |        |        |
     * 2 +========+========+========+  <- h(eader)
     * 3 |        |        |        |
     * 4 +--------+--------+--------+  <- m(iddle)
     * 5 |        |        |        |
     * 6 +--------+--------+--------+  <- m(iddle)
     * 7 |        |        |        |
     * 8 +--------+--------+--------+  <- b(ottom)
     *   
     *   ^        ^          ^^^    ^
     *   |        |           |     |
     *   l(eft) c(ross)    h(oriz)  r(ight)
     * 
     * tl - (0,0)
     * th - (1,0) -> (8,0), (10,0) -> (17,0), (19,0) -> (26,0)
     * tc - (9,0), (18,0)
     * tr - (27,0)
     * hl - (0,2)
     * hh - (1,2) -> (8,2), (10,2) -> (17,2), (19,2) -> (26,2)
     * hc - (9,2), (18,2)
     * hr - (27,2)
     * ml - (0,4), (0,6)
     * mh - (1,4) -> (8,4), (10,4) -> (17,4), (19,4) -> (26,4)
     * mc - (9,4), (18,4), (9,6), (18,6)
     * mr - (27,4), (27,6)
     * bl - (0,8)
     * bh - (1,8) -> (8,8), (10,8) -> (17,8), (19,8) -> (26,8)
     * bc - (9,8), (18,8)
     * br - (27,8)
     * l - (0,1), (0,3), (0,5), (0,7)
     * c - (9,8), (18,8)
     * r - (27,1), (27,3), (27,5), (27,7)
     * </pre>
     */
    // @formatter:on
    public static class BorderStyle {
        // Border Fill Mode
        public static final int TOP = 1;
        public static final int BOTTOM = 2;
        public static final int RIGHT = 4;
        public static final int LEFT = 8;
        public static final int CONTENT_H = 16;
        public static final int CONTENT_V = 32;
        public static final int HEADER = 64;
        public static final int HEADER_V = 128;
        public static final int FIRST_COL = 256;
        public static final int LAST_COL = 512;
        public static final int OUTER = TOP | BOTTOM | RIGHT | LEFT;
        public static final int INNER_V = HEADER_V | CONTENT_V;
        public static final int INNER_H = CONTENT_H;
        public static final int INNER = INNER_H | INNER_V | HEADER;
        public static final int H_ONLY = TOP | BOTTOM | INNER_H | HEADER;
        public static final int V_ONLY = LEFT | RIGHT | INNER_V;
        public static final int NONE = 0;
        public static final int ALL = INNER | OUTER;

        private String tl, th, tc, tr, hl, hh, hc, hr, ml, mh, mc, mr, bl, bh, bc, br, l, c, r;
        private int llen, hlen, clen, rlen;

        // @formatter:off
        /**
         * <pre>
         * +--------+--------+--------+
         * |        |        |        |
         * +========+========+========+
         * |        |        |        |
         * +--------+--------+--------+
         * |        |        |        |
         * +--------+--------+--------+
         * |        |        |        |
         * +--------+--------+--------+
         * </pre>
         */
        // @formatter:on
        public static BorderStyle BASIC = new BorderStyle("+", "-", "-", "+", "|", "=", "=", "|", "|", "-", "+", "|",
                "+", "-", "-", "+", "|", "|", "|");

        /**
         * Create the border style. The border of same type should have same length. E.g. "||" for <code>tl</code>,
         * <code>hl</code>, <code>ml</code>, <code>bl</code>, <code>l</code> and "***" for <code>tr</code>,
         * <code>hr</code>, <code>mr</code>, <code>br</code>, <code>r</code>
         * 
         * @param tl
         *            top left
         * @param th
         *            top horizontal
         * @param tc
         *            top cross
         * @param tr
         *            top right
         * @param hl
         *            header left
         * @param hh
         *            header horizontal
         * @param hc
         *            header cross
         * @param hr
         *            header right
         * @param ml
         *            bottom left
         * @param mh
         *            middle horizontal
         * @param mc
         *            middle cross
         * @param mr
         *            middle right
         * @param bl
         *            bottom left
         * @param bh
         *            bottom horizontal
         * @param bc
         *            bottom cross
         * @param br
         *            bottom right
         * @param l
         *            left border
         * @param c
         *            center vertical border
         * @param r
         *            right border
         */
        public BorderStyle(String tl, String th, String tc, String tr, String hl, String hh, String hc, String hr,
                String ml, String mh, String mc, String mr, String bl, String bh, String bc, String br, String l,
                String c, String r) {

            if (tl == null || th == null || tc == null || tr == null || hl == null || hh == null || hc == null
                    || hr == null || ml == null || mh == null || mc == null || mr == null || bl == null || bh == null
                    || bc == null || br == null || l == null || c == null || r == null) {
                throw new IllegalArgumentException(
                        "the element of " + BorderStyle.class.getSimpleName() + " cannot be null");
            }

            llen = tl.length();
            hlen = th.length();
            clen = tc.length();
            rlen = tr.length();

            if ((llen != hl.length() || llen != ml.length() || llen != bl.length() || llen != l.length())
                    || (hlen != hh.length() || hlen != mh.length() || hlen != bh.length())
                    || (clen != hc.length() || clen != mc.length() || clen != bc.length() || clen != c.length())
                    || (rlen != hr.length() || rlen != mr.length() || rlen != br.length() || rlen != r.length())) {
                throw new IllegalArgumentException("different length of same set of border (l,c,r)");
            }

            this.tl = tl;
            this.th = th;
            this.tc = tc;
            this.tr = tr;
            this.hl = hl;
            this.hh = hh;
            this.hc = hc;
            this.hr = hr;
            this.ml = ml;
            this.mh = mh;
            this.mc = mc;
            this.mr = mr;
            this.bl = bl;
            this.bh = bh;
            this.bc = bc;
            this.br = br;
            this.l = l;
            this.c = c;
            this.r = r;
        }

        private int calcWidth(int col) {
            return llen + (clen * col - 1) + rlen;
        }
    }

    public static class CellStyle {
        private Align align = Align.LEFT;
        private String nullText = "";
        private int paddingLeft = 1;
        private int paddingRight = 1;
        private Wrap wrap = Wrap.WRAP_WORD;
        private String ellipsis = "\u2026";

        public CellStyle() {
        }

        public CellStyle(CellStyle that) {
            this.align = that.align;
            this.nullText = that.nullText;
            this.paddingLeft = that.paddingLeft;
            this.paddingRight = that.paddingRight;
            this.wrap = that.wrap;
            this.ellipsis = that.ellipsis;
        }

        /**
         * Set the text alignment.
         * 
         * @param align
         *            the {@link Align} style.
         * @return this
         */
        public CellStyle setAlign(Align align) {
            this.align = align;
            return this;
        }

        /**
         * Set the text if the cell value is <code>null</code>.
         * 
         * @param nullText
         *            the text
         * @return this
         */
        public CellStyle setNullText(String nullText) {
            this.nullText = nullText;
            return this;
        }

        /**
         * Set the left padding.
         * 
         * @param paddingLeft
         *            the padding
         * @return this
         */
        public CellStyle setPaddingLeft(int paddingLeft) {
            this.paddingLeft = paddingLeft;
            return this;
        }

        /**
         * Set the right padding.
         * 
         * @param paddingRight
         *            the padding
         * @return this
         */
        public CellStyle setPaddingRight(int paddingRight) {
            this.paddingRight = paddingRight;
            return this;
        }

        /**
         * Set the Wrap style.
         * 
         * @param wrap
         *            the {@link Wrap} style
         * @return this
         */
        public CellStyle setWrap(Wrap wrap) {
            this.wrap = wrap;
            return this;
        }

        /**
         * Set the ellipse text.
         * 
         * @param ellipsis
         *            the text.
         * @return this
         */
        public CellStyle setEllipsis(String ellipsis) {
            this.ellipsis = ellipsis;
            return this;
        }

        /**
         * Return true if the align style is one of the justify style. There may be some special need to handle if the
         * align style is justify.
         * 
         * @return true if the align style is one of the justify style.
         */
        public boolean isAlignJustified() {
            return align.equals(Align.JUSTIFY_LEFT) || align.equals(Align.JUSTIFY_RIGHT)
                    || align.equals(Align.JUSTIFY_CENTER) || isAlignJustifiedAlways();
        }

        private boolean isAlignJustifiedAlways() {
            return align.equals(Align.JUSTIFY_LEFT_ALWAYS) || align.equals(Align.JUSTIFY_RIGHT_ALWAYS)
                    || align.equals(Align.JUSTIFY_CENTER_ALWAYS);
        }

        private String nullText(String s) {
            return s != null ? s : nullText;
        }

        private static Align fallbackJustify(Align align) {
            switch (align) {
            case JUSTIFY_LEFT:
            case JUSTIFY_LEFT_ALWAYS:
                return Align.LEFT;
            case JUSTIFY_RIGHT_ALWAYS:
                return Align.RIGHT;
            case JUSTIFY_CENTER_ALWAYS:
                return Align.CENTER;
            default:
                return align;
            }
        }
    }

    private class ColumnSetting {
        private CellStyle cellStyle;
        private int minWidth = -1;
        private int maxWidth = -1;
    }

    private class XY {
        private int x, y;

        private XY(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int hashCode() {
            return x * Integer.MAX_VALUE / 2 + y;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof XY) {
                XY xy = (XY) obj;
                return x == xy.x && y == xy.y;
            }
            else {
                return false;
            }
        }
    }

    /**
     * A very simple interface to redirect the logging information to your own logging framework. <code>TextTable</code>
     * is designed to be no external dependency so it will not use other common logging framework. You can use
     * {@link TextTable.setLogger} to receive the logging information. It is also ok for you skip this and leave
     * <code>TextTable</code> silence.
     * 
     * @see org.ellab.texttable.TextTable.setLogger()
     */
    public interface Logger {
        enum LogLevel {
            /** For <code>TextTable</code> development use (target audience: <code>TextTable</code> developer) */
            TRACE,
            /** Debug information for TextTable user (target audience: developer) */
            DEBUG,
            /** The table is not rendered as expected (target audience: developer) */
            DEBUG_WARN
        }

        void log(LogLevel level, String msg);

        void log(LogLevel level, String msg, Exception ex);
    }

    // A simple wrapper to call the underlying logger
    private static class SafeLogger {
        private Logger logger;

        private void setLogger(Logger logger) {
            this.logger = logger;
        }

        private void trace(String msg) {
            if (logger != null) {
                logger.log(Logger.LogLevel.TRACE, msg);
            }
        }

        private void debug(String msg) {
            if (logger != null) {
                logger.log(Logger.LogLevel.DEBUG, msg);
            }
        }

        private void warn(String msg) {
            if (logger != null) {
                logger.log(Logger.LogLevel.DEBUG_WARN, msg);
            }
        }
    }

    /**
     * An implementation of <code>TextTableLogger</code> to print the log message to <code>System.out</code>; The format
     * will be
     * <p>
     * [LEVEL] msg.
     * </p>
     */
    public static class ConsoleLogger implements Logger {
        private Logger.LogLevel level;

        public ConsoleLogger(Logger.LogLevel level) {
            this.level = level;
        }

        @Override
        public void log(LogLevel level, String msg) {
            log(level, msg, null);
        }

        @Override
        public void log(LogLevel level, String msg, Exception ex) {
            if (level.ordinal() >= this.level.ordinal()) {
                System.out.println("[" + level.name() + "] " + msg);
                if (ex != null) {
                    System.out.println(ex.getMessage());
                    ex.printStackTrace(System.out);
                }
            }
        }
    }

    /**
     * Add the ability to format different type of object. For example, you can use <code>SimpleDateFormat</code> to
     * render a <code>Date</code> object.
     * 
     * <p>
     * By default will use <code>toString()</code> to format the object.
     * </p>
     * 
     * @see #TextTable.registerFormatter(Formatter)
     */
    public interface Formatter {
        /**
         * Format the input object.
         * 
         * @param obj
         *            the input object
         * @return the formatted string
         */
        String format(Object obj);
    }

    private static final Formatter DEFAULT_FORMATTER = new Formatter() {
        @Override
        public String format(Object obj) {
            return obj.toString();
        }
    };

    /**
     * An implementation of <code>Formatter</code> to use <code>SimpleDateFormat</code> to format a <code>Date</code>
     * object.
     * 
     * see java.text.SimpleDateFormat
     */
    public static class SimpleDateFormater implements Formatter {
        private SimpleDateFormat df;

        public SimpleDateFormater(String fmt) {
            this.df = new SimpleDateFormat(fmt);
        }

        @Override
        public String format(Object obj) {
            return df.format(obj);
        }
    }

    /**
     * An implementation of <code>Formatter</code> to use <code>DecimalFormat</code> to format a <code>Number</code>
     * object.
     * 
     * @see java.text.DecimalFormat
     */
    public static class DecimalFormater implements Formatter {
        private DecimalFormat df;

        public DecimalFormater(String fmt) {
            this.df = new DecimalFormat(fmt);
        }

        @Override
        public String format(Object obj) {
            return df.format(obj);
        }
    }

    private static class Utils {
        private static String repeatToWidth(final String s, final int width) {
            return repeat(s, (width / s.length()) + 1).substring(0, width);
        }

        private static String repeat(final String s, final int count) {
            if (count <= 0 || s == null) {
                return "";
            }
            else {
                int len = s.length();
                if (count == 1 || len == 0) {
                    return s;
                }
                else if (len == 1) {
                    // most case
                    char ch = s.charAt(0);
                    char[] arr = new char[count];
                    for (int i = 0; i < count; i++) {
                        arr[i] = ch;
                    }
                    return new String(arr);
                }
                else {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < count; i++) {
                        sb.append(s);
                    }
                    return sb.toString();
                }
            }
        }

        private static void testWrapWord(String str, int length) {
            System.out.println(repeat("-", str.length()));
            System.out.println(str);
            System.out.println(repeat(" ", length) + "|");
            int pos = indexOfWrapWord(str, length);
            while (pos > 0) {
                System.out.println(str.substring(0, pos));
                if (pos < str.length()) {
                    str = str.substring(pos).trim();
                    pos = indexOfWrapWord(str, length);
                }
                else {
                    pos = -1;
                }
            }
        }

        // modified from org.apache.commons.lang3.text.WordUtils.wrap
        private static int indexOfWrapWord(String str, int length) {
            if (str == null) {
                return 0;
            }

            if (str.length() == 0) {
                return 0;
            }

            int spaceToWrapAt = str.lastIndexOf(' ', length);
            int hyphenToWrapAt = str.lastIndexOf('-', length - 1);
            int bestToWrapAt = Math.max(spaceToWrapAt, hyphenToWrapAt);
            if (spaceToWrapAt >= 0 || hyphenToWrapAt >= 0) {
                // normal case
                return bestToWrapAt + (hyphenToWrapAt == bestToWrapAt ? 1 : 0);
            }
            else {
                return Math.min(str.length(), length);
            }
        }

        private static <T> T safeGet(final ArrayList<T> list, final int index) {
            if (list != null && list.size() > index) {
                return list.get(index);
            }
            else {
                return null;
            }
        }

        private static void ensureSize(final ArrayList<?> list, final int size) {
            list.ensureCapacity(size);
            while (list.size() < size) {
                list.add(null);
            }
        }

    }

    private static final SafeLogger log = new SafeLogger();

    // style related
    private CellStyle defaultCellStyle = new CellStyle();
    private BorderStyle border = BorderStyle.BASIC;
    private int borderFill = BorderStyle.ALL;
    private int headerRow = 1;
    private int maxWidth;

    private final ArrayList<ColumnSetting> colSettings = new ArrayList<ColumnSetting>();

    // cell data related
    private final List<Object[]> data;
    private final int colCount;
    private final int[] maxColWidth;
    private final Map<XY, CellStyle> cellStyleMap = new HashMap<XY, CellStyle>();
    private final Map<Class<?>, Formatter> formatterMap = new HashMap<Class<?>, Formatter>();
    private int currCellCol;

    // store the colspan setting of cell
    private final Map<XY, Integer> colspanMap = new HashMap<XY, Integer>();
    // store the cells that are invisible due to colspan. If colspan of cell (1,2) = 3, then (2,2) and (3,2) will be
    // in the set
    private final Set<XY> colspanInvisibleSet = new HashSet<XY>();

    /**
     * Set the logger.
     * 
     * @param logger
     * @return the input logger
     */
    public static Logger setLogger(Logger logger) {
        TextTable.log.setLogger(logger);

        return logger;
    }

    public TextTable(Object[][] inputData) {
        data = Arrays.asList(inputData);
        this.colCount = data.get(0).length;
        maxColWidth = new int[colCount];
        currCellCol = Integer.MAX_VALUE;
    }

    public TextTable(int colCount) {
        data = new ArrayList<Object[]>();
        this.colCount = colCount;
        maxColWidth = new int[colCount];
        currCellCol = Integer.MAX_VALUE;
    }

    /**
     * Clear all data
     * 
     * @return the <code>TextTable</code> object for chaining
     */
    public TextTable clear() {
        data.clear();
        for (int i = 0; i < maxColWidth.length; i++) {
            maxColWidth[i] = 0;
        }
        currCellCol = Integer.MAX_VALUE;
        cellStyleMap.clear();
        colspanMap.clear();
        colspanInvisibleSet.clear();

        return this;
    }

    public TextTable setBorderFill(int borderFill) {
        this.borderFill = borderFill;

        return this;
    }

    public TextTable setHeaderRow(int headerRow) {
        this.headerRow = Math.min(headerRow, 0);

        return this;
    }

    public TextTable setMaxWidth(int maxWidth) {
        this.maxWidth = Math.max(maxWidth, 0);

        return this;
    }

    /**
     * Set the default cell style.
     * 
     * @param cs
     *            the cell style
     * @return the <code>TextTable</code> object for chaining
     */
    public TextTable setDefaultCellStyle(CellStyle cs) {
        this.defaultCellStyle = cs;

        return this;
    }

    /**
     * Set the border style.
     * 
     * @param bs
     *            the border style
     * @return the <code>TextTable</code> object for chaining
     */
    public TextTable setBorderStyle(BorderStyle bs) {
        this.border = bs;

        return this;
    }

    /**
     * Add a cell. Will automatically move to next row.
     * 
     * @param content
     *            the content of the cell
     * @return the <code>TextTable</code> object for chaining
     */
    public TextTable add(Object content) {
        return add(content, null);
    }

    /**
     * Add a cell. Will automatically move to next row.
     * 
     * @param content
     *            the content of the cell
     * @param cs
     *            the cell style, or null for inherit column style
     * @return the <code>TextTable</code> object for chaining
     */
    public TextTable add(Object content, CellStyle cs) {
        return add(content, cs, 1);
    }

    public TextTable add(Object content, CellStyle cs, int colspan) {
        if (currCellCol >= colCount || data.size() == 0) {
            // reach the end of row, add new row
            nextRow();
        }

        // prevent overflow. E.g. 3 columns, colspan of cell (1,0) = 3, will adjust to 2
        colspan = Math.min(colspan, colCount - currCellCol);

        int x = currCellCol++;
        int y = data.size() - 1;
        data.get(y)[x] = content;

        if (cs != null) {
            cellStyleMap.put(new XY(x, y), cs);
        }

        if (colspan > 1) {
            colspanMap.put(new XY(x, y), colspan);
            for (int i = 1; i < colspan; i++) {
                colspanInvisibleSet.add(new XY(x + i, y));
            }
            currCellCol += colspan - 1;
        }

        return this;
    }

    /**
     * End the current row and create a new row.
     * 
     * @return the <code>TextTable</code> object for chaining
     */
    public TextTable nextRow() {
        data.add(new Object[colCount]);
        currCellCol = 0;

        return this;
    }

    private CellStyle getColCellStyle(int col) {
        ColumnSetting colSetting = Utils.safeGet(colSettings, col);
        return colSetting != null && colSetting.cellStyle != null ? colSetting.cellStyle : defaultCellStyle;
    }

    private String format(Object data) {
        if (data == null) {
            return null;
        }

        return getFormatter(data.getClass()).format(data);
    }

    private String preRenderContent(Object data, CellStyle cs) {
        String s = cs.nullText(format(data)).trim();

        return s;
    }

    private String renderCell(final Object data, final CellStyle cs, final int width) {
        String text = preRenderContent(data, cs);

        // Align, only if text is shorter than width
        if (text.length() <= width) {
            Align align = cs.align;
            if (cs.isAlignJustified()) {
                String[] splitted = text.split("\\s+");

                if (splitted.length <= 1) {
                    // no space at all, fall back to LEFT, RIGHT, CENTER
                    align = CellStyle.fallbackJustify(align);
                }
                else {
                    int spaceNeeded = width;
                    for (String s : splitted) {
                        spaceNeeded -= s.length();
                    }
                    String pad = Utils.repeat(" ", spaceNeeded / (splitted.length - 1));
                    int mod = spaceNeeded % (splitted.length - 1);
                    StringBuilder sb = new StringBuilder(splitted[0]);
                    for (int i = 1; i < splitted.length; i++) {
                        // if splitted.length = 7, mod = 3, the 3 extra space will go to last 3 gaps (i.e. before 4-6
                        // segment (zero-based))
                        sb.append(pad).append(i >= splitted.length - mod ? " " : "").append(splitted[i]);
                    }
                    text = sb.toString();
                }
            }

            if (align.equals(Align.LEFT)) {
                text = text + Utils.repeat(" ", width - text.length());
            }
            else if (align.equals(Align.RIGHT)) {
                text = Utils.repeat(" ", width - text.length()) + text;
            }
            else if (align.equals(Align.CENTER)) {
                text = Utils.repeat(" ", (width - text.length()) / 2) + text;
                text = text + Utils.repeat(" ", width - text.length());
            }
        }
        return text;
    }

    /**
     * Set the column setting, including minimum width, maximum width and the default style of each cell of the column.
     * 
     * @param col
     *            the column index
     * @param minWidth
     *            minimum width, including cell padding
     * @param maxWidth
     *            maximum width, including cell padding
     * @param cs
     *            the style
     * @return the <code>TextTable</code> object for chaining
     */
    public TextTable setColumnSetting(int col, int minWidth, int maxWidth, CellStyle cs) {
        ColumnSetting colSetting = new ColumnSetting();
        colSetting.minWidth = minWidth;
        colSetting.maxWidth = maxWidth;
        colSetting.cellStyle = cs;
        Utils.ensureSize(colSettings, col + 1);
        colSettings.set(col, colSetting);
        return this;
    }

    /**
     * Register the formatter.
     * 
     * @param clazz
     *            object of this class will be handled by <code>formatter</code>
     * @param formatter
     *            the formatter
     */
    public void registerFormatter(Class<?> clazz, Formatter formatter) {
        formatterMap.put(clazz, formatter);
    }

    private Formatter getFormatter(Class<?> clazz) {
        Formatter f = null;
        if (formatterMap.size() > 0) {
            while (clazz != null && f == null) {
                f = formatterMap.get(clazz);
                if (f == null) {
                    clazz = clazz.getSuperclass();
                }
            }
        }

        return f != null ? f : DEFAULT_FORMATTER;
    }

    private String border(int fillFlag, String s) {
        return ((borderFill & fillFlag) > 0) ? s : "";
    }

    // row = -1 of top line
    private StringBuilder drawHLine(final StringBuilder sb, final String l, final String[] h, final String c,
            final String r, final int row, final LineType lineType) {

        for (int i = 0; i < colCount; i++) {
            if (i == 0) {
                sb.append(border(BorderStyle.LEFT, l));
            }
            else if (h[i] != null) {
                String strc = null;

                int vFill = 0;
                switch (lineType) {
                case TOP:
                case BOTTOM:
                    vFill = BorderStyle.INNER_V;
                    break;
                case HEADER:
                case HEADER_BOTTOM:
                    vFill = BorderStyle.HEADER_V;
                    break;
                case CONTENT:
                case CONTENT_BOTTOM:
                    vFill = BorderStyle.CONTENT_V;
                    break;
                }

                if (i == 1) {
                    strc = border(vFill | BorderStyle.FIRST_COL, c);
                }
                else if (i == colCount - 1) {
                    strc = border(vFill | BorderStyle.LAST_COL, c);
                }
                else {
                    strc = border(vFill, c);
                }

                if (strc.equals("")) {
                    if ((borderFill & BorderStyle.HEADER_V) > 0) {
                        // HEADER_V is on but content "c" border is empty, need to fill up with " " or "h" border
                        switch (lineType) {
                        case CONTENT:
                            strc = Utils.repeat(" ", c.length());
                            break;
                        case CONTENT_BOTTOM:
                            strc = Utils.repeatToWidth(h[0], c.length());
                            break;
                        default:
                            strc = c;
                        }
                    }
                    else if ((borderFill & BorderStyle.CONTENT_V) > 0) {
                        // CONTENT_V is on but header "c" border is empty, need to fill up with " " or "h" border
                        switch (lineType) {
                        case HEADER:
                            strc = Utils.repeat(" ", c.length());
                            break;
                        case HEADER_BOTTOM:
                            strc = Utils.repeatToWidth(h[0], c.length());
                            break;
                        default:
                            strc = c;
                        }
                    }
                }

                if (strc.length() > 0) {
                    // change to "h" border if the cell is span cell
                    if (i < data.size() - 1) {
                        if (lineType.equals(LineType.CONTENT_BOTTOM) && (colspanInvisibleSet.contains(new XY(i, row))
                                || colspanInvisibleSet.contains(new XY(i, row + 1)))) {
                            strc = Utils.repeatToWidth(h[0], c.length());
                        }
                    }

                }

                sb.append(strc);
            }

            if (h[i] != null) {
                sb.append(h[i]);
            }
        }

        sb.append(border(BorderStyle.RIGHT, r));

        return sb;
    }

    private CellStyle getCellStyle(int x, int y) {
        CellStyle cs = cellStyleMap.get(new XY(x, y));
        if (cs == null) {
            cs = getColCellStyle(x);
        }

        return cs;
    }

    private int getColspan(int x, int y) {
        if (colspanMap.size() > 0) {
            Integer i = colspanMap.get(new XY(x, y));
            return i != null ? Math.max(i.intValue(), 1) : 1;
        }
        else {
            return 1;
        }
    }

    private void fitColumnWidth() {
        int tableWidth = border.calcWidth(colCount);
        int[] minColWidth = new int[colCount];
        int[] colShrinkableWidth = new int[colCount];
        int maxShrinkWidth = Integer.MAX_VALUE;

        // XY.x = column index, XY.y = column width
        // If two column has same width, later col go first
        // e.g. column width is 0=>5, 1=>8, 2=>8, 3=>2
        // ordered will be 2,1,0,3
        Set<XY> colSortedByWidthDesc = new TreeSet<XY>(new Comparator<XY>() {
            @Override
            public int compare(XY o1, XY o2) {
                return o1.y == o2.y ? o2.x - o1.x : o2.y - o1.y;
            }
        });

        for (int i = 0; i < colCount; i++) {
            // TODO should consider min padding of all cells in the column
            minColWidth[i] = 3;

            ColumnSetting colSetting = Utils.safeGet(colSettings, i);
            if (colSetting != null) {
                if (colSetting.minWidth > 0) {
                    maxColWidth[i] = Math.max(maxColWidth[i], colSetting.minWidth);
                    minColWidth[i] = colSetting.minWidth;
                }

                if (colSetting.maxWidth > 0) {
                    maxColWidth[i] = Math.min(maxColWidth[i], colSetting.maxWidth);
                }
            }

            tableWidth += maxColWidth[i];

            colShrinkableWidth[i] = maxColWidth[i] - minColWidth[i];
            maxShrinkWidth = Math.max(maxShrinkWidth, colShrinkableWidth[i]);
            colSortedByWidthDesc.add(new XY(i, maxColWidth[i]));
        }

        if (maxWidth > 0 && maxWidth < tableWidth) {
            log.debug("Need to resize the table width from " + tableWidth + " to " + maxWidth);
            log.trace("maxShinkWidth=" + maxShrinkWidth);

            int toShrink = tableWidth - maxWidth;
            // shrinking strategy:
            // shrink the widest column until it is equal to 3rd widest column, (or 2nd of only 2 column)
            // if the width of 1st and 3rd column are same, shrink the 1st by 1 only
            // e.g. | 5 | 8 | 14 | 3 |, resize from 30 to 17
            // after 1 pass => | 5 | 8 | 5 | 3 | (width = 21)
            // after 2 pass => | 5 | 5 | 5 | 3 | (width = 20)
            // after 3 pass => | 5 | 5 | 4 | 3 | (width = 17)
            // e.g. | 5 | 8 | 14 (min 10) | 3 |, resize from 30 to 17
            // after 1 pass => | 5 | 8 | 10 | 3 | (width = 26)
            // after 2 pass => | 5 | 3 | 10 | 3 | (width = 21)
            // after 3 pass => | 3 | 3 | 10 | 3 | (width = 19)
            // after 4 pass => | 3 | 3 | 10 | 3 | (width = 18)
            // after 5 pass => | 3 | 2 | 10 | 2 | (width = 17)

            if (maxShrinkWidth > 0) {
                while (toShrink > 0 && colSortedByWidthDesc.size() > 0) {
                    Iterator<XY> it = colSortedByWidthDesc.iterator();
                    XY col1 = it.next();
                    XY col2 = it.hasNext() ? it.next() : null;
                    XY col3 = it.hasNext() ? it.next() : null;

                    // if 3 col, shrinkThisRound = 1st - 3rd
                    // if 2 col, shrinkThisRound = 1st - 2nd
                    // if 1 col, shrinkThisRound = 1st width
                    // if shrinkThisRound == 0 means all three columns are same width, then shrink 1 space
                    int shrinkThisRound = Math.max(1, col1.y - (col3 != null ? col3.y : (col2 != null ? col2.y : 0)));

                    // can't shorter than minWidth
                    shrinkThisRound = Math.min(shrinkThisRound, col1.y - minColWidth[col1.x]);
                    // won't shorter than needed
                    shrinkThisRound = Math.min(shrinkThisRound, toShrink);
                    log.trace("this round shrink col=" + col1.x + ", " + maxColWidth[col1.x] + "-" + shrinkThisRound);
                    if (shrinkThisRound == 0) {
                        // remove this column, try again until no column
                        colSortedByWidthDesc.remove(col1);
                    }
                    else {
                        maxColWidth[col1.x] -= shrinkThisRound;
                        toShrink -= shrinkThisRound;

                        colSortedByWidthDesc.remove(col1);
                        colSortedByWidthDesc.add(new XY(col1.x, maxColWidth[col1.x]));
                    }
                    log.trace("toShrink after this round=" + toShrink);
                }
            }

            if (toShrink > 0) {
                log.warn("cannot resize the table to " + maxWidth);
            }
        }
    }

    /**
     * Render the table to <code>String</code>.
     * 
     * @return the rendered string
     */
    public String renderAsString() {
        PrintWriter writer = new PrintWriter(new StringWriter());
        render(writer);
        String s = writer.toString();
        writer.close();
        return s;
    }

    /**
     * Render the table and print to <code>System.out<</code>
     */
    public void render() {
        render(new PrintWriter(System.out));
    }

    /**
     * Render the table and print to <code>PrintWriter</code>
     * 
     * @param out
     *            The <code>PrintWriter</code> to which the rendered string will print to
     */
    public void render(PrintWriter out) {
        // alias
        final BorderStyle b = border;

        StringBuilder sb = new StringBuilder();

        Map<XY, XY> colspanTextWidth = new HashMap<XY, XY>();

        // analyze data
        for (int i = 0; i < data.size(); i++) {
            Object[] row = data.get(i);
            for (int j = 0; j < row.length; j++) {
                CellStyle cs = getCellStyle(j, i);
                String text = preRenderContent(row[j], cs);
                int length = text.length() + cs.paddingLeft + cs.paddingRight;

                int colspan = getColspan(j, i);
                if (colspan > 1) {
                    // first XY:
                    // x is the last column
                    // second XY: reuse the data structure to hold
                    // x = start column
                    // y = text length
                    colspanTextWidth.put(new XY(j + colspan - 1, i), new XY(j, length));
                    j += colspan - 1;
                }
                else {
                    maxColWidth[j] = Math.max(maxColWidth[j], length);
                }
            }
        }

        // re-calc column width for colspan
        if (colspanTextWidth.size() > 0) {
            for (int i = 0; i < data.size(); i++) {
                Object[] row = data.get(i);
                for (int j = 0; j < row.length; j++) {
                    XY colspanTextLen = colspanTextWidth.get(new XY(j, i));
                    if (colspanTextLen != null) {
                        // it is the last col of a span, as we expand the last col to fit the spanned col text
                        int startCol = colspanTextLen.x;
                        int textLen = colspanTextLen.y;
                        int prevColsLen = 0;
                        for (int k = startCol; k < j; k++) {
                            prevColsLen += maxColWidth[k];
                        }
                        // offset the border
                        int needLenForLastCol = textLen - prevColsLen - (j - startCol) * b.clen;
                        maxColWidth[j] = Math.max(maxColWidth[j], needLenForLastCol);
                    }
                }
            }
        }

        fitColumnWidth();

        String[] tline = new String[colCount];
        String[] mline = new String[colCount];
        String[] hline = new String[colCount];
        String[] bline = new String[colCount];
        for (int i = 0; i < colCount; i++) {
            tline[i] = Utils.repeatToWidth(border.th, maxColWidth[i]);
            mline[i] = Utils.repeatToWidth(border.mh, maxColWidth[i]);
            hline[i] = Utils.repeatToWidth(border.hh, maxColWidth[i]);
            bline[i] = Utils.repeatToWidth(border.bh, maxColWidth[i]);
        }

        // top line
        if ((borderFill & BorderStyle.TOP) > 0) {
            drawHLine(sb, b.tl, tline, b.tc, b.tr, -1, LineType.TOP);
            out.println(sb.toString());
            sb.setLength(0);
            out.flush();
        }

        CellStyle[] cachedCellStyle = new CellStyle[colCount];
        String[] cachedContent = new String[colCount];
        String[] lineContent = new String[colCount];
        for (int i = 0; i < data.size(); i++) {
            Object[] row = data.get(i);

            for (int j = 0; j < row.length; j++) {
                CellStyle cs = cellStyleMap.get(new XY(j, i));
                if (cs == null) {
                    cs = getColCellStyle(j);
                }
                cachedCellStyle[j] = cs;

                String text = preRenderContent(row[j], cs);
                cachedContent[j] = text;
                lineContent[j] = null;
            }

            boolean multiRow = false;
            do {
                multiRow = false;
                for (int j = 0; j < cachedContent.length;) {
                    CellStyle cs = cachedCellStyle[j];
                    int maxContentWidth = 0;
                    int colspan = getColspan(j, i);
                    if (colspan > 1) {
                        for (int k = j; k < j + colspan; k++) {
                            maxContentWidth += maxColWidth[k];
                        }
                        maxContentWidth = maxContentWidth + b.clen * (colspan - 1) - cs.paddingLeft - cs.paddingRight;
                    }
                    else {
                        maxContentWidth = maxColWidth[j] - cs.paddingLeft - cs.paddingRight;
                    }

                    String text = cachedContent[j];
                    if (cachedContent[j].length() > maxContentWidth) {
                        if (cs.wrap.equals(Wrap.WRAP_WORD)) {
                            int pos = Utils.indexOfWrapWord(cachedContent[j], maxContentWidth);
                            text = cachedContent[j].substring(0, pos).trim();
                            // important to trim()
                            cachedContent[j] = cachedContent[j].substring(pos).trim();
                            multiRow = true;
                        }
                        else if (cs.wrap.equals(Wrap.WRAP)) {
                            text = cachedContent[j].substring(0, maxContentWidth);
                            cachedContent[j] = cachedContent[j].substring(maxContentWidth);
                            multiRow = true;
                        }
                        else if (cs.wrap.equals(Wrap.ELLIPSIS)) {
                            text = cachedContent[j].substring(0, maxContentWidth - cs.ellipsis.length()) + cs.ellipsis;
                            cachedContent[j] = "";
                        }
                        else {
                            // Wrap.CROP
                            text = cachedContent[j].substring(0, maxContentWidth);
                            cachedContent[j] = "";
                        }
                    }
                    else {
                        cachedContent[j] = "";
                    }

                    if (cachedContent[j].length() == 0 && cs.isAlignJustified() && !cs.isAlignJustifiedAlways()) {
                        // if it is the last line and align style is justify, prevent the line to be over-aligned
                        cs = new CellStyle(cs).setAlign(CellStyle.fallbackJustify(cs.align));
                    }
                    lineContent[j] = Utils.repeat(" ", cs.paddingLeft) + renderCell(text, cs, maxContentWidth)
                            + Utils.repeat(" ", cs.paddingRight);
                    j += colspan;
                }

                drawHLine(sb, b.l, lineContent, b.c, b.r, i, i < headerRow ? LineType.HEADER : LineType.CONTENT);
                out.println(sb.toString());
                sb.setLength(0);
                out.flush();
            } while (multiRow);

            // middle line
            if (i < data.size() - 1) {
                if (((borderFill & BorderStyle.HEADER) > 0) && i + 1 == headerRow) {
                    // header line
                    drawHLine(sb, b.hl, hline, b.hc, b.hr, i, LineType.HEADER_BOTTOM);
                    out.println(sb.toString());
                }
                else if ((borderFill & BorderStyle.INNER_H) > 0) {
                    // middle line
                    drawHLine(sb, b.ml, mline, b.mc, b.mr, i, LineType.CONTENT_BOTTOM);
                    out.println(sb.toString());
                }

                sb.setLength(0);
                out.flush();
            }
        }

        // bottom line
        if ((borderFill & BorderStyle.BOTTOM) > 0) {
            drawHLine(sb, b.bl, bline, b.bc, b.br, data.size() - 1, LineType.BOTTOM);
        }

        out.println(sb.toString());
        out.flush();
    }

    public static int guessConsoleWidth() {
        InputStream is = null;
        Scanner s = null;

        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Process p = Runtime.getRuntime().exec("cmd.exe /c mode con");
                is = p.getInputStream();
                s = new Scanner(is);
                s.useDelimiter("\\A");
                String output = s.hasNext() ? s.next() : "";
                Matcher m = Pattern.compile("Columns\\:\\s*(\\d+)").matcher(output);
                if (m.find()) {
                    return Integer.valueOf(m.group(1));
                }
            }
            else if (os.contains("mac") || os.contains("linux")) {
                Process p = Runtime.getRuntime().exec(new String[] { "bash", "-c", "tput cols 2> /dev/tty" });
                is = p.getInputStream();
                s = new Scanner(is);
                s.useDelimiter("\\A");
                String output = s.hasNext() ? s.next() : "";
                Matcher m = Pattern.compile("(\\d+)").matcher(output);
                if (m.find()) {
                    return Integer.valueOf(m.group(1));
                }
            }
        }
        catch (IOException ex) {
            ;
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException ex) {
                    ;
                }
            }
            if (s != null) {
                s.close();
            }
        }

        return -1;
    }

    public static void main(String[] args) {
        TextTable dummy = new TextTable(0);
        Object[][] data = { { "Name", "Email", "Country", "City", "Area", "Points", "Join Date", "Login Time" },
                { "Jimmy", "jimmy@gmail.com", "Hong Kong", "Hong Kong",
                        dummy.new XY((int) (Math.random() * 10) + 1, (int) (Math.random() * 10) + 1),
                        Math.random() * Math.random() * 1000000,
                        new Date((long) (new Date().getTime() - Math.random() * 100000000000d)),
                        new Timestamp((long) (new Date().getTime() - Math.random() * 1000000000d)) },
                { "Johnny", null, "United States", "New York",
                        dummy.new XY((int) (Math.random() * 10) + 1, (int) (Math.random() * 10) + 1),
                        Math.random() * Math.random() * 1000000,
                        new Date((long) (new Date().getTime() - Math.random() * 100000000000d)),
                        new Timestamp((long) (new Date().getTime() - Math.random() * 1000000000d)) },
                { "Jason", "jason@hotmail.com", "Switzerland", "Zurich",
                        dummy.new XY((int) (Math.random() * 10) + 1, (int) (Math.random() * 10) + 1),
                        Math.random() * Math.random() * 1000000,
                        new Date((long) (new Date().getTime() - Math.random() * 100000000000d)),
                        new Timestamp((long) (new Date().getTime() - Math.random() * 1000000000d)) } };

        TextTable table1 = new TextTable(data);

        // Register a formatter for Number
        table1.registerFormatter(Number.class, new TextTable.DecimalFormater("#,###.00"));
        // Register a formatter for Date
        table1.registerFormatter(Date.class, new TextTable.SimpleDateFormater("dd-MMM-yyyy"));
        // Use another Renderer for sub-type Timestamp
        table1.registerFormatter(Timestamp.class, new TextTable.SimpleDateFormater("HH:mm"));
        // Or roll you own Renderer
        table1.registerFormatter(XY.class, new TextTable.Formatter() {
            @Override
            public String format(Object obj) {
                // obj won't be null
                XY d = (XY) obj;
                return (int) d.x + "x" + (int) d.y;
            }
        });

        table1.setColumnSetting(0, 10, 0, null);
        table1.setColumnSetting(1, 0, 15, null);
        table1.setColumnSetting(2, 0, 15, null);
        table1.setColumnSetting(3, 0, 15, null);
        table1.setColumnSetting(5, 10, 0, new CellStyle().setAlign(Align.RIGHT));
        table1.setColumnSetting(7, 0, 0, new CellStyle().setAlign(Align.CENTER));

        System.out.println("DEFAULT - headerRow=1, WRAP\n");
        table1.render();

        System.out.println("\nCROP, BorderStyle INNER\n");
        table1.setDefaultCellStyle(new CellStyle().setWrap(Wrap.CROP)).setBorderFill(BorderStyle.INNER).render();

        System.out.println("\nBorderStyle OUTER\n");
        table1.setDefaultCellStyle(new CellStyle().setWrap(Wrap.CROP)).setBorderFill(BorderStyle.OUTER).render();

        System.out.println("\nV_ONLY\n");
        table1.setDefaultCellStyle(new CellStyle().setWrap(Wrap.CROP)).setBorderFill(BorderStyle.V_ONLY).render();

        System.out.println("\nELLIPSIS (default), ~INNER_H\n");
        table1.setDefaultCellStyle(new CellStyle().setWrap(Wrap.ELLIPSIS))
                .setBorderFill(BorderStyle.ALL & ~BorderStyle.INNER_H).render();

        System.out.println("\nELLIPSIS (...), H_ONLY\n");
        table1.setDefaultCellStyle(new CellStyle().setWrap(Wrap.ELLIPSIS).setEllipsis("..."))
                .setBorderFill(BorderStyle.H_ONLY).render();

        System.out.println("\nH_ONLY | FIRST_COL | LAST_COL\n");
        table1.setDefaultCellStyle(new CellStyle().setWrap(Wrap.ELLIPSIS))
                .setBorderFill(BorderStyle.H_ONLY | BorderStyle.FIRST_COL | BorderStyle.LAST_COL).render();

        System.out.println("\nH_ONLY | FIRST_COL | LAST_COL | HEADER_V\n");
        table1.setDefaultCellStyle(new CellStyle().setWrap(Wrap.ELLIPSIS))
                .setBorderFill(BorderStyle.H_ONLY | BorderStyle.FIRST_COL | BorderStyle.LAST_COL | BorderStyle.HEADER_V)
                .render();

        System.out.println("\n~HEADER_V\n");
        table1.setDefaultCellStyle(new CellStyle().setWrap(Wrap.ELLIPSIS))
                .setBorderFill(BorderStyle.ALL & ~BorderStyle.HEADER_V).render();

        System.out.println("\nHEADER\n");
        table1.setDefaultCellStyle(new CellStyle().setWrap(Wrap.ELLIPSIS)).setBorderFill(BorderStyle.HEADER).render();

        System.out.println("\nNONE\n");
        table1.setDefaultCellStyle(new CellStyle().setWrap(Wrap.ELLIPSIS)).setBorderFill(BorderStyle.NONE).render();

        System.out.println("\nAnother Example\n");
        new TextTable(3).setBorderFill(BorderStyle.NONE).setHeaderRow(0)
                .setColumnSetting(0, 0, 00, new CellStyle().setPaddingLeft(0).setPaddingRight(0))
                .setColumnSetting(1, 0, 0, new CellStyle().setPaddingLeft(1).setPaddingRight(1))
                .setColumnSetting(2, 0, 30, new CellStyle().setPaddingLeft(0).setPaddingRight(0)).add("Name").add(":")
                .add("Peter Chan").add("Gender").add(":").add("Male").add("Age").add(":").add(38).add("Nationality")
                .add(":").add("Chinese").add("Email").add(":").add("peter.chan@gmail.com").add("Language").add(":")
                .add("Basic, C, C++, Delphi, Lisp, Prolog, Smalltalk, Java, Scala, Ruby, PHP, Python, R")
                .add("Expected Salary").add(":").add("$600k per annum").render();

        TextTable table2 = new TextTable(5).setHeaderRow(2);
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 5; j++) {
                if (i == 3 && j == 1) {
                    table2.nextRow();
                    table2.nextRow();
                    ++i;
                    break;
                }
                else if (i == 2 && j == 3) {
                    table2.add("Span 3 columns very long, expand next column", null, 3);
                    j += 2;
                }
                else if (i == 5 && j == 0) {
                    table2.add("Span 3 columns very very very very very very very very long", null, 3);
                    j += 2;
                }
                else if (i == 6 && j == 0) {
                    table2.add("long enough for above col 2 & 3 not expand");
                }
                else if ((i == 7 || i == 8) && j == 1) {
                    table2.add("", null, 3);
                    j += 2;
                }
                else if (j == 2) {
                    table2.add(i * 30 + j);
                }
                else {
                    table2.add("Cell-" + j + "-" + i);
                }
            }
        }

        System.out.println("\nBasic Border\n");
        table2.setBorderStyle(BorderStyle.BASIC).render();

        System.out.println("\n~CONTENT_V\n");
        table2.setBorderStyle(BorderStyle.BASIC).setBorderFill(BorderStyle.ALL & ~BorderStyle.CONTENT_V).render();

        System.out.println("\nDEBUG Border\n");
        final BorderStyle debugBorder = new BorderStyle("tl", "t12", "tc", "trr", "hl", "h12", "hc", "hrr", "ml", "m12",
                "mc", "mrr", "bl", "b12", "bc", "brr", "l*", "c*", "*rr");
        table2.setBorderStyle(debugBorder).render();

        System.out.println("\nText Alignment\n");
        new TextTable(1).setHeaderRow(0).add("This line is align LEFT", new CellStyle().setAlign(Align.LEFT))
                .add("This line is align RIGHT", new CellStyle().setAlign(Align.RIGHT))
                .add("This line is align CENTER", new CellStyle().setAlign(Align.CENTER))
                .add("This line is align JUSTIFY_LEFT__", new CellStyle().setAlign(Align.JUSTIFY_LEFT_ALWAYS))
                .add("This line is align JUSTIFY_RIGHT_", new CellStyle().setAlign(Align.JUSTIFY_RIGHT_ALWAYS))
                .add("This line is align JUSTIFY_CENTER", new CellStyle().setAlign(Align.JUSTIFY_CENTER_ALWAYS))
                .add("Two words", new CellStyle().setAlign(Align.JUSTIFY_LEFT_ALWAYS))
                .add("Total three words", new CellStyle().setAlign(Align.JUSTIFY_LEFT_ALWAYS))
                .add("There are four words", new CellStyle().setAlign(Align.JUSTIFY_LEFT_ALWAYS))
                .add("no_space_JUSTIFY_LEFT", new CellStyle().setAlign(Align.JUSTIFY_LEFT_ALWAYS))
                .add("no_space_JUSTIFY_RIGHT", new CellStyle().setAlign(Align.JUSTIFY_RIGHT_ALWAYS))
                .add("no_space_JUSTIFY_CENTER", new CellStyle().setAlign(Align.JUSTIFY_CENTER_ALWAYS))
                .add("Only_work_on_space_`~!@#$%^&*()-=_+[]\\{}|;':\",./<>?",
                        new CellStyle().setAlign(Align.JUSTIFY_LEFT_ALWAYS))
                .add("Also works if original text already fit the         width",
                        new CellStyle().setAlign(Align.JUSTIFY_LEFT_ALWAYS))
                .add("Only one more space is added to this line 01234567890123",
                        new CellStyle().setAlign(Align.JUSTIFY_LEFT_ALWAYS))
                .add("Note  the extra space is move from first to last 01234567",
                        new CellStyle().setAlign(Align.JUSTIFY_LEFT_ALWAYS))
                .add("This line is just fit and no effect (below is empty line)",
                        new CellStyle().setAlign(Align.JUSTIFY_LEFT_ALWAYS))
                .add(null, new CellStyle().setAlign(Align.JUSTIFY_LEFT_ALWAYS)).render();

        TextTable.setLogger(new ConsoleLogger(Logger.LogLevel.TRACE));

        TextTable widthTestTable = new TextTable(4).add("1234").add("12345").add("123456").add("1234567").add("1234")
                .add("12345").add("123456").add("1234567");

        System.out.println("\nWidth Test - Original Table");
        widthTestTable.render();
        System.out.println("\nWidthText - setMaxWidth(30)\n");
        widthTestTable.setMaxWidth(30).render();
        System.out.println("\nWidthText - setMaxWidth(1)\n");
        widthTestTable.setMaxWidth(1).render();
        System.out.println("\nWidthText - setMaxWidth(1)\n");
        widthTestTable.setColumnSetting(3, 12, 0, null).setMaxWidth(30).render();

        new TextTable(2).add("Song").add("Lyrics").add("Greensleeves")
                .add("Alas, my love, you do me wrong, To cast me off discourteously."
                        + " For I have loved you well and long, Delighting in your company."
                        + " Greensleeves was all my joy Greensleeves was my delight,"
                        + " Greensleeves was my heart of gold, And who but my lady greensleeves.")
                .add("What Child Is This?")
                .add("What child is this, who, laid to rest, On Mary' lap is sleeping,"
                        + " Whom angels greet with anthems sweet. While shepherds watch are keeping?"
                        + " This, this is Christ the King, Whom shepherds guard and angels sing;"
                        + " Haste, haste to bring Him laud, The babe, the son of Mary!")
                .setColumnSetting(0, 0, 0, new CellStyle().setAlign(Align.LEFT))
                .setColumnSetting(1, 0, 0, new CellStyle().setAlign(Align.JUSTIFY_LEFT)).setMaxWidth(70).render();

        TextTable.setLogger(null);

        Utils.testWrapWord("abc def", 3);
        Utils.testWrapWord("abcdef", 3);
        Utils.testWrapWord("this is a line", 5);
        Utils.testWrapWord("this is a line", 6);
        Utils.testWrapWord("don't re-invent the wheel", 12);
        Utils.testWrapWord("don't re-invent the wheel", 8);
        Utils.testWrapWord("don't re-invent the wheel", 9);
        Utils.testWrapWord("don't re- invent the wheel", 8);
        Utils.testWrapWord("don't re- invent the wheel", 9);
        Utils.testWrapWord("don't re -invent the wheel", 8);
        Utils.testWrapWord("don't re- invent the wheel", 9);
        Utils.testWrapWord("do not wrap on comma, also do not wrap on full stop.", 20);
        Utils.testWrapWord("a b   c d", 1);
        Utils.testWrapWord("abcd", 1);
    }
}
