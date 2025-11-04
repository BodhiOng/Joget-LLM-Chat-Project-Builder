package org.joget.marketplace;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.Test;
import static org.junit.Assert.*;

public class ZipFileUtilTest {
    
    @Test
    public void testCreateZipFromChatContentWithJsonStructure() throws IOException {
        // Sample JSON structure as would be found in chat content
        String jsonStructure = "{\n" +
            "  \"hello-world-plugin\": {\n" +
            "    \"pom.xml\": null,\n" +
            "    \"src\": {\n" +
            "      \"main\": {\n" +
            "        \"java\": {\n" +
            "          \"com\": {\n" +
            "            \"example\": {\n" +
            "              \"joget\": {\n" +
            "                \"plugin\": {\n" +
            "                  \"HelloWorldElement.java\": null\n" +
            "                }\n" +
            "              }\n" +
            "            }\n" +
            "          }\n" +
            "        },\n" +
            "        \"resources\": {\n" +
            "          \"plugin.properties\": null\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
        
        // Sample code snippets that would be found in chat content
        String chatContent = jsonStructure + "\n\n" +
            "```java\n" +
            "package com.example.joget.plugin;\n\n" +
            "public class HelloWorldElement {\n" +
            "    public String sayHello() {\n" +
            "        return \"Hello, World!\";\n" +
            "    }\n" +
            "}\n" +
            "```\n\n" +
            "```xml\n" +
            "<project>\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>com.example</groupId>\n" +
            "    <artifactId>hello-world-plugin</artifactId>\n" +
            "    <version>1.0.0</version>\n" +
            "</project>\n" +
            "```\n\n" +
            "```properties\n" +
            "plugin.id=hello-world-plugin\n" +
            "plugin.name=Hello World Plugin\n" +
            "plugin.version=1.0.0\n" +
            "```\n\n" +
            "```java\n" +
            "// This file should not be included in the zip\n" +
            "public class ExtraFile {\n" +
            "    // This is not in the JSON structure\n" +
            "}\n" +
            "```";
        
        // Create zip from chat content
        byte[] zipData = ZipFileUtil.createZipFromChatContent(chatContent);
        
        // Verify zip contents
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData));
        ZipEntry entry;
        int fileCount = 0;
        boolean foundPomXml = false;
        boolean foundJavaFile = false;
        boolean foundPropertiesFile = false;
        boolean foundExtraFile = false;
        
        while ((entry = zis.getNextEntry()) != null) {
            String name = entry.getName();
            System.out.println("Found in zip: " + name);
            
            if (name.equals("hello-world-plugin/pom.xml")) {
                foundPomXml = true;
            } else if (name.equals("hello-world-plugin/src/main/java/com/example/joget/plugin/HelloWorldElement.java")) {
                foundJavaFile = true;
            } else if (name.equals("hello-world-plugin/src/main/resources/plugin.properties")) {
                foundPropertiesFile = true;
            } else if (name.contains("ExtraFile")) {
                foundExtraFile = true;
            }
            
            fileCount++;
            zis.closeEntry();
        }
        zis.close();
        
        // Verify expected files were found and unexpected files were not
        assertTrue("pom.xml should be in the zip", foundPomXml);
        assertTrue("HelloWorldElement.java should be in the zip", foundJavaFile);
        assertTrue("plugin.properties should be in the zip", foundPropertiesFile);
        assertFalse("ExtraFile should not be in the zip", foundExtraFile);
        assertEquals("Should have exactly 3 files in the zip", 3, fileCount);
    }
}
