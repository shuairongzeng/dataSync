package com.dbsync.dbsync.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义反序列化器，用于将表名数组转换为JSON字符串
 */
public class TablesListDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        
        if (node.isArray()) {
            // 如果是数组，转换为JSON字符串
            List<String> tables = new ArrayList<>();
            for (JsonNode element : node) {
                if (element.isTextual()) {
                    tables.add(element.asText());
                }
            }
            
            // 转换为JSON字符串
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < tables.size(); i++) {
                if (i > 0) {
                    json.append(",");
                }
                json.append("\"").append(tables.get(i)).append("\"");
            }
            json.append("]");
            return json.toString();
        } else if (node.isTextual()) {
            // 如果已经是字符串，直接返回
            return node.asText();
        } else {
            // 其他情况返回空数组
            return "[]";
        }
    }
}