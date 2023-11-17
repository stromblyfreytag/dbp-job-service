package com.trustwave.dbpjobservice.config;

import javax.sql.DataSource;
import java.util.Properties;

import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * <p/>
 * Configuration for dbp-asset-service
 * <p/>
 * <b><pre>
 * Copyright (c) 2021 Trustwave Holdings, Inc.
 * All rights reserved.
 * </pre></b>
 *
 * @author sfreytag
 */
@Configuration
@EnableTransactionManagement
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class})
@ComponentScan(basePackages = {
        "com.trustwave.dbpjobservice",
        "com.trustwave.dbpjobservice.workflow.hib",
        "com.trustwave.dbpjobservice.workflow.light.db",
        "com.trustwave.dbpjobservice.workflow"
})
public class DbpJobServiceDataConfig {

    @Autowired
    private Environment env;

    @Bean(name = "JobSession")
    public LocalSessionFactoryBean assetSessionFactory() throws Exception {
        Properties properties = new Properties();

        properties.put("hibernate.dialect", env.getProperty("spring.jpa.properties.hibernate.dialect"));
        properties.put("hibernate.show_sql", env.getProperty("spring.jpa.show-sql"));
        properties.put("current_session_context_class", //
                env.getProperty("spring.jpa.properties.hibernate.current_session_context_class"));
        properties.put("hibernate.jdbc.batch_size", env.getProperty("spring.jpa.properties.hibernate.batch_size"));
        LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();

        // Package contain entity classes
        factoryBean.setPackagesToScan("com.trustwave.dbpassetservice.domain");
        factoryBean.setDataSource(dataSource());
        factoryBean.setHibernateProperties(properties);
        factoryBean.afterPropertiesSet();

        return factoryBean;
    }

    public DataSource dataSource() {
        BasicDataSource dataSource = new BasicDataSource();

        dataSource.setDriverClassName(env.getProperty("spring.datasource.driver-class-name"));
        dataSource.setUrl(env.getProperty("spring.datasource.appdetectiveurl"));
        dataSource.setUsername(env.getProperty("spring.datasource.appdetectiveusername"));
        dataSource.setPassword(env.getProperty("spring.datasource.appdetectivepassword"));

        return dataSource;

    }

    @Bean(name = "JobHibernateTransaction")
    public PlatformTransactionManager hibernateTransactionManager() {
        HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        try {
            transactionManager.setSessionFactory(assetSessionFactory().getObject());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return transactionManager;
    }
}