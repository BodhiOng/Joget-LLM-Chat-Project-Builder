package org.joget.marketplace;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.joget.commons.util.LogUtil;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility class for creating zip files from code snippets
 */
public class ZipFileUtil {

    /**
     * Creates a zip file from chat content containing code snippets
     * 
     * @param chatContent The chat content containing code snippets
     * @return A byte array containing the zip file data
     */
    public static byte[] createZipFromChatContent(String chatContent) {
        try {
            LogUtil.info(ZipFileUtil.class.getName(), "Starting to create zip from chat content");

            // Extract code files from chat content
            Map<String, String> codeFiles = extractCodeFiles(chatContent);

            // Extract project structure from JSON at the end of the response
            JSONObject projectStructure = extractProjectStructure(chatContent);

            // Create a zip file with the extracted code files
            if (codeFiles.isEmpty()) {
                LogUtil.info(ZipFileUtil.class.getName(), "No code files found in chat content");
                return null;
            }

            // If we have a project structure, use it to organize files
            if (projectStructure != null) {
                return createZipFromCodeFilesWithJsonStructure(codeFiles, projectStructure);
            } else {
                // Fallback to simple zip creation if no project structure is found
                return createZipFromCodeFiles(codeFiles);
            }
        } catch (Exception e) {
            LogUtil.error(ZipFileUtil.class.getName(), e, "Error creating zip from chat content: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extracts code files from chat content
     * 
     * @param chatContent The chat content containing code snippets
     * @return A map of filenames to code content
     */
    private static Map<String, String> extractCodeFiles(String chatContent) {
        Map<String, String> codeFiles = new HashMap<>();

        // Track the current file being processed
        String currentFileName = null;

        // Pattern to match file headers like "File 1: filename.ext"
        Pattern fileHeaderPattern = Pattern.compile("<strong>File\\s+\\d+:\\s+([^<]+)</strong>");

        // Patterns to match code blocks
        List<Pattern> codeBlockPatterns = new ArrayList<>();
        codeBlockPatterns
                .add(Pattern.compile("<pre><code\\s+class=\"language-([^\"]+)\">(.*?)</code></pre>", Pattern.DOTALL));
        codeBlockPatterns.add(Pattern.compile("<pre><code>(.*?)</code></pre>", Pattern.DOTALL));
        codeBlockPatterns
                .add(Pattern.compile("<div class=\"code-block-container\"><pre>(.*?)</pre></div>", Pattern.DOTALL));
        codeBlockPatterns.add(Pattern.compile("<pre>(.*?)</pre>", Pattern.DOTALL));
        codeBlockPatterns.add(Pattern.compile("```([a-zA-Z0-9]+)\\s*\\n(.*?)\\n```", Pattern.DOTALL));

        // Find file headers
        Matcher fileHeaderMatcher = fileHeaderPattern.matcher(chatContent);
        int lastEnd = 0;

        while (fileHeaderMatcher.find()) {
            currentFileName = fileHeaderMatcher.group(1).trim();
            lastEnd = fileHeaderMatcher.end();

            // Look for code block after the file header
            boolean foundCodeBlock = false;
            for (Pattern pattern : codeBlockPatterns) {
                Matcher codeMatcher = pattern.matcher(chatContent);
                if (codeMatcher.find(lastEnd)) {
                    String codeContent;
                    if (codeMatcher.groupCount() > 1) {
                        // If the pattern has a language group, use the second group for content
                        codeContent = codeMatcher.group(2);
                    } else {
                        // Otherwise use the first group
                        codeContent = codeMatcher.group(1);
                    }

                    // Clean up HTML entities
                    codeContent = codeContent.replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&amp;", "&")
                            .replace("&quot;", "\"")
                            .replace("&#39;", "'");

                    // Add the file to our map
                    codeFiles.put(currentFileName, codeContent);
                    foundCodeBlock = true;
                    lastEnd = codeMatcher.end();
                    break;
                }
            }

            if (!foundCodeBlock) {
                LogUtil.info(ZipFileUtil.class.getName(),
                        "Found file header but no code block for: " + currentFileName);
            }
        }

        LogUtil.info(ZipFileUtil.class.getName(), "Extracted " + codeFiles.size() + " code files");
        return codeFiles;
    }

    /**
     * Extracts project structure JSON from chat content
     * 
     * @param chatContent The chat content containing project structure JSON
     * @return A JSONObject representing the project structure, or null if not found
     */
    private static JSONObject extractProjectStructure(String chatContent) {
        try {
            // Pattern to match JSON structure at the end of the response
            Pattern jsonPattern = Pattern.compile("```json\\s*\\n(.*?)\\n```", Pattern.DOTALL);
            Matcher jsonMatcher = jsonPattern.matcher(chatContent);

            if (jsonMatcher.find()) {
                String jsonStr = jsonMatcher.group(1);
                LogUtil.info(ZipFileUtil.class.getName(), "Found project structure JSON");
                return new JSONObject(jsonStr);
            }

            LogUtil.info(ZipFileUtil.class.getName(), "No project structure JSON found");
            return null;
        } catch (JSONException e) {
            LogUtil.error(ZipFileUtil.class.getName(), e, "Error parsing project structure JSON: " + e.getMessage());
            return null;
        }
    }

    /**
     * Creates a zip file from code files using the JSON project structure
     * 
     * @param codeFiles        Map of filenames to code content
     * @param projectStructure JSONObject representing the project structure
     * @return A byte array containing the zip file data
     */
    private static byte[] createZipFromCodeFilesWithJsonStructure(Map<String, String> codeFiles,
            JSONObject projectStructure) {
        try {
            LogUtil.info(ZipFileUtil.class.getName(), "Creating zip with JSON structure");

            // Extract the hierarchical project structure
            Map<String, String> filePathMap = new HashMap<>();
            String rootDir = extractHierarchicalProjectStructure(projectStructure, "", filePathMap);

            if (rootDir == null || rootDir.isEmpty()) {
                LogUtil.info(ZipFileUtil.class.getName(), "Could not determine root directory from JSON structure");
                return createZipFromCodeFiles(codeFiles); // Fallback to simple zip
            }

            // Create a map of full paths to code content
            Map<String, String> pathToContentMap = new HashMap<>();

            // Match code files to paths in the structure
            for (Map.Entry<String, String> entry : codeFiles.entrySet()) {
                String filename = entry.getKey();
                String content = entry.getValue();
                boolean fileAdded = false;

                // Try to find an exact match in the file path map
                for (Map.Entry<String, String> pathEntry : filePathMap.entrySet()) {
                    if (pathEntry.getValue().endsWith(filename)) {
                        pathToContentMap.put(pathEntry.getValue(), content);
                        fileAdded = true;
                        break;
                    }
                }

                // If no exact match, try to infer the path based on file extension and common
                // patterns
                if (!fileAdded) {
                    // Default path is in the root directory
                    String path = rootDir + "/" + filename;
                    pathToContentMap.put(path, content);
                }
            }

            // Create the zip file
            return createZipFromCodeFiles(pathToContentMap);

        } catch (Exception e) {
            LogUtil.error(ZipFileUtil.class.getName(), e, "Error creating zip with JSON structure: " + e.getMessage());
            return createZipFromCodeFiles(codeFiles); // Fallback to simple zip
        }
    }

    /**
     * Recursively extracts the hierarchical project structure from a JSONObject
     * 
     * @param json        The JSONObject to extract from
     * @param currentPath The current path in the hierarchy
     * @param filePathMap Map to store file paths
     * @return The root directory name
     */
    private static String extractHierarchicalProjectStructure(JSONObject json, String currentPath,
            Map<String, String> filePathMap) {
        String rootDir = null;

        try {
            // Get the first key as the root directory if we're at the top level
            if (currentPath.isEmpty() && json.keys().hasNext()) {
                rootDir = json.keys().next();
                currentPath = rootDir;
            }

            // Iterate through all keys in this object
            for (String key : json.keySet()) {
                String newPath = currentPath.isEmpty() ? key : currentPath + "/" + key;
                Object value = json.get(key);

                if (value instanceof JSONObject) {
                    // Recursively process nested directories
                    extractHierarchicalProjectStructure((JSONObject) value, newPath, filePathMap);
                } else if (value == null || value.equals(JSONObject.NULL)) {
                    // This is a file (null value)
                    filePathMap.put(key, newPath);
                }
            }

            return rootDir;
        } catch (JSONException e) {
            LogUtil.error(ZipFileUtil.class.getName(), e, "Error extracting hierarchical structure: " + e.getMessage());
            return null;
        }
    }

    /**
     * Creates a zip file from a map of filenames to code content
     * 
     * @param codeFiles Map of filenames to code content
     * @return A byte array containing the zip file data
     */
    private static byte[] createZipFromCodeFiles(Map<String, String> codeFiles) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(baos)) {

            LogUtil.info(ZipFileUtil.class.getName(), "Creating zip with " + codeFiles.size() + " files");

            // Add each file to the zip
            for (Map.Entry<String, String> entry : codeFiles.entrySet()) {
                String filename = entry.getKey();
                String content = entry.getValue();

                // Normalize path separators
                filename = filename.replace("\\", "/");

                // Create a zip entry
                ZipEntry zipEntry = new ZipEntry(filename);
                zos.putNextEntry(zipEntry);

                // Write the file content
                byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                zos.write(bytes, 0, bytes.length);

                // Close the entry
                zos.closeEntry();
            }

            // Finish the zip
            zos.finish();

            return baos.toByteArray();
        } catch (IOException e) {
            LogUtil.error(ZipFileUtil.class.getName(), e, "Error creating zip file: " + e.getMessage());
            return null;
        }
    }
}