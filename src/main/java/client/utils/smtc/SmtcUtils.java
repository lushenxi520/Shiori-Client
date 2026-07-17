package client.utils.smtc;

import dsj.smtc.SmtcLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SmtcUtils {

    private static final long SMTC_TIMEOUT_MS = 2000;
    private static boolean initializationFailed = false;

    public static class SmtcInfo {
        public final String title;
        public final long position;
        public final long duration;
        public final String base64Cover;

        public SmtcInfo(String title, long position, long duration, String base64Cover) {
            this.title = title;
            this.position = position;
            this.duration = duration;
            this.base64Cover = base64Cover;
        }

        public boolean isPlaying() {
            return title != null && !title.isEmpty() && !"No media".equals(title);
        }

        public String getFormattedPosition() {
            if (position <= 0) return "00:00";
            return String.format("%02d:%02d", position / 60, position % 60);
        }

        public String getFormattedDuration() {
            if (duration <= 0) return "??:??";
            return String.format("%02d:%02d", duration / 60, duration % 60);
        }

        public String getProgressBar(int barLength) {
            if (duration <= 0) {
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < barLength; i++) {
                    sb.append("-");
                }
                sb.append("] (unknown duration)");
                return sb.toString();
            }
            int filled = (int) ((double) position / duration * barLength);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < barLength; i++) {
                sb.append(i < filled ? "#" : "-");
            }
            sb.append("]");
            return sb.toString();
        }
    }

    public static String getRawInfo() {
        if (initializationFailed) {
            return "DLL not loaded";
        }

        try {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                return SmtcLoader.getSmtcInfo();
            });

            return future.get(SMTC_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            initializationFailed = true;
            return "Error: " + e.getMessage();
        }
    }

    public static SmtcInfo getCurrentInfo() {
      try {
         CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            return SmtcLoader.getSmtcInfo();
         });

            String raw = future.get(SMTC_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (raw == null) return null;

            String[] parts = raw.split("\\|", -1);
            if (parts.length < 4) return null;

            String title = parts[0];
            if ("No media".equals(title)) return null;

            long position = 0;
            long duration = 0;
            try {
                position = Long.parseLong(parts[1]);
                duration = Long.parseLong(parts[2]);
            } catch (NumberFormatException ignored) {}

            String base64 = parts.length > 3 ? parts[3] : "";

            return new SmtcInfo(title, position, duration, base64);
        } catch (Exception e) {
            System.err.println("[Shiori-SMTC] Error getting SMTC info: " + e.getMessage());
            return null;
         }
    }
}