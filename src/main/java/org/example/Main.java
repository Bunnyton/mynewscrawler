package org.example;

import org.example.tasks.GetLinksTask;
import org.example.tasks.TaskManager;

public class Main {
    private static final String URL = "https://habr.com/ru/news/";

    public static void main(String[] args) throws Exception {
        System.out.println("Starting after 10 s...");
        Thread.sleep(10000);

        Constants.Init();
        TaskManager tm = new TaskManager();
        tm.addTask(new GetLinksTask(URL));
        tm.start();
    }
}
