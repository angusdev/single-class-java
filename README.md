# single-class-java
Collection of single class java utilities, just copy to your project to use it.

## FindClasses
Find the class / package in classpath and the directory or jar file of the class file.
<pre>
SubTypePredicate("java.sql.Driver")
oracle.jdbc.OracleDriver (C:\.m2\repository\com\oracle\ojdbc6\11.2.0.3\ojdbc6-11.2.0.3.jar)
oracle.jdbc.driver.OracleDriver (C:\.m2\repository\com\oracle\ojdbc6\11.2.0.3\ojdbc6-11.2.0.3.jar)

PackagePredicate("oracle", true) and ClassnamePredicate("\\d+$")
oracle.jdbc.driver.CRC64 (C:\.m2\repository\com\oracle\ojdbc6\11.2.0.3\ojdbc6-11.2.0.3.jar)
oracle.jdbc.driver.Message11 (C:\.m2\repository\com\oracle\ojdbc6\11.2.0.3\ojdbc6-11.2.0.3.jar)
oracle.jdbc.oracore.OracleTypeSINT32 (C:\.m2\repository\com\oracle\ojdbc6\11.2.0.3\ojdbc6-11.2.0.3.jar)
oracle.net.ns.Message11 (C:\.m2\repository\com\oracle\ojdbc6\11.2.0.3\ojdbc6-11.2.0.3.jar)
oracle.sql.CharacterSetAL16UTF16 (C:\.m2\repository\com\oracle\ojdbc6\11.2.0.3\ojdbc6-11.2.0.3.jar)
oracle.sql.CharacterSetAL32UTF8 (C:\.m2\repository\com\oracle\ojdbc6\11.2.0.3\ojdbc6-11.2.0.3.jar)
oracle.sql.CharacterSetGB18030 (C:\.m2\repository\com\oracle\ojdbc6\11.2.0.3\ojdbc6-11.2.0.3.jar)
</pre>

## TextTable
This class is to render fixed character table with support of cell wrapping, alignment, padding, border style, column width. The target to output table-based information in console program nicely.
<pre>
+-----------------------------------------------------------------------------------------------------+
| Name     | Email         | Country       | City      | Area |     Points | Join Date   | Login Time | 
|=====================================================================================================|
| Jimmy    | jimmy@gmai... | Hong Kong     | Hong Kong | 7x9  |  76,348.48 | 09-Aug-2012 |   17:37    |
|----------+---------------+---------------+-----------+------+------------+-------------+------------|
| Johnny   |               | United States | New York  | 5x9  |  13,905.24 | 17-Jun-2013 |   18:19    |
|----------+---------------+---------------+-----------+------+------------+-------------+------------|
| Jason    | jason@hotm... | Switzerland   | Zurich    | 3x8  | 620,912.23 | 25-Jan-2012 |   18:08    |
+-----------------------------------------------------------------------------------------------------+
</pre>

## WordWrap
Wrap a long string to multiple lines.

## SimpleCrypt
Simple encryption functions for MD5, AES.
```java
public static void main(String[] args) throws Exception {
    String t = "someplain";
    String m = md5(t);
    String s = SimpleCrypt.saltedMd5(t, ENCRYPT_KEY);
    String a = SimpleCrypt.aesEncrypt(t, ENCRYPT_KEY);
    String u = SimpleCrypt.aesDecrypt(a, ENCRYPT_KEY);
    System.out.println("plain         :" + t);
    System.out.println("md5           :" + m);
    System.out.println("salted md5    :" + s);
    System.out.println("matched md5   :" + SimpleCrypt.isMatchMD5(t, s, ENCRYPT_KEY));
    System.out.println("unmatched md5 :" + SimpleCrypt.isMatchMD5(t, s + "0", ENCRYPT_KEY));
    System.out.println("aes encrypted :" + a);
    System.out.println("aes decrypted :" + u);
    System.out.println("aes matched   :" + t.equals(u));
}
```
<pre>
plain         :someplain
md5           :68804d9e54ae99c9dde25dfb3a485475
salted md5    :afb1a4000f74fabf8d8ac3a251730c6e
matched md5   :true
unmatched md5 :false
aes encrypted :6dee4ca2b78abf5305744e0272f7ce68
aes decrypted :someplain
aes matched   :true
</pre>

## Performance
Simple performance monitoring and reporting.  

<pre>
Downloaded 495/3936 KB (12.58%), ellapsed: 0:01, estimated: 0:08, remaining: 0:07, avg:474.59 KB/s
Downloaded 718/3936 KB (18.24%), ellapsed: 0:02, estimated: 0:16, remaining: 0:14, avg:222.11 KB/s
Downloaded 1131/3936 KB (28.73%), ellapsed: 0:03, estimated: 0:11, remaining: 0:08, avg:317.37 KB/s
Downloaded 1138/3936 KB (28.91%), ellapsed: 0:04, estimated: 0:17, remaining: 0:13, avg:214.05 KB/s
Downloaded 1311/3936 KB (33.31%), ellapsed: 0:05, estimated: 0:17, remaining: 0:12, avg:203.8 KB/s
Downloaded 1386/3936 KB (35.21%), ellapsed: 0:06, estimated: 0:20, remaining: 0:14, avg:178.06 KB/s
Downloaded 1712/3936 KB (43.5%), ellapsed: 0:07, estimated: 0:18, remaining: 0:10, avg:202.7 KB/s
Downloaded 2084/3936 KB (52.95%), ellapsed: 0:08, estimated: 0:16, remaining: 0:08, avg:226.87 KB/s
Downloaded 2224/3936 KB (56.5%), ellapsed: 0:09, estimated: 0:16, remaining: 0:07, avg:216.02 KB/s
Downloaded 2628/3936 KB (66.77%), ellapsed: 0:10, estimated: 0:15, remaining: 0:05, avg:236.89 KB/s
Downloaded 2951/3936 KB (74.97%), ellapsed: 0:11, estimated: 0:15, remaining: 0:04, avg:245.5 KB/s
Downloaded 3127/3936 KB (79.45%), ellapsed: 0:12, estimated: 0:15, remaining: 0:03, avg:239.19 KB/s
Downloaded 3604/3936 KB (91.57%), ellapsed: 0:13, estimated: 0:14, remaining: 0:01, avg:259.0 KB/s
Downloaded 3684/3936 KB (93.6%), ellapsed: 0:14, estimated: 0:15, remaining: 0:01, avg:245.23 KB/s
Downloaded 3926/3936 KB (99.75%), ellapsed: 0:15, estimated: 0:15, remaining: 0:00, avg:245.0 KB/s
Downloaded 3936/3936 KB (100.0%), ellapsed: 0:16, avg:245.28 KB/s
</pre>

## SCLog
Single Class Logger without compile time dependency.

## JSQLPlus
Command line SQL Plus like tools requires JDBC driver only.
