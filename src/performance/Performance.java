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
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.Locale;

/**
 * Simple performance monitoring and reporting.
 * 
 * @author http://twitter.com/angusdev
 * @version 20170228
 */
public class Performance {
    public static final int DEFAULT_SAMPLE_SIZE = 1000;
    public static final int DEFAULT_SAMPLING_INTERVAL = 1000;

    private NumberFormat numfmt;

    private LinkedList<long[]> data = new LinkedList<long[]>();
    private long total;
    private int samplingInterval;
    private long nextSamplingTime;
    private String unit;
    private int sampleSize;

    private long start;
    private int totalSamples;
    private long latestSample;
    private long current;
    private double progressPercentage;
    private long ellapsed;
    private long estimated;
    private long remaining;
    private double recentAvg;
    private double overallAvg;

    public Performance(long total) {
        this(total, DEFAULT_SAMPLING_INTERVAL);
    }

    public Performance(long total, String unit) {
        this(total, unit, DEFAULT_SAMPLING_INTERVAL);
    }

    public Performance(long total, int samplingInterval) {
        this(total, null, samplingInterval, DEFAULT_SAMPLE_SIZE);
    }

    public Performance(long total, String unit, int samplingInterval) {
        this(total, unit, samplingInterval, DEFAULT_SAMPLE_SIZE);
    }

    public Performance(long total, String unit, int samplingInterval, int sampleSize) {
        this.total = total;
        this.samplingInterval = samplingInterval;
        this.unit = unit;
        this.sampleSize = sampleSize;
        start = System.currentTimeMillis();
        nextSamplingTime = start + samplingInterval;

        numfmt = NumberFormat.getNumberInstance(Locale.US);
        numfmt.setMinimumIntegerDigits(0);
        numfmt.setMinimumFractionDigits(2);
        numfmt.setMaximumFractionDigits(2);
        numfmt.setGroupingUsed(false);
    }

    public boolean inc() {
        return inc(1);
    }

    public boolean inc(int inc) {
        latestSample += inc;
        return addSample(latestSample, false);
    }

    public synchronized boolean incSynchronized() {
        return inc();
    }

    public synchronized boolean incSynchronized(int inc) {
        return inc(inc);
    }

    public boolean addSample(long count) {
        return addSample(count, false);
    }

    public Performance addSampleAlways(long count) {
        addSample(count, true);
        return this;
    }

    public synchronized Performance addSampleAlwaysSynchronized(long count) {
        return addSampleAlways(count);
    }

    public boolean addSample(long count, boolean forceAdd) {
        long now = System.currentTimeMillis();

        latestSample = count;

        if (!forceAdd && now < nextSamplingTime) {
            return false;
        }

        nextSamplingTime = now + samplingInterval;

        ++totalSamples;

        long[] item = { now, count };
        data.add(item);
        while (data.size() > sampleSize) {
            data.removeFirst();
        }

        calcResult(now);

        return true;
    }

    public synchronized boolean addSampleSynchronized(long count, boolean forceAdd) {
        return addSample(count, forceAdd);
    }

    public String millisToStr(long m) {
        m = Math.max(0, m);

        long sec = (m / 1000) % 60;
        long min = (m / 1000 / 60) % 60;
        long hour = m / 1000 / 60 / 60;
        long day = m / 1000 / 60 / 60 / 24;

        if (m > 48 * 3600 * 1000) {
            if (hour % 24 > 0) {
                ++day;
            }
            return day + " day" + (day > 1 ? "s" : "");
        }
        else {
            String hourStr = ((day > 0 || hour > 0) ? hour + ":" : "");
            String minStr = ((min < 10 && hour > 0 ? "0" : "") + min);
            String secStr = ":" + (sec < 10 ? "0" : "") + sec;

            return hourStr + minStr + secStr;
        }
    }

    private void calcResult(long now) {
        if (data.size() == 0) {
            return;
        }

        long[] first = data.getFirst();
        long[] last = data.getLast();

        current = (int) (totalSamples > 0 ? last[1] : 0);
        ellapsed = now - start;

        progressPercentage = Math.round(current * 10000.00 / total) / 100.0;
        long sampleMillis = last[0] - (data.size() > 1 ? first[0] : start);
        long sampleCount = last[1] - (data.size() > 1 ? first[1] : 0);
        remaining = Math.round((total - last[1]) * 1.0 / sampleCount * sampleMillis);
        estimated = ellapsed + remaining;
        recentAvg = sampleCount * 1.0 / sampleMillis * 1000;
        overallAvg = last[1] * 1.0 / ellapsed * 1000;
    }

    public long getTotal() {
        return total;
    }

    public String getUnit() {
        return unit;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public long getStart() {
        return start;
    }

    public int getTotalSamples() {
        return totalSamples;
    }

    public long getCurrent() {
        return current;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public long getEllapsed() {
        return ellapsed;
    }

    public long getEstimated() {
        return estimated;
    }

    public long getRemaining() {
        return remaining;
    }

    public double getRecentAvg() {
        return recentAvg;
    }

    public double getOverallAvg() {
        return overallAvg;
    }

    public String getResultDesc() {
        return getResultDesc(null);
    }

    public String getResultDesc(String prefix) {
        return getResultDesc(prefix, unit);
    }

    private String getResultDesc(String prefix, String unit) {
        // String currentStr = current < 1000000000000l ? numfmt.format(current) : (current + "");
        String currentStr = current + "";
        // String totalStr = total < 1000000000000l ? numfmt.format(total) : (total + "");
        String totalStr = total > 1000000000 ? String.format("%.2fB", total / 1000000000.0) : ("" + total);

        StringBuilder sb = new StringBuilder();
        sb.append(prefix != null ? (prefix + " ") : "").append(currentStr).append("/").append(totalStr)
                .append(unit != null ? (" " + unit) : "").append(" (").append(progressPercentage)
                .append("%), ellapsed: ").append(millisToStr(ellapsed));
        if (current < total) {
            sb.append(", estimated: ").append(millisToStr(estimated)).append(", remaining: ")
                    .append(millisToStr(remaining));
        }

        return sb.toString();
    }

    public String getResultDescWithAvg() {
        return getResultDescWithAvg(null);
    }

    public String getResultDescWithAvg(String prefix) {
        return getResultDescWithAvg(prefix, unit);
    }

    private String getResultDescWithAvg(String prefix, String unit) {
        double avg = (Math.round((current < total ? recentAvg : overallAvg) * 100.0) / 100.0);
        String avgStr = avg > 1000000000 ? String.format("%.2fB", avg / 1000000000.0) : numfmt.format(avg);
        return getResultDesc(prefix, unit) + ", avg:" + avgStr + (unit != null ? (" " + unit) : "") + "/s";
    }

    public static void main(String[] args) {
        Performance p = new Performance(10, 0);
        for (int i = 1; i <= 10; i++) {
            System.out.println(p.addSampleAlways(i).getResultDescWithAvg());
        }

        // test multi-thread
        System.out.println();
        System.out.println("-----------------------------");
        System.out.println("Multi Thread with addSample()");
        System.out.println("-----------------------------");
        System.out.println();
        final Thread[] threads = new Thread[10];
        final Performance pt = new Performance(1000000 * 10, 0);
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (long i = 1; i <= 1000000; i++) {
                        try {
                            pt.addSample(i, false);
                        }
                        catch (Exception ex) {
                            System.out.println(ex.getClass().getName());
                            return;
                        }
                    }
                }
            });
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            try {
                threads[i].join();
            }
            catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        System.out.println();
        System.out.println("-----------------------------------------");
        System.out.println("Multi Thread with addSampleSynchronized()");
        System.out.println("-----------------------------------------");
        System.out.println();
        Thread[] threads2 = new Thread[10];
        final Performance pt2 = new Performance(1000000 * 10, 0);
        for (int i = 0; i < threads.length; i++) {
            threads2[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (long i = 1; i <= 1000000; i++) {
                        pt2.addSampleSynchronized(i, false);
                    }
                }
            });
        }
        for (int i = 0; i < threads.length; i++) {
            threads2[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            try {
                threads2[i].join();
            }
            catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        System.out.println("No exception");

        System.out.println();
        System.out.println("--------------");
        System.out.println("Download Meter");
        System.out.println("--------------");
        System.out.println();
        int size = (int) (Math.random() * 4000 + 1000);
        p = new Performance(size, "KB", 1000);
        int downloaded = 0;
        while (downloaded < size) {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException ex) {
                ;
            }
            downloaded += (int) (Math.random() * 500);
            downloaded = Math.min(downloaded, size);
            p.addSample(downloaded, true);
            System.out.println(p.getResultDescWithAvg("Downloaded"));
        }

        System.out.println();
        System.out.println("---------------------------");
        System.out.println("From 0 to Integer.MAX_VALUE");
        System.out.println("---------------------------");
        System.out.println();
        p = new Performance(Integer.MAX_VALUE, 1000);
        for (long i = 0; i < Integer.MAX_VALUE; i++) {
            if (p.addSample(i)) {
                System.out.println(p.getResultDescWithAvg());
            }
        }
        p.addSample(Integer.MAX_VALUE, true);
        System.out.println(p.getResultDescWithAvg());

        System.out.println();
        System.out.println("----------------------------------------------------");
        System.out.println("From 0 to Long.MAX_VALUE (stop at Integer.MAX_VALUE)");
        System.out.println("----------------------------------------------------");
        System.out.println();
        p = new Performance(Long.MAX_VALUE, 1000);
        for (long i = 0; i < Integer.MAX_VALUE; i++) {
            if (p.addSample(i)) {
                System.out.println(p.getResultDescWithAvg());
            }
        }
        p.addSample(Integer.MAX_VALUE, true);
        System.out.println(p.getResultDescWithAvg());
    }
}
