package org.example;

import java.lang.reflect.Field;

public class Constants {
    public static void Init() {
        Constants constants = new Constants();
        for (Field field : constants.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            String value = System.getenv(field.getName());
            if (value == null || value.isEmpty()) {
                throw new RuntimeException(String.format("Env variable %s is not set!", field.getName()));
            }
            try {
                if (!field.getName().equals("RABBITMQ_PORT")) {
                    field.set(constants, value);
                } else {
                    field.set(constants, Integer.valueOf(value));
                }

            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String ES_SERVER_URL;
    public static String ES_INDEX;

    public static String RABBITMQ_HOST;
    public static int RABBITMQ_PORT;
    public static String RABBITMQ_USER;
    public static String RABBITMQ_PASS;
    public static String RABBITMQ_QUEUE_NAME;
    public static String RABBITMQ_EXCHANGE_NAME;
    public static Integer POOL_COUNT;
    public static long TIMEOUT;


}
