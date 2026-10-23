package cn.g.clear;

import java.util.Locale;
import java.util.regex.Pattern;

/** Pure decision rules: no screen text is persisted. */
public final class Rules {
    private static final Pattern SKIP = Pattern.compile("(?:跳过(?:广告)?|skip(?:\\s+ad)?)(?:\\s*[（(]?\\s*\\d{1,2}\\s*(?:s|秒)?\\s*[）)]?)?|\\d{1,2}\\s*(?:s|秒)?\\s*(?:跳过(?:广告)?|skip(?:\\s+ad)?)");
    public static boolean skip(String text) {
        if (text == null || text.length() > 32) return false;
        return SKIP.matcher(text.trim().toLowerCase(Locale.ROOT)).matches();
    }
    public static boolean ad(String text) {
        return text != null && (text.trim().equals("广告") || text.trim().equalsIgnoreCase("advertisement") || text.trim().equalsIgnoreCase("ad"));
    }
    public static boolean corner(int left, int top, int right, int bottom, int width, int height) {
        return width > 0 && height > 0 && right > left && bottom > top && left >= width * .50 && top >= 0
            && bottom <= height * .35 && right <= width && right-left <= width * .48 && bottom-top <= height * .15;
    }
    public static final class Session {
        private String foreground = "", armed = "";
        private long entered, armedUntil, lastAction = -10000;
        private boolean acted;
        /** Only call on a verified active application root, never a background event package. */
        public String enter(String pkg, long now, boolean boundary, boolean mayReturn) {
            if (pkg.equals(foreground)) return null;
            String back = !boundary && mayReturn && !armed.isEmpty() && !pkg.equals(armed) && now < armedUntil ? armed : null;
            foreground = pkg; entered = now; acted = false; armed = ""; armedUntil = 0;
            return back;
        }
        public boolean scanning(long now) { return !acted && now >= entered && now-entered <= 12000 && now-lastAction >= 1800; }
        public void evidence(String pkg, long now) { armed=pkg; armedUntil=now+3500; }
        public void clearEvidence() { armed=""; armedUntil=0; }
        public void action(long now) { acted=true; lastAction=now; }
        public void reset() { foreground=""; clearEvidence(); acted=true; }
    }
}
