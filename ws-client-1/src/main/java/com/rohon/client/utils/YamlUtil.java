package com.rohon.client.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.Properties;

@Slf4j
public class YamlUtil {

    private static final Properties ymlProperties;

    static {
        Resource app = new ClassPathResource("application.yml");
        YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
        yamlPropertiesFactoryBean.setResources(app);
        ymlProperties = yamlPropertiesFactoryBean.getObject();
    }

    public static String getStr(String key){
        return ymlProperties.getProperty(key);
    }

    public static Integer getInt(String key){
        String property = ymlProperties.getProperty(key);
        return Integer.valueOf(property);
    }


}