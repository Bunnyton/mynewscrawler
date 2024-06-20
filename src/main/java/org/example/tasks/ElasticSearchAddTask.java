package org.example.tasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.*;
import org.json.JSONObject;

import org.example.Article;

public class ElasticSearchAddTask extends Task {
    private ElasticSearchManager esm;
    private final String articlestr;

    public ElasticSearchAddTask(String articlestr) {
        this.articlestr = articlestr;
        this.type = Type.ELASTIC_SEARCH_ADD;
    }

    public ElasticSearchAddTask(String articlestr, ElasticSearchManager esm) {
        this.articlestr = articlestr;
        this.esm = esm;
        this.type = Type.ELASTIC_SEARCH_ADD;
    }

    public String getArticleJsonStr() {
        return articlestr;
    }

    @Override
    public void run() {
        try {
            Article art = Article.fromJsonString(new JSONObject(articlestr));
            esm.addDocument(art);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Can't save document to ElasticSearch");
        }
    }
}
