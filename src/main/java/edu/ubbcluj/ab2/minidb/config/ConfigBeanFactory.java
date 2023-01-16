package edu.ubbcluj.ab2.minidb.config;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class ConfigBeanFactory {
    private static ConfigBean configBean;

    static {
        ObjectMapper objectMapper = new ObjectMapper();
        try (InputStream inputStream = ConfigBeanFactory.class.getResourceAsStream("/config.json")) {
            configBean = objectMapper.readValue(inputStream, ConfigBean.class);
        } catch (IOException e) {

            throw new RuntimeException("Failed loading config");
        }
    }

    public static ConfigBean getConfigBean() {
        return configBean;
    }
}
