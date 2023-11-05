package com.icodetest.icodetest.configuration;

import com.mongodb.client.MongoClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import javax.sql.DataSource;

//@Configuration
public class DatabaseConfig {

//    @Bean
//    public MongoDatabaseFactory mongoDbFactory(MongoClient mongo) {
//        return new SimpleMongoClientDatabaseFactory(mongo, "icodetest");
//    }
//
//    @Bean
//    @Primary
//    @ConfigurationProperties(prefix = "spring.datasource")
//    public DataSource dataSource() {
//        return DataSourceBuilder.create().build();
//    }
}
