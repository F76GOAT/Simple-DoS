package f76goat.simpledos;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ProxyFetcher {
    private static final String PROXY_API_URL = "https://api.proxyscrape.com/v4/free-proxy-list/get?request=display_proxies&protocol=http&proxy_format=protocolipport&format=text&anonymity=Anonymous,Elite&timeout=20000";

    public static List<String> fetchProxies() throws Exception {
        URL url = new URL(PROXY_API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        List<String> proxies = new ArrayList<>();

        while ((inputLine = in.readLine()) != null) {
            proxies.add(inputLine);
        }

        in.close();
        connection.disconnect();

        return proxies;
    }

    public static void main(String[] args) {
        try {
            List<String> proxies = fetchProxies();
            for (String proxy : proxies) {
                System.out.println(proxy);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}