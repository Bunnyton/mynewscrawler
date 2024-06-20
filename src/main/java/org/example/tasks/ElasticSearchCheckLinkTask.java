package org.example.tasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.*;
import org.json.JSONObject;

import org.example.Article;

import java.io.IOException;

public class ElasticSearchCheckLinkTask extends Task {
    private final String url;
    private final String hash;
    private ElasticSearchManager esm;

    public ElasticSearchCheckLinkTask(String url, String hash) {
        this.url = url;
        this.hash = hash;
        this.type = Type.ELASTIC_SEARCH_CHECK_LINK;
    }

    public ElasticSearchCheckLinkTask(String url, String hash, ElasticSearchManager esm) {
        this.url = url;
        this.hash = hash;
        this.type = Type.ELASTIC_SEARCH_CHECK_LINK;
        this.esm = esm;
    }

    @Override
    public void run() {
        try {
            if (!esm.checkLinkExist(hash)) {
                TaskManager.getInstance().addTask(new ParseTask(url, hash));
            }
        } catch (IOException e) {
            throw new RuntimeException("Can't get info from ElasticSearch" + e);
        }
    }
}
