package edu.ubbcluj.ab2.minidb.config;

import java.io.Serializable;

public class ConfigBean implements Serializable {
    private String mongoClientURL;

    public String getMongoClientURL() {
        return mongoClientURL;
    }

    public void setMongoClientURL(String mongoClientURL) {
        this.mongoClientURL = mongoClientURL;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ConfigBean{");
        sb.append(", mongoClientURL='").append(mongoClientURL).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
