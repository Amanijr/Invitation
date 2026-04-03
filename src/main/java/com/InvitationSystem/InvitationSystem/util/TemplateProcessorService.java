package com.InvitationSystem.InvitationSystem.util;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TemplateProcessorService {

    public String renderTemplate(String templateContent, Map<String, String> data) {
        if (templateContent == null) {
            return "";
        }

        String rendered = templateContent;
        if (data == null || data.isEmpty()) {
            return rendered;
        }

        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() == null ? "" : entry.getValue();
            rendered = rendered.replace("{{" + key + "}}", value);
        }

        return rendered;
    }
}
