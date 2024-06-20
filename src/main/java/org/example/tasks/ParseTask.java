package org.example.tasks;

import org.example.Article;
import org.example.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;


public class ParseTask extends Task {
    private final String url;
    private final String hash;

    public ParseTask(String url, String hash) {
        this.url = url;
        this.hash = hash;
        this.type = Type.PARSE_PAGE;
    }

    private Article ParseArticle() {
        try {
            URL urlObj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            int statusCode = conn.getResponseCode();

            switch (statusCode) {
                case 200:
                    Article article = new Article();

                    Document doc = Jsoup.parse(conn.getInputStream(), "UTF-8", url);
                    article.setTitle(doc.select(".post-card-content.post-full-title span").text().trim());
                    article.setAuthor(doc.select(".post-card-content.author-list-item a.meta-item").text().trim());
                    article.setTime(doc.select(".post-card-content.time.meta-time").attr("datetime"));
                    article.setHash(hash);

                    StringBuilder text = new StringBuilder();
                    doc.select(".post-card-content.post-full-content p").forEach(paragraph -> text.append(paragraph.text()).append("\n"));
                    article.setText(text.toString());

                    return article;
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
        } catch (IOException e) {
            Logger.err("Error parsing article from URL: " + url, e);
        }
        return null;
    }

    @Override
    public void run() {
        try {
            if (!TaskManager.getInstance().getESM().checkLinkExist(hash)) {
                Article article = ParseArticle();
                if (article != null) {
                    TaskManager.getInstance().getESM().addDocument(article);
                } else {
                    throw new RuntimeException("Can't parse document - " + url + " hash(" + hash + ")");
                }
            }
        } catch (IOException e) {
            Logger.err("ParseTask failed: ", e);
        }
    }
}
