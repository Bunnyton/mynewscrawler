package org.example.tasks;

import com.rabbitmq.client.*;
import org.example.Article;
import org.example.Constants;
import org.example.ElasticSearchManager;
import org.example.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static java.lang.System.exit;


public class TaskManager {

    private ExecutorService es; // FIXME final
    private ElasticSearchManager esm; // FIXME final

    private long lastMessageTime;

    public TaskManager() {
        try {
            if (instance == null) {
                rabbitmqInit();
                esm = new ElasticSearchManager();
                esm.init();
                es = Executors.newFixedThreadPool(Constants.POOL_COUNT);
                instance = this;
            }
        } catch (IOException e) {
            throw new RuntimeException("TaskManager start failed: " + e);
        }
    }

    public static TaskManager instance;

    public static TaskManager getInstance() {
        if (instance == null) {
            instance = new TaskManager();
        }
        return instance;
    }

    private ConnectionFactory createConnection() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(Constants.RABBITMQ_HOST);
        factory.setPort(Constants.RABBITMQ_PORT);
        factory.setUsername(Constants.RABBITMQ_USER);
        factory.setPassword(Constants.RABBITMQ_PASS);
        return factory;
    }

    private void rabbitmqInit() {
        var factory = createConnection();
        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {

            boolean durable = true;
            channel.exchangeDeclare(Constants.RABBITMQ_EXCHANGE_NAME, "direct", durable);
            channel.queueDeclare(Constants.RABBITMQ_QUEUE_NAME, durable, false, false, null);

            for (Task.Type tasktype : Task.Type.values()) {
                channel.queueBind(Constants.RABBITMQ_QUEUE_NAME, Constants.RABBITMQ_EXCHANGE_NAME, tasktype.toString());
            }
        } catch (IOException | TimeoutException e) {
            Logger.err("Initialing RabbitMQ failed: %s", e.toString());
            exit(1);
        }
    }

    private int getMessageCount(Channel channel, String queueName) throws IOException {
        AMQP.Queue.DeclareOk declareOk = channel.queueDeclarePassive(queueName);
        return declareOk.getMessageCount();
    }

    public void start() {
        var factory = createConnection();
        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {

            DefaultConsumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String message = new String(body, "UTF-8");
                    long deliveryTag = envelope.getDeliveryTag();
                    String receivedRoutingKey = envelope.getRoutingKey();
                    lastMessageTime = System.currentTimeMillis();

                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        JsonNode node = objectMapper.readTree(message);

                        if (receivedRoutingKey.equals(Task.Type.GET_LINKS.toString())) {
                            es.submit(new GetLinksTask(node.get("url").toString()));
                        } else if (receivedRoutingKey.equals(Task.Type.PARSE_PAGE.toString())) {
                            es.submit(new ParseTask(node.get("link").toString(), node.get("hash").toString()));
                        }
                        channel.basicAck(deliveryTag, false);
                    } catch (Exception e) {
                        Logger.logException(e);
                        channel.basicReject(deliveryTag, true);
                    }

                }
            };

            DefaultConsumer consumerESM = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String message = new String(body, "UTF-8");
                    long deliveryTag = envelope.getDeliveryTag();
                    String receivedRoutingKey = envelope.getRoutingKey();
                    lastMessageTime = System.currentTimeMillis();

                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        JsonNode node = objectMapper.readTree(message);

                        if (receivedRoutingKey.equals(Task.Type.GET_LINKS.toString())) {
                            es.submit(new GetLinksTask(node.get("url").toString()));
                        } else if (receivedRoutingKey.equals(Task.Type.PARSE_PAGE.toString())) {
                            es.submit(new ParseTask(node.get("link").toString(), node.get("hash").toString()));
                        } else if (receivedRoutingKey.equals(Task.Type.ELASTIC_SEARCH_CHECK_LINK.toString())) {
                            es.submit(new ElasticSearchCheckLinkTask(node.get("link").toString(), node.get("hash").toString(), esm));
                        } else if (receivedRoutingKey.equals(Task.Type.ELASTIC_SEARCH_CHECK_LINK.toString())) {
                            es.submit(new ElasticSearchAddTask(message, esm));
                        }
                        channel.basicAck(deliveryTag, false);
                    } catch (Exception e) {
                        Logger.logException(e);
                        channel.basicReject(deliveryTag, true);
                    }

                }
            };

            Thread timeoutChecker = new Thread(() -> {
                lastMessageTime = System.currentTimeMillis();
                while (true) {
                    try {
                        Thread.sleep(1000); // Интервал ожидания между проверками, например, 1 секунда
                        if (System.currentTimeMillis() - lastMessageTime > Constants.TIMEOUT) {
                            int messageCount = getMessageCount(channel, Constants.RABBITMQ_QUEUE_NAME);
                            if (messageCount == 0) {
                                Logger.info("No messages in the queue for the specified timeout, exiting.");
                                channel.basicCancel(consumer.getConsumerTag());
                                break;
                            }
                        }
                    } catch (InterruptedException | IOException e) {
                        Logger.err("Error in timeout checker: %s", e.toString());
                        break;
                    }
                }
            });

            timeoutChecker.start();
            new Thread(channel.basicConsume(Constants.RABBITMQ_QUEUE_NAME, false, consumer)).start();
        } catch (IOException | TimeoutException e) {
            Logger.err("Task execution exception: %s", e.toString());
        }
    }

    public void addTask(Task task) {
        var factory = createConnection();
        try {
            try (Connection connection = factory.newConnection();
                 Channel channel = connection.createChannel()) {
                String data;
                if (task.getType() == Task.Type.ELASTIC_SEARCH_ADD) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    data = ((ElasticSearchAddTask) task).getArticleJsonStr();
                } else {
                    ObjectMapper objectMapper = new ObjectMapper();
                    data = objectMapper.valueToTree(task).toString();
                }
                channel.basicPublish(Constants.RABBITMQ_EXCHANGE_NAME, task.type.getCode().toString(), null, data.getBytes());
                Logger.info("Sending task typ: %s", task.getType().toString());
            }
        } catch (IOException | TimeoutException e) {
            Logger.err("Task execution exception: %s", e.toString());
        }
    }
}


