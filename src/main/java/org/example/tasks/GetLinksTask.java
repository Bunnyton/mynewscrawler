package org.example.tasks;

import org.example.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;


public class GetLinksTask extends Task {
    private final String url;

    public GetLinksTask(String url) {
        this.url = url;
        this.type = Type.GET_LINKS;
    }

    private String getHash(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashBytes);
    }

    private Document getDocument(String url) throws IOException {
        URL urlObj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        int statusCode = connection.getResponseCode();
        switch (statusCode) {
            case 200:
                Logger.info("HTTP 200 for URL {}", url);
                return Jsoup.parse(connection.getInputStream(), "UTF-8", url);
            case 403:
                Logger.err("HTTP 403 Forbidden: Access is denied for URL {}", url);
                throw new IOException("HTTP 403 Forbidden: Access is denied");
            case 404:
                Logger.err("HTTP 404 Not Found: The requested URL {} was not found on this server", url);
                throw new IOException("HTTP 404 Not Found: URL not found");
            case 503:
                Logger.err("HTTP 503 Service Unavailable: The server is currently unable to handle the request for URL {}", url);
                throw new IOException("HTTP 503 Service Unavailable: Service unavailable");
            default:
                Logger.err("HTTP err code: {}", statusCode);
                throw new IOException("HTTP err code: " + statusCode);
        }
    }

    public void run() {
        try {
            Document doc = getDocument(url);
            Elements posts = doc.select(".post-card-content");

            for (Element post : posts) {
                String link = new URL(url).getHost() + post.select(".post-card-title > a").attr("href");
                String hash = getHash(link);

                Logger.info("Get link({}) with hash ({})", link, hash);
                TaskManager.getInstance().addTask(new ParseTask(link, hash));
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            Logger.err("Exception: ", e);
        }
    }
}
