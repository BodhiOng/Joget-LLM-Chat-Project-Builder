package org.joget.marketplace;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.joget.commons.util.LogUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OllamaApiClient {

    private static final String DEFAULT_OLLAMA_ENDPOINT = "http://localhost:11434/api/generate";

    /**
     * Calls the Ollama API with the given parameters
     * 
     * @param message      User message to send to the API
     * @param apiEndpoint  API endpoint URL (defaults to Ollama local endpoint if
     *                     null)
     * @param model        Model name to use (e.g., "llama2")
     * @param systemPrompt System prompt to set context
     * @param temperature  Temperature parameter (0.0 to 1.0)
     * @return The response from the LLM
     * @throws IOException   If there's an error communicating with the API
     * @throws JSONException If there's an error parsing the JSON response
     */
    public static String callOllamaApi(String message, String apiEndpoint,
            String model, String systemPrompt, double temperature)
            throws IOException, JSONException {

        if (apiEndpoint == null || apiEndpoint.trim().isEmpty()) {
            apiEndpoint = DEFAULT_OLLAMA_ENDPOINT;
        }

        if (model == null || model.trim().isEmpty()) {
            model = "gpt-oss:120b-cloud";
        }

        URL url = new URL(apiEndpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Create the request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        requestBody.put("prompt", message);

        // Enhance system prompt to request project structure in JSON format for
        // code-related prompts
        String enhancedSystemPrompt = systemPrompt + "\n\nIMPORTANT INSTRUCTION FOR CODE RESPONSES:\n" +
                "When answering ANY coding-related questions, you MUST include a project structure in JSON format. " +
                "Always add a 'projectStructure' JSON object at the end of your response with this exact format:\n" +
                "```json\n{\"projectStructure\": {\n  \"filename1\": \"path/to/file1\",\n  \"filename2\": \"path/to/file2\"\n}}\n```\n"
                +
                "The keys should be filenames and values should be full paths. " +
                "Example: {\"projectStructure\": {\"HelloWorldElement.java\": \"src/main/java/com/example/joget/plugin/HelloWorldElement.java\"}}\n"
                +
                "This is REQUIRED for ALL code-related responses without exception.";

        requestBody.put("system", enhancedSystemPrompt);

        // Add temperature
        requestBody.put("temperature", temperature);

        // Set stream to false to get the complete response at once
        requestBody.put("stream", false);

        // Log the request payload
        String requestPayload = requestBody.toString();
        LogUtil.info(OllamaApiClient.class.getName(), "Non-streaming request payload: " + requestPayload);

        // Send the request
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Check if the request was successful
        int responseCode = connection.getResponseCode();
        if (responseCode >= 400) {
            StringBuilder errorResponse = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            connection.getErrorStream() != null ? connection.getErrorStream()
                                    : new java.io.ByteArrayInputStream(new byte[0]),
                            StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    errorResponse.append(responseLine.trim());
                }
            }

            String errorMessage = "Ollama API error (code " + responseCode + ")";
            if (errorResponse.length() > 0) {
                errorMessage += ": " + errorResponse.toString();
            }

            LogUtil.error(OllamaApiClient.class.getName(), null, errorMessage);
            throw new IOException(errorMessage);
        }

        // Read the response
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        // Parse the response
        try {
            String responseStr = response.toString();
            LogUtil.debug(OllamaApiClient.class.getName(), "Raw Ollama response: " + responseStr);

            // Check if the response is empty
            if (responseStr == null || responseStr.trim().isEmpty()) {
                String errorMsg = "Empty response from Ollama API";
                LogUtil.error(OllamaApiClient.class.getName(), null, errorMsg);
                throw new IOException(errorMsg);
            }

            try {
                JSONObject jsonResponse = new JSONObject(responseStr);
                if (jsonResponse.has("response")) {
                    return jsonResponse.getString("response");
                } else if (jsonResponse.has("error")) {
                    String errorMsg = "Ollama API error: " + jsonResponse.getString("error");
                    LogUtil.error(OllamaApiClient.class.getName(), null, errorMsg);
                    throw new IOException(errorMsg);
                } else {
                    String errorMsg = "Unexpected response format from Ollama API: " + responseStr;
                    LogUtil.error(OllamaApiClient.class.getName(), null, errorMsg);
                    throw new IOException(errorMsg);
                }
            } catch (JSONException e) {
                String errorMsg = "Failed to parse Ollama API response: " + e.getMessage() + ". Raw response: "
                        + responseStr;
                LogUtil.error(OllamaApiClient.class.getName(), e, errorMsg);
                throw new JSONException(errorMsg);
            }
        } catch (Exception e) {
            if (!(e instanceof JSONException) && !(e instanceof IOException)) {
                String errorMsg = "Unexpected error processing Ollama API response: " + e.getMessage();
                LogUtil.error(OllamaApiClient.class.getName(), e, errorMsg);
                throw new IOException(errorMsg);
            } else {
                throw e;
            }
        }
    }

    /**
     * Calls the Ollama API with streaming enabled
     * 
     * @param message      User message to send to the API
     * @param apiEndpoint  API endpoint URL (defaults to Ollama local endpoint if
     *                     null)
     * @param model        Model name to use (e.g., "llama2")
     * @param systemPrompt System prompt to set context
     * @param temperature  Temperature parameter (0.0 to 1.0)
     * @param callback     Callback function to handle streaming responses
     * @throws IOException   If there's an error communicating with the API
     * @throws JSONException If there's an error parsing the JSON response
     */
    public static void callOllamaApiStreaming(String message, String apiEndpoint,
            String model, String systemPrompt, double temperature,
            StreamingResponseCallback callback)
            throws IOException, JSONException {

        if (apiEndpoint == null || apiEndpoint.trim().isEmpty()) {
            apiEndpoint = DEFAULT_OLLAMA_ENDPOINT;
        }

        if (model == null || model.trim().isEmpty()) {
            model = "llama2";
        }

        URL url = new URL(apiEndpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Create the request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        requestBody.put("prompt", message);

        // Add system prompt if provided
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            // Enhance system prompt to request project structure in JSON format for
            // code-related prompts
            String enhancedSystemPrompt = systemPrompt + "\n\nIMPORTANT INSTRUCTION FOR CODE RESPONSES:\n" +
                    "When answering ANY coding-related questions, you MUST include a project structure in JSON format. "
                    +
                    "Always add a 'projectStructure' JSON object at the end of your response with this exact format:\n"
                    +
                    "```json\n{\"projectStructure\": {\n  \"filename1\": \"path/to/file1\",\n  \"filename2\": \"path/to/file2\"\n}}\n```\n"
                    +
                    "The keys should be filenames and values should be full paths. " +
                    "Example: {\"projectStructure\": {\"HelloWorldElement.java\": \"src/main/java/com/example/joget/plugin/HelloWorldElement.java\"}}\n"
                    +
                    "This is REQUIRED for ALL code-related responses without exception.";

            requestBody.put("system", enhancedSystemPrompt);
        }

        // Add temperature
        requestBody.put("temperature", temperature);

        // Set stream to true to get streaming responses
        requestBody.put("stream", true);

        // Log the request payload
        String requestPayload = requestBody.toString();
        LogUtil.info(OllamaApiClient.class.getName(), "Streaming request payload: " + requestPayload);

        // Send the request
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Check if the request was successful
        int responseCode = connection.getResponseCode();
        if (responseCode >= 400) {
            StringBuilder errorResponse = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            connection.getErrorStream() != null ? connection.getErrorStream()
                                    : new java.io.ByteArrayInputStream(new byte[0]),
                            StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    errorResponse.append(responseLine.trim());
                }
            }

            String errorMessage = "Ollama streaming API error (code " + responseCode + ")";
            if (errorResponse.length() > 0) {
                errorMessage += ": " + errorResponse.toString();
            }

            LogUtil.error(OllamaApiClient.class.getName(), null, errorMessage);
            throw new IOException(errorMessage);
        }

        // Read the streaming response
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                if (!responseLine.trim().isEmpty()) {
                    try {
                        String trimmedLine = responseLine.trim();
                        LogUtil.debug(OllamaApiClient.class.getName(), "Streaming chunk: " + trimmedLine);

                        JSONObject jsonResponse = new JSONObject(trimmedLine);
                        if (jsonResponse.has("response")) {
                            String responseChunk = jsonResponse.getString("response");
                            callback.onResponseChunk(responseChunk);
                        }

                        // Check if this is the final response
                        if (jsonResponse.has("done") && jsonResponse.getBoolean("done")) {
                            callback.onComplete();
                            break;
                        }
                    } catch (JSONException e) {
                        LogUtil.error(OllamaApiClient.class.getName(), e,
                                "Failed to parse streaming response: " + responseLine);
                        // Continue processing other chunks
                    }
                }
            }
        }
    }

    /**
     * Interface for handling streaming responses
     */
    public interface StreamingResponseCallback {
        void onResponseChunk(String chunk);

        void onComplete();
    }

    /**
     * Gets a list of available models from the Ollama API
     * 
     * @param apiEndpoint API endpoint URL (defaults to Ollama local endpoint if
     *                    null)
     * @return A map of model names and their descriptions
     * @throws IOException   If there's an error communicating with the API
     * @throws JSONException If there's an error parsing the JSON response
     */
    public static Map<String, String> getAvailableModels(String apiEndpoint)
            throws IOException, JSONException {

        if (apiEndpoint == null || apiEndpoint.trim().isEmpty()) {
            apiEndpoint = "http://localhost:11434/api/tags";
        } else {
            // Convert generate endpoint to tags endpoint if needed
            if (apiEndpoint.endsWith("/generate")) {
                apiEndpoint = apiEndpoint.substring(0, apiEndpoint.lastIndexOf("/")) + "/tags";
            } else if (!apiEndpoint.endsWith("/tags")) {
                // Ensure the endpoint ends with /tags
                if (apiEndpoint.endsWith("/")) {
                    apiEndpoint += "tags";
                } else {
                    apiEndpoint += "/tags";
                }
            }
        }

        URL url = new URL(apiEndpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Check if the request was successful
        int responseCode = connection.getResponseCode();
        if (responseCode >= 400) {
            StringBuilder errorResponse = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            connection.getErrorStream() != null ? connection.getErrorStream()
                                    : new java.io.ByteArrayInputStream(new byte[0]),
                            StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    errorResponse.append(responseLine.trim());
                }
            }

            String errorMessage = "Ollama models API error (code " + responseCode + ")";
            if (errorResponse.length() > 0) {
                errorMessage += ": " + errorResponse.toString();
            }

            LogUtil.error(OllamaApiClient.class.getName(), null, errorMessage);
            throw new IOException(errorMessage);
        }

        // Read the response
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        // Parse the response
        Map<String, String> models = new HashMap<>();
        JSONObject jsonResponse = new JSONObject(response.toString());
        if (jsonResponse.has("models")) {
            JSONArray modelsArray = jsonResponse.getJSONArray("models");
            for (int i = 0; i < modelsArray.length(); i++) {
                JSONObject model = modelsArray.getJSONObject(i);
                String name = model.getString("name");
                String description = name;
                if (model.has("details") && model.getJSONObject("details").has("parameter_size")) {
                    description += " (" + model.getJSONObject("details").getString("parameter_size") + ")";
                }
                models.put(name, description);
            }
        }

        return models;
    }
}
