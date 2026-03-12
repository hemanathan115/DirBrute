package com.dirbrute;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DirBrute - Directory Brute Force Tool
 * Discovers hidden directories on web servers using wordlists and multithreading.
 */
public class DirBrute {

    // ANSI color codes
    static final String RESET  = "\u001B[0m";
    static final String GREEN  = "\u001B[32m";
    static final String YELLOW = "\u001B[33m";
    static final String RED    = "\u001B[31m";
    static final String CYAN   = "\u001B[36m";
    static final String BOLD   = "\u001B[1m";

    private final String baseUrl;
    private final String wordlistPath;
    private final int threads;
    private final int timeout;
    private final AtomicInteger scanned  = new AtomicInteger(0);
    private final AtomicInteger found    = new AtomicInteger(0);
    private final List<String> results   = Collections.synchronizedList(new ArrayList<>());

    public DirBrute(String baseUrl, String wordlistPath, int threads, int timeout) {
        this.baseUrl      = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.wordlistPath = wordlistPath;
        this.threads      = threads;
        this.timeout      = timeout;
    }

    public void run() throws Exception {
        printBanner();
        List<String> words = loadWordlist(wordlistPath);
        System.out.println(CYAN + "[*] Target   : " + RESET + baseUrl);
        System.out.println(CYAN + "[*] Wordlist : " + RESET + wordlistPath + " (" + words.size() + " entries)");
        System.out.println(CYAN + "[*] Threads  : " + RESET + threads);
        System.out.println(CYAN + "[*] Timeout  : " + RESET + timeout + "ms");
        System.out.println(CYAN + "[*] Starting scan...\n" + RESET);

        long start = System.currentTimeMillis();
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (String word : words) {
            if (word.isBlank() || word.startsWith("#")) continue;
            String path = word.trim();
            pool.submit(() -> checkPath(path));
        }

        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.HOURS);

        long elapsed = System.currentTimeMillis() - start;
        printSummary(words.size(), elapsed);
    }

    private void checkPath(String path) {
        String urlStr = baseUrl + path;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) DirBrute/1.0");

            int code = conn.getResponseCode();
            scanned.incrementAndGet();
            conn.disconnect();

            if (code == 200) {
                String msg = GREEN + "[200 FOUND]   " + RESET + urlStr;
                System.out.println(msg);
                results.add("[200] " + urlStr);
                found.incrementAndGet();
            } else if (code == 301 || code == 302) {
                String location = conn.getHeaderField("Location");
                String msg = YELLOW + "[" + code + " REDIRECT] " + RESET + urlStr
                           + YELLOW + " -> " + location + RESET;
                System.out.println(msg);
                results.add("[" + code + "] " + urlStr + " -> " + location);
                found.incrementAndGet();
            } else if (code == 403) {
                String msg = YELLOW + "[403 FORBIDDEN] " + RESET + urlStr;
                System.out.println(msg);
                results.add("[403] " + urlStr);
                found.incrementAndGet();
            }
            // 404 and others are silently ignored

        } catch (Exception ignored) {
            scanned.incrementAndGet();
        }
    }

    private List<String> loadWordlist(String path) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) lines.add(line);
        }
        return lines;
    }

    private void printBanner() {
        System.out.println(BOLD + CYAN);
        System.out.println("  ____  _      ____             _       ");
        System.out.println(" |  _ \\(_)_ __| __ ) _ __ _   _| |_ ___ ");
        System.out.println(" | | | | | '__|  _ \\| '__| | | | __/ _ \\");
        System.out.println(" | |_| | | |  | |_) | |  | |_| | ||  __/");
        System.out.println(" |____/|_|_|  |____/|_|   \\__,_|\\__\\___|");
        System.out.println(RESET);
        System.out.println(BOLD + "  Directory Brute Force Tool  v1.0" + RESET);
        System.out.println("  For authorized security testing only.\n");
    }

    private void printSummary(int total, long elapsed) {
        System.out.println("\n" + CYAN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + RESET);
        System.out.println(BOLD + " Scan Complete" + RESET);
        System.out.println(CYAN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + RESET);
        System.out.println(" Total checked : " + scanned.get() + " / " + total);
        System.out.println(" Interesting   : " + GREEN + found.get() + RESET);
        System.out.printf(" Time elapsed  : %.2fs%n", elapsed / 1000.0);
        if (!results.isEmpty()) {
            System.out.println("\n" + BOLD + " Results:" + RESET);
            results.forEach(r -> System.out.println("  " + r));
        }
        System.out.println(CYAN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + RESET);
    }

    // ─── Entry point ────────────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java -jar DirBrute.jar <url> <wordlist> [threads] [timeout_ms]");
            System.out.println("Example: java -jar DirBrute.jar http://example.com wordlists/common.txt 20 3000");
            System.exit(1);
        }

        String url      = args[0];
        String wordlist = args[1];
        int threads     = args.length >= 3 ? Integer.parseInt(args[2]) : 20;
        int timeout     = args.length >= 4 ? Integer.parseInt(args[3]) : 3000;

        new DirBrute(url, wordlist, threads, timeout).run();
    }
}
