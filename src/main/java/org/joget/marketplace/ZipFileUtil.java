package org.joget.marketplace;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.joget.commons.util.LogUtil;

/**
 * Utility class for creating zip files from code snippets
 */
public class ZipFileUtil {

    /**
     * Creates a zip file containing code snippets organized in a folder structure
     * 
     * @param chatContent The chat content containing code snippets
     * @return Byte array containing the zip file data
     * @throws IOException If there's an error creating the zip file
     */
    public static byte[] createZipFromChatContent(String chatContent) throws IOException {
        LogUtil.info(ZipFileUtil.class.getName(), "Starting to create zip from chat content");
        LogUtil.info(ZipFileUtil.class.getName(), "Chat content length: " + chatContent.length());
        
        // Initialize project structure from chat content
        initializeProjectStructure(chatContent);
        
        // Extract code snippets from chat content
        Map<String, String> codeSnippets = extractCodeSnippets(chatContent);
        
        // Create a byte array output stream to hold the zip data
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        
        try {
            // Add each code snippet to the zip file
            for (Map.Entry<String, String> entry : codeSnippets.entrySet()) {
                String filePath = entry.getKey();
                String codeContent = entry.getValue();
                
                // Create a new zip entry
                ZipEntry zipEntry = new ZipEntry(filePath);
                zos.putNextEntry(zipEntry);
                
                // Write the code content to the zip file
                zos.write(codeContent.getBytes());
                
                // Close the zip entry
                zos.closeEntry();
            }
        } finally {
            // Close the zip output stream
            zos.close();
        }
        
        byte[] zipData = baos.toByteArray();
        LogUtil.info(ZipFileUtil.class.getName(), "Created zip file with " + codeSnippets.size() + " code snippets, zip size: " + zipData.length + " bytes");
        return zipData;
    }
    
    /**
     * Extracts the file name or path from a code block header or comment
     * 
     * @param content The content before the code block or the code block itself
     * @return The extracted file path/name or empty string if not found
     */
    private static String extractFileNameFromContext(String content) {
        LogUtil.info(ZipFileUtil.class.getName(), "Analyzing context for file name: " + 
                     (content.length() > 200 ? content.substring(0, 200) + "..." : content));
        
        // Look for directory tree structures (highest priority)
        // Example: "+- src/main/java/org/example/joget/HelloWorldActivity.java"
        Pattern directoryTreePattern = Pattern.compile("(?:[|+`]-|-+)\\s+([\\w\\-./]+\\.[a-zA-Z0-9]+)");
        Matcher directoryTreeMatcher = directoryTreePattern.matcher(content);
        
        if (directoryTreeMatcher.find()) {
            String path = directoryTreeMatcher.group(1).trim();
            LogUtil.info(ZipFileUtil.class.getName(), "Found file path in directory tree: " + path);
            return path;
        }
        
        // Look for explicit file paths with structure (high priority)
        // Example: "src/main/java/com/example/MyClass.java"
        Pattern fullPathPattern = Pattern.compile("(?i)(?:src|app|lib|config|public|components|utils|styles|database)/[\\w\\-./]+\\.[a-zA-Z0-9]+");
        Matcher fullPathMatcher = fullPathPattern.matcher(content);
        
        // Look for code blocks with file path comments
        Pattern codeCommentPattern = Pattern.compile("(?i)(?://|#|\\*)\\s*(?:file|path):\\s*([\\w\\-./]+\\.[a-zA-Z0-9]+)");
        Matcher codeCommentMatcher = codeCommentPattern.matcher(content);
        
        // Look for file paths in HTML tags (common in responses)
        Pattern htmlTagPattern = Pattern.compile("<(?:h\\d|p|strong|em|code|pre)>([\\w\\-./]+\\.[a-zA-Z0-9]+)</(?:h\\d|p|strong|em|code|pre)>");
        Matcher htmlTagMatcher = htmlTagPattern.matcher(content);
        
        // Look for code blocks with language class attributes
        // Example: <pre><code class="language-java"> - then look for filename in surrounding text
        Pattern languageClassPattern = Pattern.compile("<(?:pre|code)\\s+class=\"language-([a-zA-Z0-9_+-]+)\">");
        Matcher languageClassMatcher = languageClassPattern.matcher(content);
        
        if (languageClassMatcher.find()) {
            String language = languageClassMatcher.group(1).trim();
            // Look for a filename near this language tag
            String searchArea = content;
            if (content.length() > 200) {
                int startPos = Math.max(0, languageClassMatcher.start() - 100);
                int endPos = Math.min(content.length(), languageClassMatcher.end() + 100);
                searchArea = content.substring(startPos, endPos);
            }
            
            // Try to find a filename with matching extension
            String fileExtension = getFileExtensionForLanguage(language).substring(1); // remove the dot
            Pattern fileWithExtPattern = Pattern.compile("([\\w\\-./]+\\."+fileExtension+")");
            Matcher fileWithExtMatcher = fileWithExtPattern.matcher(searchArea);
            
            if (fileWithExtMatcher.find()) {
                String fileName = fileWithExtMatcher.group(1).trim();
                LogUtil.info(ZipFileUtil.class.getName(), "Found filename matching language class: " + fileName);
                return fileName;
            }
        }
        
        if (htmlTagMatcher.find()) {
            String path = htmlTagMatcher.group(1).trim();
            LogUtil.info(ZipFileUtil.class.getName(), "Found file path in HTML tag: " + path);
            return path;
        }
        
        if (codeCommentMatcher.find()) {
            String path = codeCommentMatcher.group(1).trim();
            LogUtil.info(ZipFileUtil.class.getName(), "Found file path in code comment: " + path);
            return path;
        }
        
        if (fullPathMatcher.find()) {
            String path = fullPathMatcher.group(0).trim();
            LogUtil.info(ZipFileUtil.class.getName(), "Found explicit path structure in context: " + path);
            return path;
        }
        
        // Look for explicit file path mentions
        // Example: "Create file at path: src/components/Button.js"
        Pattern pathMentionPattern = Pattern.compile("(?i)(?:path:|file path:|location:|directory:|folder:|in path:|at path:|save (?:to|in|at)|create (?:in|at)|write (?:to|in|at))\\s+['\"]?([\\w\\-./]+/[\\w\\-./]+\\.[a-zA-Z0-9]+)['\"]?");
        Matcher pathMentionMatcher = pathMentionPattern.matcher(content);
        
        if (pathMentionMatcher.find()) {
            String path = pathMentionMatcher.group(1).trim();
            LogUtil.info(ZipFileUtil.class.getName(), "Found path mention in context: " + path);
            return path;
        }
        
        // Look for file name with explicit mention (medium priority)
        // Example: "filename: MyClass.java" or "File: MyClass.java"
        Pattern fileNamePattern = Pattern.compile("(?i)(?:file(?:name)?:|name:|named:|called:|save as|create file|new file|file is)\\s+['\"]?([\\w\\-]+\\.[a-zA-Z0-9]+)['\"]?");
        Matcher fileNameMatcher = fileNamePattern.matcher(content);
        
        if (fileNameMatcher.find()) {
            String fileName = fileNameMatcher.group(1).trim();
            LogUtil.info(ZipFileUtil.class.getName(), "Found explicit filename in context: " + fileName);
            return fileName;
        }
        
        // Look for code file references in headings or emphasized text (lower priority)
        // Example: "## Creating MyClass.java" or "**MyClass.java**"
        Pattern headingPattern = Pattern.compile("(?i)(?:#{1,6}|\\*\\*|__)\\s*([\\w\\-]+\\.[a-zA-Z0-9]+)\\s*(?:\\*\\*|__|$)");
        Matcher headingMatcher = headingPattern.matcher(content);
        
        // Look for section headers with file names (common in responses)
        // Example: "4. Java Class – `HelloWorldActivity.java`"
        Pattern sectionHeaderPattern = Pattern.compile("(?i)(?:\\d+\\.\\s+)?(?:java|class|file|create)\\s+(?:file|class)?\\s*[\\-–]\\s*[`'\"]?([\\w\\-./]+\\.[a-zA-Z0-9]+)[`'\"]?");
        Matcher sectionHeaderMatcher = sectionHeaderPattern.matcher(content);
        
        if (sectionHeaderMatcher.find()) {
            String fileName = sectionHeaderMatcher.group(1).trim();
            LogUtil.info(ZipFileUtil.class.getName(), "Found filename in section header: " + fileName);
            return fileName;
        }
        
        // Look for markdown code block references
        // Example: ```java:MyClass.java or ```javascript:app.js
        Pattern markdownCodePattern = Pattern.compile("```[a-zA-Z0-9_+-]+:([\\w\\-./]+\\.[a-zA-Z0-9]+)");
        Matcher markdownCodeMatcher = markdownCodePattern.matcher(content);
        
        // Look for code block with language and filename comment
        // Example: ```language-java (HelloWorldActivity.java)
        Pattern codeBlockLanguagePattern = Pattern.compile("```language-[a-zA-Z0-9_+-]+\\s*\\(([\\w\\-./]+\\.[a-zA-Z0-9]+)\\)");
        Matcher codeBlockLanguageMatcher = codeBlockLanguagePattern.matcher(content);
        
        if (codeBlockLanguageMatcher.find()) {
            String fileName = codeBlockLanguageMatcher.group(1).trim();
            LogUtil.info(ZipFileUtil.class.getName(), "Found filename in code block language comment: " + fileName);
            return fileName;
        }
        
        if (markdownCodeMatcher.find()) {
            String fileName = markdownCodeMatcher.group(1).trim();
            LogUtil.info(ZipFileUtil.class.getName(), "Found filename in markdown code block: " + fileName);
            return fileName;
        }
        
        if (headingMatcher.find()) {
            String fileName = headingMatcher.group(1).trim();
            LogUtil.info(ZipFileUtil.class.getName(), "Found filename in heading/emphasis: " + fileName);
            return fileName;
        }
        
        // Look for any file extension pattern as last resort
        Pattern extensionPattern = Pattern.compile("([\\w\\-]+\\.(java|js|py|html|css|xml|json|md|txt))");
        Matcher extensionMatcher = extensionPattern.matcher(content);
        
        if (extensionMatcher.find()) {
            String fileName = extensionMatcher.group(1).trim();
            LogUtil.info(ZipFileUtil.class.getName(), "Found filename with extension: " + fileName);
            return fileName;
        }
        
        return "";
    }
    
    private static Map<String, String> extractCodeSnippets(String chatContent) {
        Map<String, String> codeSnippets = new HashMap<>();
        
        LogUtil.info(ZipFileUtil.class.getName(), "Extracting code snippets from chat content");
        
        // First try to extract code blocks from HTML content
        // Look for various patterns of HTML code blocks as rendered by marked.js
        
        // Debug: Log the first 500 chars of content to see what we're dealing with
        LogUtil.info(ZipFileUtil.class.getName(), "Content sample: " + 
            (chatContent.length() > 500 ? chatContent.substring(0, 500) + "..." : chatContent));
        
        // Pattern 1: <pre><code class="language-xxx"> (standard marked.js output)
        Pattern htmlCodeBlockPattern1 = Pattern.compile("<pre><code\\s+class=\"language-([a-zA-Z0-9_+-]+)\">(.*?)</code></pre>", Pattern.DOTALL);
        
        // Pattern 2: <pre><code> without language class (generic code blocks)
        Pattern htmlCodeBlockPattern2 = Pattern.compile("<pre><code>(.*?)</code></pre>", Pattern.DOTALL);
        
        // Pattern 3: <div class="code-block-container"><pre> (our custom wrapper)
        Pattern htmlCodeBlockPattern3 = Pattern.compile("<div class=\"code-block-container\"><pre>(.*?)</pre>", Pattern.DOTALL);
        
        // Pattern 4: <pre> tags directly (simpler format)
        Pattern htmlCodeBlockPattern4 = Pattern.compile("<pre>(.*?)</pre>", Pattern.DOTALL);
        
        // Pattern 5: Code blocks with line numbers (as rendered by some markdown processors)
        Pattern htmlCodeBlockPattern5 = Pattern.compile("<pre><code[^>]*>(.*?)</code></pre>", Pattern.DOTALL);
        
        LogUtil.info(ZipFileUtil.class.getName(), "Defined 5 HTML code block patterns for extraction");
        
        // Try pattern 1 first (with language specifier)
        Matcher htmlMatcher = htmlCodeBlockPattern1.matcher(chatContent);
        
        int snippetCounter = 1;
        boolean foundHtmlCodeBlocks = false;
        
        while (htmlMatcher.find()) {
            foundHtmlCodeBlocks = true;
            String language = htmlMatcher.group(1).trim().toLowerCase();
            String code = htmlMatcher.group(2);
            
            // Try to find the context before this code block to extract filename
            String contextBefore = "";
            int startPos = Math.max(0, htmlMatcher.start() - 500); // Look at up to 500 chars before the code block
            if (startPos < htmlMatcher.start()) {
                contextBefore = chatContent.substring(startPos, htmlMatcher.start());
            }
            
            // Unescape HTML entities
            code = code.replace("&lt;", "<")
                      .replace("&gt;", ">")
                      .replace("&amp;", "&")
                      .replace("&quot;", "\"")
                      .replace("&#39;", "'");
            
            // Try to extract file name from context
            String suggestedFileName = extractFileNameFromContext(contextBefore);
            
            // Determine file path based on language, content, and suggested file name
            String filePath = determineFilePath(language, code, snippetCounter, suggestedFileName);
            
            // Add the code snippet to the map
            codeSnippets.put(filePath, code);
            
            snippetCounter++;
            LogUtil.info(ZipFileUtil.class.getName(), "Found HTML code block: " + language + ", path: " + filePath);
        }
        
        // If no HTML code blocks were found with pattern 1, try pattern 2
        if (!foundHtmlCodeBlocks) {
            htmlMatcher = htmlCodeBlockPattern2.matcher(chatContent);
            
            while (htmlMatcher.find()) {
                foundHtmlCodeBlocks = true;
                // No language specified, try to guess from content
                String code = htmlMatcher.group(1);
                
                // Unescape HTML entities
                code = code.replace("&lt;", "<")
                          .replace("&gt;", ">")
                          .replace("&amp;", "&")
                          .replace("&quot;", "\"")
                          .replace("&#39;", "'");
                
                // Try to determine language from content
                String language = guessLanguageFromContent(code);
                
                // Try to find the context before this code block to extract filename
                String contextBefore = "";
                int startPos = Math.max(0, htmlMatcher.start() - 500); // Look at up to 500 chars before the code block
                if (startPos < htmlMatcher.start()) {
                    contextBefore = chatContent.substring(startPos, htmlMatcher.start());
                }
                
                // Try to extract file name from context
                String suggestedFileName = extractFileNameFromContext(contextBefore);
                
                // Determine file path based on language, content, and suggested file name
                String filePath = determineFilePath(language, code, snippetCounter, suggestedFileName);
                
                // Add the code snippet to the map
                codeSnippets.put(filePath, code);
                
                snippetCounter++;
                LogUtil.info(ZipFileUtil.class.getName(), "Found HTML code block (no language): guessed as " + language + ", path: " + filePath);
            }
        }
        
        // If still no HTML code blocks found, try pattern 3
        if (!foundHtmlCodeBlocks) {
            LogUtil.info(ZipFileUtil.class.getName(), "Trying pattern 3 (code-block-container)...");
            htmlMatcher = htmlCodeBlockPattern3.matcher(chatContent);
            
            while (htmlMatcher.find()) {
                foundHtmlCodeBlocks = true;
                String code = htmlMatcher.group(1);
                
                // Unescape HTML entities
                code = code.replace("&lt;", "<")
                          .replace("&gt;", ">")
                          .replace("&amp;", "&")
                          .replace("&quot;", "\"")
                          .replace("&#39;", "'");
                
                // Try to determine language from content
                String language = guessLanguageFromContent(code);
                
                // Try to find the context before this code block to extract filename
                String contextBefore = "";
                int startPos = Math.max(0, htmlMatcher.start() - 500); // Look at up to 500 chars before the code block
                if (startPos < htmlMatcher.start()) {
                    contextBefore = chatContent.substring(startPos, htmlMatcher.start());
                }
                
                // Try to extract file name from context
                String suggestedFileName = extractFileNameFromContext(contextBefore);
                
                // Determine file path based on language, content, and suggested file name
                String filePath = determineFilePath(language, code, snippetCounter, suggestedFileName);
                
                // Add the code snippet to the map
                codeSnippets.put(filePath, code);
                
                snippetCounter++;
                LogUtil.info(ZipFileUtil.class.getName(), "Found code block container: guessed as " + language + ", path: " + filePath);
            }
        }
        
        // Try pattern 4 if still no matches
        if (!foundHtmlCodeBlocks) {
            LogUtil.info(ZipFileUtil.class.getName(), "Trying pattern 4 (simple pre tags)...");
            htmlMatcher = htmlCodeBlockPattern4.matcher(chatContent);
            
            while (htmlMatcher.find()) {
                foundHtmlCodeBlocks = true;
                String code = htmlMatcher.group(1);
                
                // Unescape HTML entities
                code = code.replace("&lt;", "<")
                          .replace("&gt;", ">")
                          .replace("&amp;", "&")
                          .replace("&quot;", "\"")
                          .replace("&#39;", "'");
                
                // Try to determine language from content
                String language = guessLanguageFromContent(code);
                
                // Try to find the context before this code block to extract filename
                String contextBefore = "";
                int startPos = Math.max(0, htmlMatcher.start() - 500); // Look at up to 500 chars before the code block
                if (startPos < htmlMatcher.start()) {
                    contextBefore = chatContent.substring(startPos, htmlMatcher.start());
                }
                
                // Try to extract file name from context
                String suggestedFileName = extractFileNameFromContext(contextBefore);
                
                // Determine file path based on language, content, and suggested file name
                String filePath = determineFilePath(language, code, snippetCounter, suggestedFileName);
                
                // Add the code snippet to the map
                codeSnippets.put(filePath, code);
                
                snippetCounter++;
                LogUtil.info(ZipFileUtil.class.getName(), "Found simple pre tag: guessed as " + language + ", path: " + filePath);
            }
        }
        
        // Try pattern 5 if still no matches
        if (!foundHtmlCodeBlocks) {
            LogUtil.info(ZipFileUtil.class.getName(), "Trying pattern 5 (code with line numbers)...");
            htmlMatcher = htmlCodeBlockPattern5.matcher(chatContent);
            
            while (htmlMatcher.find()) {
                foundHtmlCodeBlocks = true;
                String code = htmlMatcher.group(1);
                
                // Unescape HTML entities
                code = code.replace("&lt;", "<")
                          .replace("&gt;", ">")
                          .replace("&amp;", "&")
                          .replace("&quot;", "\"")
                          .replace("&#39;", "'");
                
                // Try to determine language from content
                String language = guessLanguageFromContent(code);
                
                // Try to find the context before this code block to extract filename
                String contextBefore = "";
                int startPos = Math.max(0, htmlMatcher.start() - 500); // Look at up to 500 chars before the code block
                if (startPos < htmlMatcher.start()) {
                    contextBefore = chatContent.substring(startPos, htmlMatcher.start());
                }
                
                // Try to extract file name from context
                String suggestedFileName = extractFileNameFromContext(contextBefore);
                
                // Determine file path based on language, content, and suggested file name
                String filePath = determineFilePath(language, code, snippetCounter, suggestedFileName);
                
                // Add the code snippet to the map
                codeSnippets.put(filePath, code);
                
                snippetCounter++;
                LogUtil.info(ZipFileUtil.class.getName(), "Found code with line numbers: guessed as " + language + ", path: " + filePath);
            }
        }
        
        // If still no HTML code blocks were found, try to extract markdown code blocks
        if (!foundHtmlCodeBlocks) {
            LogUtil.info(ZipFileUtil.class.getName(), "Trying markdown code block pattern...");
            // Pattern to match markdown code blocks with language specifier
            // Format: ```language\ncode\n```
            Pattern codeBlockPattern = Pattern.compile("```([a-zA-Z0-9_+-]+)\\s*\\r?\\n(.*?)\\r?\\n```", Pattern.DOTALL);
            Matcher matcher = codeBlockPattern.matcher(chatContent);
            
            snippetCounter = 1;
            
            while (matcher.find()) {
                String language = matcher.group(1).trim().toLowerCase();
                String code = matcher.group(2);
                
                // Try to find the context before this code block to extract filename
                String contextBefore = "";
                int startPos = Math.max(0, matcher.start() - 500); // Look at up to 500 chars before the code block
                if (startPos < matcher.start()) {
                    contextBefore = chatContent.substring(startPos, matcher.start());
                }
                
                // Try to extract file name from context
                String suggestedFileName = extractFileNameFromContext(contextBefore);
                
                // Determine file path based on language, content, and suggested file name
                String filePath = determineFilePath(language, code, snippetCounter, suggestedFileName);
                
                // Add the code snippet to the map
                codeSnippets.put(filePath, code);
                
                snippetCounter++;
                LogUtil.info(ZipFileUtil.class.getName(), "Found markdown code block: " + language + ", path: " + filePath);
            }
        }
        
        // If still no code blocks found, try a more generic approach for HTML pre tags
        if (codeSnippets.isEmpty()) {
            LogUtil.info(ZipFileUtil.class.getName(), "No code blocks found yet, trying generic pre tag pattern...");
            // This pattern matches both <pre> tags and <pre><code> combinations
            Pattern preTagPattern = Pattern.compile("<pre>(?:<code>)?(.*?)(?:</code>)?</pre>", Pattern.DOTALL);
            Matcher preTagMatcher = preTagPattern.matcher(chatContent);
            
            snippetCounter = 1;
            
            while (preTagMatcher.find()) {
                String code = preTagMatcher.group(1);
                
                // Try to determine language from content
                String language = guessLanguageFromContent(code);
                
                // Unescape HTML entities
                code = code.replace("&lt;", "<")
                          .replace("&gt;", ">")
                          .replace("&amp;", "&")
                          .replace("&quot;", "\"")
                          .replace("&#39;", "'");
                
                // Try to find the context before this code block to extract filename
                String contextBefore = "";
                int startPos = Math.max(0, preTagMatcher.start() - 500); // Look at up to 500 chars before the code block
                if (startPos < preTagMatcher.start()) {
                    contextBefore = chatContent.substring(startPos, preTagMatcher.start());
                }
                
                // Try to extract file name from context
                String suggestedFileName = extractFileNameFromContext(contextBefore);
                
                // Determine file path based on language, content, and suggested file name
                String filePath = determineFilePath(language, code, snippetCounter, suggestedFileName);
                
                // Add the code snippet to the map
                codeSnippets.put(filePath, code);
                
                snippetCounter++;
                LogUtil.info(ZipFileUtil.class.getName(), "Found generic pre tag: language guessed as " + language + ", path: " + filePath);
            }
        }
        
        LogUtil.info(ZipFileUtil.class.getName(), "Total code snippets found: " + codeSnippets.size());
        
        return codeSnippets;
    }
    
    /**
     * Determines the file path for a code snippet based on language, content, and suggested file name
     * 
     * @param language The programming language of the code snippet
     * @param code The code content
     * @param snippetCounter A counter to ensure unique file names
     * @param suggestedFileName A file name suggested in the context
     * @return The file path for the code snippet
     */
    // Store the project structure from the last response for reference
    private static final Map<String, String> knownProjectStructure = new HashMap<>();
    private static boolean projectStructureInitialized = false;
    
    /**
     * Extract project structure from JSON format in the chat content
     * 
     * @param chatContent The chat content that might contain JSON project structure
     * @return True if project structure was found and extracted, false otherwise
     */
    private static boolean extractProjectStructureFromJson(String chatContent) {
        if (chatContent == null) {
            LogUtil.info(ZipFileUtil.class.getName(), "No chat content provided for JSON project structure extraction");
            return false;
        }
        
        LogUtil.info(ZipFileUtil.class.getName(), "Looking for JSON project structure in chat content");
        
        // First try to extract hierarchical project structure
        boolean foundHierarchical = extractHierarchicalProjectStructure(chatContent);
        if (foundHierarchical) {
            return true;
        }
        
        // Fallback to flat project structure format
        // Look for JSON structure pattern with multiple variations
        // Example: {"projectStructure": {"pom.xml": "hello-world-plugin/pom.xml", "HelloWorldElement.java": "hello-world-plugin/src/main/java/com/example/joget/plugin/HelloWorldElement.java"}}
        Pattern jsonPattern1 = Pattern.compile("[\"']?projectStructure[\"']?\\s*[:=]\\s*(\\{[^}]+\\})");
        Pattern jsonPattern2 = Pattern.compile("\\{\\s*[\"']?projectStructure[\"']?\\s*:\\s*(\\{[^}]+\\})\\s*\\}");
        
        // Try first pattern
        Matcher jsonMatcher = jsonPattern1.matcher(chatContent);
        boolean found = false;
        String jsonStr = null;
        
        if (jsonMatcher.find()) {
            jsonStr = jsonMatcher.group(1);
            found = true;
        } else {
            // Try second pattern
            jsonMatcher = jsonPattern2.matcher(chatContent);
            if (jsonMatcher.find()) {
                jsonStr = jsonMatcher.group(1);
                found = true;
            }
        }
        
        if (found && jsonStr != null) {
            LogUtil.info(ZipFileUtil.class.getName(), "Found JSON project structure: " + jsonStr);
            
            try {
                // Try to extract key-value pairs with more robust pattern matching
                // Format: "filename": "path/to/file"
                Pattern keyValuePattern = Pattern.compile("[\"']([^\"':,{}]+)[\"']\\s*:\\s*[\"']([^\"',{}]+)[\"']");
                Matcher keyValueMatcher = keyValuePattern.matcher(jsonStr);
                
                boolean foundAny = false;
                while (keyValueMatcher.find()) {
                    String fileName = keyValueMatcher.group(1).trim();
                    String path = keyValueMatcher.group(2).trim();
                    
                    // In the example format, keys are filenames and values are paths
                    knownProjectStructure.put(fileName, path);
                    LogUtil.info(ZipFileUtil.class.getName(), "Added to project structure: " + fileName + " -> " + path);
                    foundAny = true;
                }
                
                return foundAny;
            } catch (Exception e) {
                LogUtil.error(ZipFileUtil.class.getName(), e, "Error parsing JSON project structure");
            }
        }
        
        return false;
    }
    
    /**
     * Extract hierarchical project structure from JSON format in the chat content
     * 
     * @param chatContent The chat content that might contain hierarchical JSON project structure
     * @return True if project structure was found and extracted, false otherwise
     */
    private static boolean extractHierarchicalProjectStructure(String chatContent) {
        LogUtil.info(ZipFileUtil.class.getName(), "Looking for hierarchical JSON project structure");
        
        // Try multiple patterns to extract JSON structure
        boolean found = false;
        String jsonBlock = null;
        
        // Pattern 1: Code block with json format
        Pattern codeBlockPattern = Pattern.compile("```(?:json)?\\s*(\\{[\\s\\S]*?\\})\\s*```");
        Matcher codeBlockMatcher = codeBlockPattern.matcher(chatContent);
        if (codeBlockMatcher.find()) {
            jsonBlock = codeBlockMatcher.group(1).trim();
            found = true;
            LogUtil.info(ZipFileUtil.class.getName(), "Found hierarchical JSON structure in code block");
        }
        
        // Pattern 2: Raw JSON object with a root project structure
        if (!found) {
            // Look for JSON with a single root key that contains nested objects
            Pattern rawJsonPattern = Pattern.compile("\\{\\s*\"[^\"]+\"\\s*:\\s*\\{[\\s\\S]*?\\}\\s*\\}");
            Matcher rawJsonMatcher = rawJsonPattern.matcher(chatContent);
            if (rawJsonMatcher.find()) {
                jsonBlock = rawJsonMatcher.group(0).trim();
                found = true;
                LogUtil.info(ZipFileUtil.class.getName(), "Found hierarchical JSON structure with root project key");
            }
        }
        
        // Pattern 3: Any JSON object with nested structure
        if (!found) {
            Pattern anyJsonPattern = Pattern.compile("\\{[\\s\\S]*?\\{[\\s\\S]*?null[\\s\\S]*?\\}[\\s\\S]*?\\}");
            Matcher anyJsonMatcher = anyJsonPattern.matcher(chatContent);
            if (anyJsonMatcher.find()) {
                jsonBlock = anyJsonMatcher.group(0).trim();
                found = true;
                LogUtil.info(ZipFileUtil.class.getName(), "Found hierarchical JSON structure with nested objects");
            }
        }
        
        if (found && jsonBlock != null) {
            LogUtil.info(ZipFileUtil.class.getName(), "Processing hierarchical JSON structure: " + 
                         (jsonBlock.length() > 100 ? jsonBlock.substring(0, 100) + "..." : jsonBlock));
            
            try {
                // Clear existing structure before processing
                knownProjectStructure.clear();
                
                // Process the hierarchical structure recursively
                processHierarchicalStructure("", jsonBlock);
                
                if (!knownProjectStructure.isEmpty()) {
                    LogUtil.info(ZipFileUtil.class.getName(), "Successfully extracted " + knownProjectStructure.size() + " file paths from hierarchical structure");
                    return true;
                }
            } catch (Exception e) {
                LogUtil.error(ZipFileUtil.class.getName(), e, "Error parsing hierarchical JSON structure");
            }
        }
        
        return false;
    }
    
    /**
     * Process hierarchical JSON structure recursively
     * 
     * @param currentPath The current path in the hierarchy
     * @param jsonStr The JSON string to process
     */
    private static void processHierarchicalStructure(String currentPath, String jsonStr) {
        LogUtil.info(ZipFileUtil.class.getName(), "Processing hierarchical structure at path: " + currentPath + ", JSON length: " + jsonStr.length());
        
        try {
            // More robust JSON parsing for nested structures
            int depth = 0;
            StringBuilder currentKey = new StringBuilder();
            boolean inQuotes = false;
            boolean keyComplete = false;
            StringBuilder nestedJson = new StringBuilder();
            boolean collectingNestedJson = false;
            int nestedStartDepth = 0;
            
            for (int i = 0; i < jsonStr.length(); i++) {
                char c = jsonStr.charAt(i);
                
                // Handle escape sequences in quotes
                if (c == '\\' && i + 1 < jsonStr.length() && inQuotes) {
                    if (collectingNestedJson) {
                        nestedJson.append(c);
                        nestedJson.append(jsonStr.charAt(i + 1));
                    } else if (!keyComplete) {
                        currentKey.append(c);
                        currentKey.append(jsonStr.charAt(i + 1));
                    }
                    i++; // Skip the escaped character
                    continue;
                }
                
                // Handle quotes
                if (c == '"') {
                    inQuotes = !inQuotes;
                    if (collectingNestedJson) {
                        nestedJson.append(c);
                        continue;
                    }
                    
                    if (!inQuotes && !keyComplete && currentKey.length() > 0) {
                        keyComplete = true;
                    } else if (inQuotes && !keyComplete && currentKey.length() == 0) {
                        // Start of a key, don't add the quote
                        continue;
                    }
                    continue;
                }
                
                // Collect key
                if (inQuotes && !keyComplete) {
                    currentKey.append(c);
                    continue;
                }
                
                // Look for key-value separator
                if (c == ':' && keyComplete && !collectingNestedJson) {
                    // Skip to the value
                    continue;
                }
                
                // Handle nested objects
                if (c == '{') {
                    depth++;
                    if (depth > 1) {
                        if (!collectingNestedJson) {
                            collectingNestedJson = true;
                            nestedStartDepth = depth;
                        }
                        nestedJson.append(c);
                    }
                    continue;
                }
                
                if (c == '}') {
                    if (collectingNestedJson) {
                        nestedJson.append(c);
                        depth--;
                        
                        // If we've closed the nested object we were collecting
                        if (depth < nestedStartDepth) {
                            collectingNestedJson = false;
                            String key = currentKey.toString();
                            String newPath = currentPath.isEmpty() ? key : currentPath + "/" + key;
                            
                            // Process the nested structure
                            processHierarchicalStructure(newPath, nestedJson.toString());
                            
                            // Reset for next key-value pair
                            currentKey = new StringBuilder();
                            keyComplete = false;
                            nestedJson = new StringBuilder();
                        }
                    } else {
                        depth--;
                    }
                    continue;
                }
                
                // Collect nested JSON
                if (collectingNestedJson) {
                    nestedJson.append(c);
                    continue;
                }
                
                // Handle null values (files)
                if (keyComplete && c == 'n' && i + 3 < jsonStr.length() && 
                    jsonStr.substring(i, i + 4).equals("null")) {
                    String key = currentKey.toString();
                    String newPath = currentPath.isEmpty() ? key : currentPath + "/" + key;
                    
                    // This is a file
                    knownProjectStructure.put(key, newPath);
                    LogUtil.info(ZipFileUtil.class.getName(), "Added file to hierarchical structure: " + key + " -> " + newPath);
                    
                    // Skip to end of null
                    i += 3;
                    
                    // Reset for next key-value pair
                    currentKey = new StringBuilder();
                    keyComplete = false;
                    continue;
                }
                
                // Skip whitespace and commas between key-value pairs
                if (c == ',' || Character.isWhitespace(c)) {
                    continue;
                }
                
                // If we get here with a complete key, something unexpected happened
                if (keyComplete && !collectingNestedJson) {
                    LogUtil.info(ZipFileUtil.class.getName(), "Unexpected character in JSON: " + c + " at position " + i);
                }
            }
        } catch (Exception e) {
            LogUtil.error(ZipFileUtil.class.getName(), e, "Error processing hierarchical structure at path: " + currentPath);
        }
    }
    
    /**
     * Extract project structure from directory tree format in the chat content
     * 
     * @param chatContent The chat content that might contain directory tree
     * @return True if project structure was found and extracted, false otherwise
     */
    private static boolean extractProjectStructureFromDirectoryTree(String chatContent) {
        LogUtil.info(ZipFileUtil.class.getName(), "Looking for directory tree project structure in chat content");
        
        // Look for directory tree pattern
        Pattern treePattern = Pattern.compile("(?:[|+`]-|-+)\\s+([\\w\\-./]+\\.[a-zA-Z0-9]+)");
        Matcher treeMatcher = treePattern.matcher(chatContent);
        
        boolean found = false;
        while (treeMatcher.find()) {
            String filePath = treeMatcher.group(1).trim();
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
            
            // Try to reconstruct the full path from the directory tree
            String fullPath = reconstructPathFromDirectoryTree(chatContent, treeMatcher.start());
            if (fullPath != null && !fullPath.isEmpty()) {
                knownProjectStructure.put(fileName, fullPath);
                LogUtil.info(ZipFileUtil.class.getName(), "Added from directory tree: " + fileName + " -> " + fullPath);
                found = true;
            } else {
                knownProjectStructure.put(fileName, filePath);
                LogUtil.info(ZipFileUtil.class.getName(), "Added from directory tree (simple): " + fileName + " -> " + filePath);
                found = true;
            }
        }
        
        return found;
    }
    
    /**
     * Reconstruct full path from directory tree context
     * 
     * @param content The content containing the directory tree
     * @param position The position of the file in the content
     * @return The reconstructed full path or null if not possible
     */
    private static String reconstructPathFromDirectoryTree(String content, int position) {
        // Find the start of the directory tree (look for a line with a root folder)
        int treeStart = content.lastIndexOf('\n', position);
        while (treeStart > 0 && !content.substring(treeStart, Math.min(treeStart + 20, content.length())).matches(".*[\\w\\-./]+/.*")) {
            treeStart = content.lastIndexOf('\n', treeStart - 1);
        }
        
        if (treeStart < 0) {
            return null;
        }
        
        // Extract the relevant part of the tree
        String treeSection = content.substring(treeStart, Math.min(position + 100, content.length()));
        String[] lines = treeSection.split("\n");
        
        StringBuilder path = new StringBuilder();
        int currentIndent = -1;
        String[] folders = new String[20]; // Max depth
        
        for (String line : lines) {
            // Skip empty lines
            if (line.trim().isEmpty()) {
                continue;
            }
            
            // Calculate indent level
            int indent = 0;
            while (indent < line.length() && (line.charAt(indent) == ' ' || line.charAt(indent) == '|' || 
                   line.charAt(indent) == '+' || line.charAt(indent) == '`' || line.charAt(indent) == '-')) {
                indent++;
            }
            
            // Extract folder/file name
            String name = line.substring(indent).trim();
            if (name.isEmpty()) {
                continue;
            }
            
            // If this is a folder (ends with /)
            if (name.endsWith("/")) {
                if (currentIndent < 0 || indent > currentIndent) {
                    // Going deeper
                    currentIndent = indent;
                    folders[currentIndent / 2] = name;
                } else if (indent <= currentIndent) {
                    // Same level or going back up
                    currentIndent = indent;
                    folders[currentIndent / 2] = name;
                    // Clear deeper levels
                    for (int i = (currentIndent / 2) + 1; i < folders.length; i++) {
                        folders[i] = null;
                    }
                }
            } 
            // If this is a file (contains a dot)
            else if (name.contains(".")) {
                // Build the path
                path.setLength(0);
                for (int i = 0; i <= currentIndent / 2; i++) {
                    if (folders[i] != null) {
                        path.append(folders[i]);
                    }
                }
                path.append(name);
                return path.toString();
            }
        }
        
        return null;
    }
    
    /**
     * Initialize the project structure map with known paths from the response
     * 
     * @param chatContent The chat content to analyze for project structure, or null to use defaults
     */
    private static void initializeProjectStructure(String chatContent) {
        if (projectStructureInitialized) {
            return;
        }
        
        // Extract JSON project structure - only use JSON format as requested
        boolean foundJson = false;
        if (chatContent != null) {
            foundJson = extractProjectStructureFromJson(chatContent);
            LogUtil.info(ZipFileUtil.class.getName(), "JSON project structure extraction result: " + (foundJson ? "found" : "not found"));
        }
        
        // Add default paths if structure is still empty
        if (knownProjectStructure.isEmpty()) {
            // Add paths matching the provided example structure
            String projectRoot = "hello-world-plugin";
            
            // Basic project files
            knownProjectStructure.put("pom.xml", projectRoot + "/pom.xml");
            
            // Java source files
            String javaPath = projectRoot + "/src/main/java/com/example/joget/plugin/";
            knownProjectStructure.put("HelloWorldElement.java", javaPath + "HelloWorldElement.java");
            
            // Resource files
            String resourcesPath = projectRoot + "/src/main/resources/";
            knownProjectStructure.put("plugin.properties", resourcesPath + "plugin.properties");
            knownProjectStructure.put("plugin.json", resourcesPath + "plugin.json");
            
            // Static resources
            String staticPath = resourcesPath + "static/";
            knownProjectStructure.put("HelloWorldComponent.jsx", staticPath + "HelloWorldComponent.jsx");
            knownProjectStructure.put("hello-world.css", staticPath + "hello-world.css");
        }
        
        // Mark as initialized
        projectStructureInitialized = true;
        LogUtil.info(ZipFileUtil.class.getName(), "Project structure initialized with " + knownProjectStructure.size() + " paths");
    }
    
    private static String determineFilePath(String language, String code, int snippetCounter, String suggestedFileName) {
        String fileExtension = getFileExtensionForLanguage(language);
        String folderPath = "";
        String fileName = "";
        
        // Initialize the project structure map if not done yet
        initializeProjectStructure(null);
        
        LogUtil.info(ZipFileUtil.class.getName(), "Determining file path for language: " + language + 
                     ", suggested file name: " + suggestedFileName);
        
        // If a suggested file name was provided, use it exactly as specified
        if (suggestedFileName != null && !suggestedFileName.trim().isEmpty()) {
            // Check if it's a full path with directory structure
            if (suggestedFileName.contains("/")) {
                // Use the path exactly as specified in the response
                String path = suggestedFileName;
                
                // Ensure it doesn't start with a slash
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                
                LogUtil.info(ZipFileUtil.class.getName(), "Using exact path from response: " + path);
                return path;
            } else {
                // It's just a filename - check if it's in our known structure
                if (knownProjectStructure.containsKey(suggestedFileName)) {
                    String path = knownProjectStructure.get(suggestedFileName);
                    LogUtil.info(ZipFileUtil.class.getName(), "Found file in known project structure: " + path);
                    return path;
                }
                
                // Extract the base filename without path
                fileName = suggestedFileName;
                String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                
                // For Java files, check if it's a specific known class
                if ("java".equals(ext)) {
                    // Check if it's a specific known Java class
                    if (fileName.contains("HelloWorld") || fileName.contains("Plugin") || 
                        fileName.contains("Activity") || fileName.contains("Element")) {
                        // Try to extract package from code
                        Pattern packagePattern = Pattern.compile("package\\s+([A-Za-z0-9_.]+)");
                        Matcher packageMatcher = packagePattern.matcher(code);
                        
                        if (packageMatcher.find()) {
                            String packageName = packageMatcher.group(1);
                            // Use the exact package structure from the code
                            folderPath = "src/main/java/" + packageName.replace('.', '/') + "/";
                            LogUtil.info(ZipFileUtil.class.getName(), "Using Java package structure: " + folderPath);
                            return folderPath + fileName;
                        } else {
                            // Use the default structure for main Java files
                            folderPath = "src/main/java/com/example/joget/plugin/";
                            LogUtil.info(ZipFileUtil.class.getName(), "Using default Java structure: " + folderPath);
                            return folderPath + fileName;
                        }
                    } else {
                        // Not a recognized main class, put in other folder
                        folderPath = "other/java/";
                    }
                } 
                // For JavaScript files
                else if ("js".equals(ext)) {
                    // Check if it's a specific known JS file
                    if (fileName.equals("helloWorld.js")) {
                        folderPath = "src/resources/app/helloWorld/";
                    } else {
                        // Not a recognized JS file, put in other folder
                        folderPath = "other/js/";
                    }
                } 
                // For HTML files
                else if ("html".equals(ext)) {
                    // Check if it's a specific known HTML file
                    if (fileName.equals("helloWorld.html")) {
                        folderPath = "src/resources/app/helloWorld/";
                    } else {
                        // Not a recognized HTML file, put in other folder
                        folderPath = "other/html/";
                    }
                } 
                // For properties files
                else if ("properties".equals(ext)) {
                    // Check if it's plugin.properties
                    if (fileName.equals("plugin.properties")) {
                        folderPath = "src/resources/";
                    } else {
                        // Not a recognized properties file, put in other folder
                        folderPath = "other/properties/";
                    }
                } 
                // For XML files
                else if ("xml".equals(ext)) {
                    // Check if it's plugin.xml
                    if (fileName.equals("plugin.xml")) {
                        folderPath = "src/resources/";
                    } else {
                        // Not a recognized XML file, put in other folder
                        folderPath = "other/xml/";
                    }
                } 
                // For all other files
                else {
                    // Put in other folder by extension
                    folderPath = "other/" + ext + "/";
                }
                
                LogUtil.info(ZipFileUtil.class.getName(), "Using filename with determined folder: " + folderPath + fileName);
                return folderPath + fileName;
            }
        }
        
        // If no suggested file name was found, determine based on content
        
        // Try to determine project structure based on code content
        if ("java".equals(language.toLowerCase())) {
            // For Java, try to extract the package and class name
            Pattern packagePattern = Pattern.compile("package\\s+([A-Za-z0-9_.]+)");
            Matcher packageMatcher = packagePattern.matcher(code);
            
            // Try to extract class name
            Pattern classPattern = Pattern.compile("(public\\s+)?class\\s+([A-Za-z0-9_]+)");
            Matcher classMatcher = classPattern.matcher(code);
            
            if (classMatcher.find()) {
                String className = classMatcher.group(2);
                fileName = className + fileExtension;
                
                // Check if it's a recognized class from the project structure
                if (className.contains("HelloWorld") || className.contains("Plugin") || 
                    className.contains("Activity") || className.contains("Element")) {
                    
                    if (packageMatcher.find()) {
                        String packageName = packageMatcher.group(1);
                        // Check if it matches the expected package
                        if (packageName.contains("example")) {
                            folderPath = "hello-world-plugin/src/main/java/" + packageName.replace('.', '/') + "/";
                        } else {
                            // Package doesn't match expected structure, put in other
                            folderPath = "hello-world-plugin/src/main/java/other/";
                        }
                    } else {
                        // No package but recognized class, use default structure
                        folderPath = "hello-world-plugin/src/main/java/com/example/joget/plugin/";
                    }
                } else {
                    // Not a recognized class, put in other folder
                    folderPath = "hello-world-plugin/src/main/java/other/";
                }
            } else {
                // No class name found
                fileName = "Unknown" + snippetCounter + fileExtension;
                folderPath = "hello-world-plugin/src/main/java/other/";
            }
        } else if ("javascript".equals(language.toLowerCase()) || "js".equals(language.toLowerCase())) {
            // Try to extract function or class name
            Pattern functionPattern = Pattern.compile("(function|class)\\s+([A-Za-z0-9_]+)");
            Matcher functionMatcher = functionPattern.matcher(code);
            
            if (functionMatcher.find()) {
                fileName = functionMatcher.group(2) + fileExtension;
            } else {
                fileName = "script" + snippetCounter + fileExtension;
            }
            
            // Check if it's a recognized file from the project structure
            if (fileName.equals("main.js") || fileName.equals("app.js")) {
                folderPath = "hello-world-plugin/src/main/resources/static/";
            } else {
                // Not recognized, put in other folder
                folderPath = "hello-world-plugin/src/main/resources/static/";
            }
        } else if ("html".equals(language.toLowerCase())) {
            // Try to extract title
            Pattern titlePattern = Pattern.compile("<title>(.*?)</title>");
            Matcher titleMatcher = titlePattern.matcher(code);
            
            if (titleMatcher.find()) {
                fileName = titleMatcher.group(1).replaceAll("\\s+", "_").toLowerCase() + fileExtension;
            } else {
                fileName = "page" + snippetCounter + fileExtension;
            }
            
            // Check if it's a recognized file from the project structure
            if (fileName.equals("index.html") || fileName.contains("page")) {
                folderPath = "hello-world-plugin/src/main/resources/static/";
            } else {
                // Not recognized, put in other folder
                folderPath = "hello-world-plugin/src/main/resources/static/";
            }
        } else if ("properties".equals(language.toLowerCase())) {
            // Check if it's plugin.properties
            if (code.contains("plugin.id") || code.contains("plugin.name") || 
                code.contains("plugin.version") || code.contains("plugin.class")) {
                fileName = "plugin.properties";
                folderPath = "hello-world-plugin/src/main/resources/";
            } else {
                fileName = "config" + snippetCounter + ".properties";
                folderPath = "hello-world-plugin/src/main/resources/";
            }
        } else if ("xml".equals(language.toLowerCase())) {
            // Check if it's plugin.xml
            if (code.contains("<plugin>") && (code.contains("<id>") || code.contains("<class>"))) {
                fileName = "plugin.xml";
                folderPath = "hello-world-plugin/src/main/resources/";
            } else {
                fileName = "config" + snippetCounter + ".xml";
                folderPath = "hello-world-plugin/src/main/resources/";
            }
        } else if ("css".equals(language.toLowerCase())) {
            folderPath = "hello-world-plugin/src/main/resources/static/";
            fileName = "style" + snippetCounter + fileExtension;
        } else if ("sql".equals(language.toLowerCase())) {
            folderPath = "hello-world-plugin/src/main/resources/";
            fileName = "query" + snippetCounter + fileExtension;
        } else if ("json".equals(language.toLowerCase())) {
            folderPath = "hello-world-plugin/src/main/resources/";
            fileName = "config" + snippetCounter + fileExtension;
        } else if ("python".equals(language.toLowerCase()) || "py".equals(language.toLowerCase())) {
            // Try to extract class or function name
            Pattern defPattern = Pattern.compile("def\\s+([A-Za-z0-9_]+)");
            Matcher defMatcher = defPattern.matcher(code);
            
            if (defMatcher.find()) {
                fileName = defMatcher.group(1) + fileExtension;
            } else {
                Pattern classPattern = Pattern.compile("class\\s+([A-Za-z0-9_]+)");
                Matcher classMatcher = classPattern.matcher(code);
                
                if (classMatcher.find()) {
                    fileName = classMatcher.group(1) + fileExtension;
                } else {
                    fileName = "script" + snippetCounter + fileExtension;
                }
            }
            
            // Put in other folder
            folderPath = "hello-world-plugin/src/main/resources/";
        } else {
            // For other languages, use the other folder
            folderPath = "hello-world-plugin/src/main/resources/other/" + language.toLowerCase() + "/";
            fileName = "snippet_" + snippetCounter + fileExtension;
        }
        
        return folderPath + fileName;
    }
    
    /**
     * Guesses the programming language based on code content
     * 
     * @param code The code content
     * @return The guessed programming language
     */
    private static String guessLanguageFromContent(String code) {
        String language = "txt";
        
        if (code.contains("class") && code.contains("function")) {
            language = "javascript";
        } else if (code.contains("public class") || code.contains("private class") || code.contains("package ")) {
            language = "java";
        } else if (code.contains("def ") && code.contains(":")) {
            language = "python";
        } else if (code.contains("<html>") || code.contains("<!DOCTYPE")) {
            language = "html";
        } else if (code.contains("function") && code.contains("{") && code.contains("}")) {
            language = "javascript";
        } else if (code.contains("import React") || code.contains("useState") || code.contains("useEffect")) {
            language = "jsx";
        } else if (code.contains("#include") && (code.contains("int main") || code.contains("void main"))) {
            language = "c";
        } else if (code.contains("<?php")) {
            language = "php";
        } else if (code.contains("SELECT") && code.contains("FROM") && (code.contains("WHERE") || code.contains("JOIN"))) {
            language = "sql";
        } else if (code.contains("<style>") || code.contains("{") && code.contains("}") && 
                  (code.contains("margin") || code.contains("padding") || code.contains("color"))) {
            language = "css";
        }
        
        return language;
    }
    
    /**
     * Gets the file extension for a programming language
     * 
     * @param language The programming language
     * @return The file extension for the language
     */
    private static String getFileExtensionForLanguage(String language) {
        switch (language.toLowerCase()) {
            case "java":
                return ".java";
            case "javascript":
            case "js":
                return ".js";
            case "typescript":
            case "ts":
                return ".ts";
            case "html":
                return ".html";
            case "css":
                return ".css";
            case "python":
            case "py":
                return ".py";
            case "c":
                return ".c";
            case "cpp":
            case "c++":
                return ".cpp";
            case "csharp":
            case "c#":
                return ".cs";
            case "php":
                return ".php";
            case "ruby":
                return ".rb";
            case "go":
                return ".go";
            case "rust":
                return ".rs";
            case "swift":
                return ".swift";
            case "kotlin":
                return ".kt";
            case "scala":
                return ".scala";
            case "sql":
                return ".sql";
            case "xml":
                return ".xml";
            case "json":
                return ".json";
            case "yaml":
            case "yml":
                return ".yml";
            case "markdown":
            case "md":
                return ".md";
            case "bash":
            case "sh":
                return ".sh";
            case "powershell":
            case "ps1":
                return ".ps1";
            case "dockerfile":
                return ".dockerfile";
            default:
                return ".txt";
        }
    }
}
