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

public class LlmApiClient {
    
    private static final String DEFAULT_OPENAI_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    
    /**
     * Calls the OpenAI API with the given parameters
     * 
     * @param message User message to send to the API
     * @param apiKey API key for authentication
     * @param apiEndpoint API endpoint URL (defaults to OpenAI if null)
     * @param model Model name to use (e.g., "gpt-4")
     * @param systemPrompt System prompt to set context
     * @param temperature Temperature parameter (0.0 to 1.0)
     * @param maxTokens Maximum tokens to generate
     * @return The response from the LLM
     * @throws IOException If there's an error communicating with the API
     * @throws JSONException If there's an error parsing the JSON response
     */
    public static String callOpenAiApi(String message, String apiKey, String apiEndpoint, 
            String model, String systemPrompt, double temperature, int maxTokens) 
            throws IOException, JSONException {
        
        if (apiEndpoint == null || apiEndpoint.trim().isEmpty()) {
            apiEndpoint = DEFAULT_OPENAI_ENDPOINT;
        }
        
        if (model == null || model.trim().isEmpty()) {
            model = "gpt-4";
        }
        
        if (systemPrompt == null) {
            systemPrompt = "You are a helpful assistant.";
        }
        
        URL url = new URL(apiEndpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setDoOutput(true);
        
        // Create the request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        
        // Add messages
        JSONArray messages = new JSONArray();
        
        // System message
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.put(systemMessage);
        
        // User message
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", message);
        messages.put(userMessage);
        
        requestBody.put("messages", messages);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);
        
        // Send the request
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
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
        JSONObject jsonResponse = new JSONObject(response.toString());
        JSONArray choices = jsonResponse.getJSONArray("choices");
        if (choices.length() > 0) {
            JSONObject choice = choices.getJSONObject(0);
            JSONObject responseMessage = choice.getJSONObject("message");
            return responseMessage.getString("content");
        } else {
            throw new IOException("No response from LLM API");
        }
    }
    
    /**
     * Generic method to call any LLM API with custom parameters
     * 
     * @param message User message
     * @param apiKey API key
     * @param apiEndpoint API endpoint URL
     * @param requestParams Additional request parameters
     * @param headers Additional HTTP headers
     * @return The response from the LLM
     * @throws IOException If there's an error communicating with the API
     */
    public static String callGenericLlmApi(String message, String apiKey, String apiEndpoint,
            Map<String, Object> requestParams, Map<String, String> headers) 
            throws IOException {
        
        URL url = new URL(apiEndpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        
        // Set authorization header if API key is provided
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
        
        // Set additional headers
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        
        connection.setDoOutput(true);
        
        // Create the request body
        JSONObject requestBody = new JSONObject();
        
        // Add message to request params
        if (requestParams == null) {
            requestParams = new HashMap<>();
        }
        requestParams.put("message", message);
        
        // Add all request parameters to the JSON body
        for (Map.Entry<String, Object> entry : requestParams.entrySet()) {
            requestBody.put(entry.getKey(), entry.getValue());
        }
        
        // Send the request
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        // Check if the request was successful
        int responseCode = connection.getResponseCode();
        if (responseCode >= 400) {
            StringBuilder errorResponse = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    errorResponse.append(responseLine.trim());
                }
            }
            throw new IOException("API error (code " + responseCode + "): " + errorResponse.toString());
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
        
        return response.toString();
    }
}
