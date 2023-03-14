package eu.fbk.das.utils;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import gnu.trove.list.array.TIntArrayList;

public class Utils {

    static DateTimeFormatter sdf = DateTimeFormat.forPattern("dd/MM/yyyy");

    static DateTimeFormatter df = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm");

    static DateTimeFormatter dff = DateTimeFormat.forPattern("yyyy-MM-dd");

    static Random r = new Random(System.currentTimeMillis());

    private static final Logger log = Logger.getLogger(
            Utils.class.getName());

    public static String rmExt(String str) {
        if (str.contains("."))
            return str.substring(0, str.lastIndexOf('.'));
        else
            return str;
    }

public static double rand() {
        return r.nextDouble();
}

public static int rand(int v) {
    return r.nextInt(v);
}

    public static int index(int s, int v1, int v2) {
        if (v1 > v2) {
            int t = v1;

            v1 = v2;
            v2 = t;
        }

        int n = 0;

        for (int i = 0; i < v1; i++) {
            n += (s - i - 2);
        }
        n += (v2 - 1);
        return n;
    }

    /*
    public static int waitForProc(Process process, double timeout) {

        Worker worker = new Worker(process);
        worker.start();
        try {
            worker.join((long) timeout);
            if (worker.exit != null)
                return worker.exit;
        } catch (InterruptedException ex) {
            worker.interrupt();
            Thread.currentThread().interrupt();
            logExp(log, ex);
        } finally {
            process.destroy();
        }
        return -1;
    } */


    public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<T>(c);

        Collections.sort(list);
        return list;
    }

    public static <K, V extends Comparable> Map<K, V> sortByValues(Map<K, V> map) {
        List<Map.Entry<K, V>> entries = new LinkedList<Map.Entry<K, V>>(
                map.entrySet());

        Collections.sort(entries, new Comparator<Map.Entry<K, V>>() {

            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        // LinkedHashMap will keep the keys in the order they are inserted
        // which is currently sorted on natural ordering
        Map<K, V> sortedMap = new LinkedHashMap<K, V>();

        for (Map.Entry<K, V> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public static <K, V extends Comparable> List<K> sortByValuesList(Map<K, V> map) {
        List<Map.Entry<K, V>> entries = new LinkedList<Map.Entry<K, V>>(
                map.entrySet());

        Collections.sort(entries, new Comparator<Map.Entry<K, V>>() {

            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        // LinkedHashMap will keep the keys in the order they are inserted
        // which is currently sorted on natural ordering
        List<K> sortedList = new ArrayList<K>();

        for (Map.Entry<K, V> entry : entries) {
            sortedList.add(entry.getKey());
        }

        return sortedList;
    }

    public static boolean doubleEquals(double s, double v) {
        return doubleEquals(s, v, 5);
    }

    public static boolean doubleEquals(double s, double v, int p) {
        if (!(Math.abs(s - v) < Math.pow(2, -p))) {
            return false;
        } else {
            return true;
        }
    }

    public static <K, V extends Comparable> Map<K, V> sortInvByValues(Map<K, V> map) {

        List<Map.Entry<K, V>> entries = new LinkedList<Map.Entry<K, V>>(
                map.entrySet());

        Collections.sort(entries, new Comparator<Map.Entry<K, V>>() {

            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return -o1.getValue().compareTo(o2.getValue());
            }
        });

        // LinkedHashMap will keep the keys in the order they are inserted
        // which is currently sorted on natural ordering
        Map<K, V> sortedMap = new LinkedHashMap<K, V>();

        for (Map.Entry<K, V> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public static <K, V extends Comparable> List<K> sortInvByValuesList(Map<K, V> map) {
        List<Map.Entry<K, V>> entries = new LinkedList<Map.Entry<K, V>>(
                map.entrySet());

        Collections.sort(entries, new Comparator<Map.Entry<K, V>>() {

            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return -o1.getValue().compareTo(o2.getValue());
            }
        });

        // LinkedHashMap will keep the keys in the order they are inserted
        // which is currently sorted on natural ordering
        List<K> sortedList = new ArrayList<K>();

        for (Map.Entry<K, V> entry : entries) {
            sortedList.add(entry.getKey());
        }

        return sortedList;
    }

    public static float listMeanL(List<? extends Number> list) {
        long sum = 0;

        for (Number l : list) {
            sum += l.doubleValue();
        }
        return sum / list.size();
    }

    public static float listMeanF(List<Float> list) {
        float sum = 0;

        for (float l : list) {
            sum += l;
        }
        return sum / list.size();
    }

    public static float listMeanD(List<Double> list) {
        float sum = 0;

        for (double l : list) {
            sum += l;
        }
        return sum / list.size();
    }

    public static float listMeanInt(List<Integer> list) {
        Integer sum = 0;

        for (Integer l : list) {
            sum += l;
        }
        return sum / list.size();
    }

    /**
     * Re-print a message on the current screen line
     *
     * @param msg Useful message for the user
     */
    public static void printR(String msg) {
        System.out.print('\r' + msg);
    }

    public static long factorial(int n) {
        long fact = 1; // this  will be the result

        for (int i = 1; i <= n; i++) {
            fact *= i;
        }
        return fact;
    }

    public static String printOrdMap(Map<Integer, Integer> map) {
        String stri = "{ ";
        boolean first = true;
        List<Integer> keys = new ArrayList<Integer>(map.keySet());

        Collections.sort(keys);
        for (int key : keys) {
            if (first) {
                first = false;
            } else {
                stri += ", ";
            }

            stri += String.format("%d: %d", key, map.get(key));
        }
        stri += " }";
        return stri;
    }

    public static boolean different(TIntArrayList v1, TIntArrayList v2) {
        for (int e1 : v1.toArray()) {
            if (!v2.contains(e1)) {
                return true;
            }
        }
        for (int e2 : v2.toArray()) {
            if (!v1.contains(e2)) {
                return true;
            }
        }

        return false;
    }

    public static void logExp(Throwable trw) {
        log.error(trw.toString(), trw);
    }

    public static void logExp(Logger log, Throwable trw) {
        log.error(trw.toString(), trw);
    }

    public static String readFile(String s) throws IOException {

        File f = new File(s);

        StringBuilder fileContents = new StringBuilder((int) f.length());
        Scanner scanner = new Scanner(f);
        String lineSeparator = System.getProperty("line.separator");

        try {
            while (scanner.hasNextLine()) {
                fileContents.append(scanner.nextLine() + lineSeparator);
            }
            return fileContents.toString();
        } finally {
            scanner.close();
        }
    }

    /**
     * Get writer for the output scores
     *
     * @param ph (optional) file path
     * @return writer to use to output result
     * @throws java.io.UnsupportedEncodingException error in stream creation
     * @throws java.io.FileNotFoundException        file path not valid
     */
    public static Writer getWriter(String ph)
            throws UnsupportedEncodingException, FileNotFoundException {
        Writer writer;

        if (ph != null) {
            writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(ph), "utf-8"));
        } else {
            writer = new BufferedWriter(new OutputStreamWriter(System.out));
        }
        return writer;
    }


    public static BufferedReader getReader(String format, Object... args) throws FileNotFoundException {
        return getReader(f(format, args));
    }


    public static BufferedReader getReader(String ph) throws FileNotFoundException {
        return new BufferedReader(new FileReader(ph));
    }


    public static BufferedReader getReader(File file) {
        return getReader(file.getAbsoluteFile());
    }

    /**
     * Get directory
     *
     * @param ph dir path
     * @return directory
     * @throws java.io.UnsupportedEncodingException error in stream creation
     * @throws java.io.FileNotFoundException        file path not valid
     */
    public static File getDirectory(String ph) throws Exception {

        if (ph == null) {
            throw new Exception("No path provided: " + ph);
        }
        File f = new File(ph);

        if (!(f.exists() && f.isDirectory())) {
            throw new Exception("No valid path provided: " + ph);
        }
        return f;
    }


    public static void closeIt(Logger log, Closeable writer) {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (Exception exp) {
            logExp(log, exp);
        }
    }

    public static void checkPath(String ph) throws Exception, IOException {
        if (ph == null) {
            throw new Exception("No path provided: " + ph);
        }
        File f = new File(ph);

        if (!f.createNewFile()) {
            throw new Exception(
                    "Impossible to create path provided: " + ph);
        }
        f.delete();
    }

    public static <K, V extends Comparable<? super V>>
    SortedSet<Map.Entry<K, V>> entriesSortedByValues(Map<K, V> map) {
        return entriesSortedByValues(map, false);
    }

    public static <K, V extends Comparable<? super V>>
    SortedSet<Map.Entry<K, V>> entriesSortedByValues(Map<K, V> map, final boolean invert) {
        SortedSet<Map.Entry<K, V>> sortedEntries = new TreeSet<Map.Entry<K, V>>(
                new Comparator<Map.Entry<K, V>>() {

                    public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {
                        int res = e1.getValue().compareTo(e2.getValue());

                        if (invert) {
                            res = -res;
                        }
                        return res != 0 ? res : 1;
                    }
                });

        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }

    public static <K, V> void p(SortedSet<Map.Entry<K, V>> entries) {
        for (Map.Entry<K, V> entry : entries) {
            pf("%s - %s \n", entry.getKey().toString(),
                    entry.getValue().toString());
        }
    }

    public static <K extends Comparable<? super K>, V>
    SortedSet<Map.Entry<K, V>> entriesSortedByKey(Map<K, V> map) {
        return entriesSortedByKey(map, false);
    }

    public static <K extends Comparable<? super K>, V>
    SortedSet<Map.Entry<K, V>> entriesSortedByKey(Map<K, V> map, final boolean b) {
        SortedSet<Map.Entry<K, V>> sortedEntries = new TreeSet<Map.Entry<K, V>>(
                new Comparator<Map.Entry<K, V>>() {

                    public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {
                        int res = e1.getKey().compareTo(e2.getKey());

                        if (b) {
                            res = -res;
                        }
                        return res != 0 ? res : 1;
                    }
                });

        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }

    static public String cmd(String cmd) throws IOException {

        // p(cmd);

        Process proc = Runtime.getRuntime().exec(cmd);

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(proc.getErrorStream()));

        String s = null;
        StringBuilder out = new StringBuilder();

        while ((s = stdInput.readLine()) != null) {
            out.append(s);
        }

        // read any errors from the attempted command
        StringBuilder err = new StringBuilder();
        boolean e = false;

        while ((s = stdError.readLine()) != null) {
            err.append(s);
            System.out.println(s);
            e = true;
        }

        if (e) {
            System.out.println("errors: " + err);
        }

        return out.toString();
    }

    static public void cmd(ProcessBuilder pb, String s) throws IOException {
        pb.directory(new File(s));

        // redirect stdout, stderr, etc
        procOutput(pb.start());
    }

    static public void cmdOutput(String cmd) throws IOException {

        // read the output from the command
        System.out.println("cmd: " + cmd + "\noutput: ");

        procOutput(Runtime.getRuntime().exec(cmd));
    }

    static public void procOutput(Process proc) throws IOException {
        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(proc.getErrorStream()));

        String s = null;

        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }

        // read any errors from the attempted command
        System.out.printf("errors: ");
        StringBuilder st = new StringBuilder();
        while ((s = stdError.readLine()) != null) {
            st.append(s);
        }

        if (st.length() > 0)
            f("errors: %s \n", st.toString());
    }

    public static void dbg(Logger log, String format, Object... args) {
        log.debug(String.format(format, args));
    }

    public static void err(Logger log, String format, Object... args) {
        log.error(String.format(format, args));
    }

    public static void warn(Logger log, String format, Object... args) {
        log.warn(String.format(format, args));
    }


    public static void pf(String format, Object... args) {
        System.out.printf(format, args);
    }

    public static String f(String format, Object... args) {
        return String.format(format, args);
    }

    public static void wf(Writer writer, String format, Object... args) throws IOException {
        writer.write(String.format(format, args));
    }

    public static void p(String s) {
        System.out.println(s);
    }

    public static void p(Object s) {
        System.out.println(String.valueOf(s));
    }

    public static void p(int[] s) {
        System.out.println(Arrays.toString(s));
    }

    public static boolean exists(String s) {
        File f = new File(s);

        return f.exists() && !f.isDirectory();
    }

    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();

        if (files != null) { // some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }


    public static void copyDirectory(File src, File trg) {
        try {
            if (src.isDirectory()) {
                if (!trg.exists()) {
                    trg.mkdir();
                }

                for (File f : src.listFiles()) {
                    String s = f.getName();
                    if (f.isFile()) {
                        copySomething(new File(src, s), new File(trg, s));
                    } else {
                        copyDirectory(new File(src, s), new File(trg, s));
                    }

                }
                //              String[] children = sourceLocation.listFiles();
                //                for (int thread = 0; thread < children.length; thread++) {
                //                      copyDirectory(new File(sourceLocation, children[thread]),
                //                                new File(targetLocation, children[thread]));

            } else {

                copySomething(src, trg);
            }
        } catch (IOException ex) {
            logExp(log, ex);
        }
    }

    public static void copySomething(String f1, String f2) throws IOException {
        copySomething(new File(f1), new File(f2));
    }

    public static void copySomething(File f1, File f2) throws IOException {
        InputStream in = new FileInputStream(f1);
        OutputStream out = new FileOutputStream(f2);

        // Copy the bits from instream to outstream
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }


    public static String clean(String s) {
        return s.trim().toLowerCase();
    }

    public static void deleteDir(File file) {

        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();

    }

    public static ArrayList<String> exec(Process proc) throws IOException, InterruptedException {
        InputStream stdin = proc.getInputStream();
        InputStreamReader isr = new InputStreamReader(stdin);
        BufferedReader br = new BufferedReader(isr);

        String line = null;
        ArrayList<String> o = new ArrayList<String>();

        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.length() > 0)
                o.add(line);
        }

        int exitVal = proc.waitFor();
        // System.out.println("Process exitValue: " + exitVal);

        return o;
    }

    public static DateTime stringToDate(String s) {
            return sdf.parseDateTime(s);
    }

    public static int daysApart(DateTime d1, DateTime d2) {
        return Math.abs(Days.daysBetween(d1.toLocalDate(), d2.toLocalDate()).getDays());
    }

    public static String joinArray(double[] a, String s) {
        StringBuilder sb = new StringBuilder();
        sb.append(f("%.3f", a[0]));
        for (int i = 1; i < a.length; i++)
            sb.append(",").append(f("%.3f", a[i]));

        return sb.toString();
    }

    public static String joinArray(String[] a) {
        StringBuilder sb = new StringBuilder();
        sb.append(a[0]);
        for (int i = 1; i < a.length; i++)
            sb.append(",").append(a[i]);

        return sb.toString();
    }


    public static Date removeOneWeek(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.add(Calendar.DATE, -7);
        return cal.getTime();
    }

    public static DateTime jumpToMonday(DateTime d) {

        int v = d.getDayOfWeek() -1;
        return d.minusDays(v);
    }


    public static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    public static String formatDate(DateTime d) {
        return sdf.print(d);
    }

    public static String formatDateTime(DateTime d) {
        return df.print(d);
    }

    public static String formatDateTimeFileName(DateTime d) {
        return dff.print(d);
    }

    public static boolean equal (String s1, String s2) {
        return slug(s1).equals(slug(s2));
    }

    public static String slug(String s1) {
        return s1.toLowerCase().replace(" ", "");
    }

    public static boolean randChance(double v) {
        return (rand() <= v);
    }

    public static DateTime parseDateTime(String s) {
        return df.parseDateTime(s);
    }

    public static String printDate(DateTime d) {
        return sdf.print(d);
    }

    public static DateTime parseDate(String s) {
        return sdf.parseDateTime(s);
    }

    public static int findIndex(int arr[], int t)
    {

        int index = Arrays.binarySearch(arr, t);
        return (index < 0) ? -1 : index;
    }

    public static void logFirstStackTrace(Exception e) {
        logFirstStackTrace(e, 5);
    }

    public static void logFirstStackTrace(Exception e, int max) {
        logFirstStackTrace(e, max, null);
    }

    public static void logFirstStackTrace(Exception e, int max, StringBuilder sb) {
        ps(sb, e.toString());
        StackTraceElement[] elements = e.getStackTrace();
        for(int ix = 0; ix < elements.length && ix < max; ix++) {
            ps(sb, "     " + elements[ix]);
        }
    }

    public static void logMap(String tx, Map map) {
        logMap(tx, map, null);
    }

    public static void logMap(String tx, Map map, StringBuilder sb) {
        pfs(sb, "map: %s\n", tx);
        for (Object key: map.keySet()) {
            pfs(sb, "    %s: %s\n", key, map.get(key));
        }
    }

    public static void logSet(String tx, Set set) {
        logSet(tx, set, null);
    }

    public static void logSet(String tx, Set set, StringBuilder sb) {
        pfs(sb, "set: %s\n", tx);
        for (Object key: set.toArray()) {
            pfs(sb, "    %s\n", key);
        }
    }

    public static void ps(StringBuilder sb, String mex) {
        if  (sb != null) sb.append(mex);
        p(mex);
    }

    public static void pfs(StringBuilder sb, String format, Object... args) {
        PrintStream s = System.out.format(format, args);
        ps(sb, s.toString());
    }
}
