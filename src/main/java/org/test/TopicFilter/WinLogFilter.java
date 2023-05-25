package org.test.TopicFilter;

public class WinLogFilter {
    public static boolean filter(String message) {

        String[] winLogfilter = {
                "cdd31c3f-3b24-4526-848a-c8c2921f5eac"
        };

        boolean winMatchStatus = false;

        for (String artifact : winLogfilter) {
            if (message.contains(artifact)) {
                winMatchStatus  = true;
                break;
            }
        }
        return winMatchStatus;
    }
}

