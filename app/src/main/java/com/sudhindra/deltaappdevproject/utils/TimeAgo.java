package com.sudhindra.deltaappdevproject.utils;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class TimeAgo {

    private static String[] outputStrChatChannel = new String[]{"y", "m", "w", "d", "h", "m", "s"};
    private static long[] minisArray = new long[]{
            DAYS.toMillis(365),
            DAYS.toMillis(30),
            DAYS.toMillis(7),
            DAYS.toMillis(1),
            HOURS.toMillis(1),
            MINUTES.toMillis(1),
            SECONDS.toMillis(1)
    };

    public static String getTimeAgo(final long date) {
        long duration = System.currentTimeMillis() - date;
        StringBuilder sb = new StringBuilder();
        int i;
        for (i = 0; i < minisArray.length - 1; i++) {
            long temp = duration / minisArray[i];
            if (temp > 0) {
                sb.append(temp)
                        .append(" ")
                        .append(outputStrChatChannel[i]);
                break;
            }
        }
        if (i == 6) return duration / 1000 + " s";
        return sb.toString();
    }
}
