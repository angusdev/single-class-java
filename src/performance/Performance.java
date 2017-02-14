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
 * @version 1.0
 */
public class Performance {
    public static final int DEFAULT_SAMPLE_SIZE = 1000;
    public static final int DEFAULT_SAMPLING_INTERVAL = 1000;

    private NumberFormat numfmt;

    private LinkedList<double[]> data = new LinkedList<double[]>();
    private double total;
    private int samplingInterval;
    private long nextSamplingTime;
    private String unit;
    private int sampleSize;

    private long start;
    private int totalSamples;
    private double current;
    private double progressPercentage;
    private long ellapsed;
    private long estimated;
    private long remaining;
    private double recentAvg;
    private double overallAvg;

    public Performance(double total) {
        this(total, DEFAULT_SAMPLING_INTERVAL);
    }

    public Performance(double total, int samplingInterval) {
        this(total, samplingInterval, null);
    }

    public Performance(double total, int samplingInterval, String unit) {
        this(total, samplingInterval, unit, DEFAULT_SAMPLE_SIZE);
    }

    public Performance(double total, int samplingInterval, String unit, int sampleSize) {
        this.total = total;
        this.samplingInterval = samplingInterval;
        this.unit = unit;
        this.sampleSize = sampleSize;
        start = System.currentTimeMillis();
        nextSamplingTime = start + samplingInterval;

        numfmt = NumberFormat.getNumberInstance(Locale.US);
        numfmt.setMinimumIntegerDigits(0);
        numfmt.setGroupingUsed(false);
    }

    public boolean addSample(double count) {
        return addSample(count, false);
    }

    public boolean addSample(double count, boolean forceAdd) {
        long now = System.currentTimeMillis();

        if (!forceAdd && now < nextSamplingTime) {
            return false;
        }

        nextSamplingTime = now + samplingInterval;

        ++totalSamples;

        double[] item = { now, count };
        data.add(item);
        while (data.size() > sampleSize) {
            data.removeFirst();
        }

        calcResult(now);

        return true;
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
        current = (int) (totalSamples > 0 ? data.getLast()[1] : 0);
        ellapsed = now - start;

        if (data.size() > 0) {
            progressPercentage = Math.round(current * 10000.00 / total) / 100.0;
            long sampleMillis = (long) data.getLast()[0] - (data.size() > 1 ? (long) data.getFirst()[0] : start);
            double sampleCount = data.getLast()[1] - (data.size() > 1 ? data.getFirst()[1] : 0);
            remaining = Math.round((total - data.getLast()[1]) * 1.0 / sampleCount * sampleMillis);
            estimated = ellapsed + remaining;
            recentAvg = sampleCount * 1.0 / sampleMillis * 1000;
            overallAvg = data.getLast()[1] * 1.0 / ellapsed * 1000;
        }
    }

    public double getTotal() {
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

    public double getCurrent() {
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
        String currentStr = current < 1000000000000l ? numfmt.format(current) : (current + "");
        // String totalStr = total < 1000000000000l ? numfmt.format(total) : (total + "");
        String totalStr = total > 1000000000 ? String.format("%.2fB", total / 1000000000.0) : numfmt.format(total);

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
        return getResultDesc(prefix, unit) + ", avg:"
                + (Math.round((current < total ? recentAvg : overallAvg) * 100.0) / 100.0)
                + (unit != null ? (" " + unit) : "") + "/s";
    }

    public static void main(String[] args) {
        int size = (int) (Math.random() * 4000 + 1000);
        Performance p = new Performance(size, 1000, "KB");
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
            p.addSample(downloaded);
            System.out.println(p.getResultDescWithAvg("Downloaded"));
        }
    }
}