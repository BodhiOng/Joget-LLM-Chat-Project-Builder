package org.joget.marketplace;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.userview.model.UserviewBuilderPalette;
import org.joget.apps.userview.model.UserviewMenu;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.StringUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.json.JSONException;
import org.json.JSONObject;

public class LlmChatUserviewMenu extends UserviewMenu implements PluginWebSupport {

    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getName() {
        return "LLM Chat Interface";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getLabel() {
        return "LLM Chat Interface";
    }

    @Override
    public String getIcon() {
        return "<i class=\"fas fa-comments\"></i>";
    }

    @Override
    public String getRenderPage() {
        // Implement direct HTML rendering as a fallback
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"llm-chat-container\" style=\"max-width: 800px; margin: 0 auto; padding: 20px;\">\n");
        html.append("    <h2><i class=\"fas fa-comments\"></i> LLM Chat</h2>\n");
        html.append("    <div id=\"chatMessages\" style=\"height: 500px; overflow-y: auto; border: 1px solid #ddd; border-radius: 5px; padding: 10px; margin-bottom: 15px; background-color: #f9f9f9;\">\n");
        html.append("        <div style=\"margin-bottom: 15px; padding: 10px; border-radius: 5px; max-width: 80%; background-color: #ffffff; margin-right: auto; margin-left: 10px; border: 1px solid #e0e0e0;\">\n");
        html.append("            Hello! How can I assist you today?\n");
        html.append("        </div>\n");
        html.append("    </div>\n");
        html.append("    <div style=\"display: flex; margin-top: 10px;\">\n");
        html.append("        <textarea id=\"messageInput\" placeholder=\"Type your message here...\" style=\"flex-grow: 1; padding: 10px; border: 1px solid #ddd; border-radius: 5px; resize: none; height: 60px;\"></textarea>\n");
        html.append("        <button id=\"sendButton\" style=\"margin-left: 10px; padding: 10px 20px; background-color: #4CAF50; color: white; border: none; border-radius: 5px; cursor: pointer;\"><i class=\"fas fa-paper-plane\"></i> Send</button>\n");
        html.append("    </div>\n");
        html.append("    <div id=\"typingIndicator\" style=\"display: none; margin-left: 10px; font-style: italic; color: #666;\">AI is thinking...</div>\n");
        html.append("    <div id=\"errorMessage\" style=\"color: red; margin-top: 10px; display: none;\"></div>\n");
        html.append("</div>\n");
        
        // Add JavaScript for chat functionality
        html.append("<script>\n");
        html.append("$(document).ready(function() {\n");
        html.append("    const chatMessages = $('#chatMessages');\n");
        html.append("    const messageInput = $('#messageInput');\n");
        html.append("    const sendButton = $('#sendButton');\n");
        html.append("    const typingIndicator = $('#typingIndicator');\n");
        html.append("    const errorMessage = $('#errorMessage');\n");
        html.append("    \n");
        html.append("    // Function to add a message to the chat\n");
        html.append("    function addMessage(message, isUser) {\n");
        html.append("        const messageDiv = $('<div>').css({\n");
        html.append("            'margin-bottom': '15px',\n");
        html.append("            'padding': '10px',\n");
        html.append("            'border-radius': '5px',\n");
        html.append("            'max-width': '80%',\n");
        html.append("            'word-wrap': 'break-word'\n");
        html.append("        });\n");
        html.append("        \n");
        html.append("        if (isUser) {\n");
        html.append("            messageDiv.css({\n");
        html.append("                'background-color': '#dcf8c6',\n");
        html.append("                'margin-left': 'auto',\n");
        html.append("                'margin-right': '10px'\n");
        html.append("            });\n");
        html.append("        } else {\n");
        html.append("            messageDiv.css({\n");
        html.append("                'background-color': '#ffffff',\n");
        html.append("                'margin-right': 'auto',\n");
        html.append("                'margin-left': '10px',\n");
        html.append("                'border': '1px solid #e0e0e0'\n");
        html.append("            });\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        messageDiv.text(message);\n");
        html.append("        chatMessages.append(messageDiv);\n");
        html.append("        chatMessages.scrollTop(chatMessages[0].scrollHeight);\n");
        html.append("    }\n");
        html.append("    \n");
        html.append("    // Function to send a message to the LLM API\n");
        html.append("    function sendMessage() {\n");
        html.append("        const message = messageInput.val().trim();\n");
        html.append("        if (!message) return;\n");
        html.append("        \n");
        html.append("        // Add user message to chat\n");
        html.append("        addMessage(message, true);\n");
        html.append("        \n");
        html.append("        // Clear input\n");
        html.append("        messageInput.val('');\n");
        html.append("        \n");
        html.append("        // Disable send button and show typing indicator\n");
        html.append("        sendButton.prop('disabled', true);\n");
        html.append("        typingIndicator.show();\n");
        html.append("        errorMessage.hide();\n");
        html.append("        \n");
        html.append("        // Send message to server\n");
        html.append("        $.ajax({\n");
        html.append("            url: '" + getUrl() + "&action=sendMessage',\n");
        html.append("            type: 'POST',\n");
        html.append("            data: {\n");
        html.append("                message: message\n");
        html.append("            },\n");
        html.append("            dataType: 'json',\n");
        html.append("            success: function(data) {\n");
        html.append("                // Add bot response to chat\n");
        html.append("                addMessage(data.response, false);\n");
        html.append("            },\n");
        html.append("            error: function(xhr, status, error) {\n");
        html.append("                // Show error message\n");
        html.append("                let errorText = 'Error communicating with the LLM API';\n");
        html.append("                try {\n");
        html.append("                    const response = JSON.parse(xhr.responseText);\n");
        html.append("                    if (response.error) {\n");
        html.append("                        errorText = response.error;\n");
        html.append("                    }\n");
        html.append("                } catch (e) {\n");
        html.append("                    console.error('Error parsing error response:', e);\n");
        html.append("                }\n");
        html.append("                errorMessage.text(errorText).show();\n");
        html.append("            },\n");
        html.append("            complete: function() {\n");
        html.append("                // Re-enable send button and hide typing indicator\n");
        html.append("                sendButton.prop('disabled', false);\n");
        html.append("                typingIndicator.hide();\n");
        html.append("            }\n");
        html.append("        });\n");
        html.append("    }\n");
        html.append("    \n");
        html.append("    // Send message when button is clicked\n");
        html.append("    sendButton.click(sendMessage);\n");
        html.append("    \n");
        html.append("    // Send message when Enter key is pressed (but allow Shift+Enter for new lines)\n");
        html.append("    messageInput.keydown(function(event) {\n");
        html.append("        if (event.keyCode === 13 && !event.shiftKey) {\n");
        html.append("            event.preventDefault();\n");
        html.append("            sendMessage();\n");
        html.append("        }\n");
        html.append("    });\n");
        html.append("});\n");
        html.append("</script>\n");
        
        return html.toString();
    }

    @Override
    public String getCategory() {
        return UserviewBuilderPalette.CATEGORY_GENERAL;
    }

    @Override
    public boolean isHomePageSupported() {
        return true;
    }

    @Override
    public String getDescription() {
        return "A chat interface for interacting with LLM APIs";
    }

    @Override
    public String getPropertyOptions() {
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String appId = appDef.getId();
        String appVersion = appDef.getVersion().toString();
        
        Object[] arguments = new Object[]{appId, appVersion};
        
        String json = AppUtil.readPluginResource(getClass().getName(), "/properties/llmChatUserviewMenu.json", arguments, true, "messages/llmChatUserviewMenu");
        return json;
    }

    @Override
    public String getDecoratedMenu() {
        return "<a href=\"" + getUrl() + "\" class=\"menu-link\"><span class=\"menu-icon\"><i class=\"fas fa-comments\"></i></span><span class=\"menu-label\">" + getPropertyString("label") + "</span></a>";
    }

    @Override
    public String getJspPage() {
        // Return null to force using getRenderPage instead
        LogUtil.info(getClassName(), "Using direct HTML rendering instead of JSP template");
        return null;
    }

    @Override
    public String getReadyJspPage() {
        return null;
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        
        if ("sendMessage".equals(action)) {
            try {
                // Get message from request
                String message = request.getParameter("message");
                String apiKey = getPropertyString("apiKey");
                String apiEndpoint = getPropertyString("apiEndpoint");
                String model = getPropertyString("model");
                String systemPrompt = getPropertyString("systemPrompt");
                
                // Get temperature and maxTokens with default values
                double temperature = 0.7;
                int maxTokens = 1000;
                
                try {
                    String tempStr = getPropertyString("temperature");
                    if (tempStr != null && !tempStr.isEmpty()) {
                        temperature = Double.parseDouble(tempStr);
                    }
                } catch (NumberFormatException e) {
                    LogUtil.warn(getClassName(), "Invalid temperature value, using default 0.7");
                }
                
                try {
                    String tokensStr = getPropertyString("maxTokens");
                    if (tokensStr != null && !tokensStr.isEmpty()) {
                        maxTokens = Integer.parseInt(tokensStr);
                    }
                } catch (NumberFormatException e) {
                    LogUtil.warn(getClassName(), "Invalid maxTokens value, using default 1000");
                }
                
                // Call LLM API
                String result;
                
                // Check if the endpoint is OpenAI or another provider
                if (apiEndpoint == null || apiEndpoint.isEmpty() || apiEndpoint.contains("openai.com")) {
                    // Use OpenAI-specific implementation
                    result = LlmApiClient.callOpenAiApi(message, apiKey, apiEndpoint, model, systemPrompt, temperature, maxTokens);
                } else {
                    // Use generic implementation for other providers
                    Map<String, Object> params = new HashMap<>();
                    params.put("model", model);
                    params.put("temperature", temperature);
                    params.put("max_tokens", maxTokens);
                    if (systemPrompt != null && !systemPrompt.isEmpty()) {
                        params.put("system_prompt", systemPrompt);
                    }
                    
                    result = LlmApiClient.callGenericLlmApi(message, apiKey, apiEndpoint, params, null);
                    
                    // Try to parse the response if it's JSON
                    try {
                        JSONObject jsonResult = new JSONObject(result);
                        if (jsonResult.has("response")) {
                            result = jsonResult.getString("response");
                        } else if (jsonResult.has("text")) {
                            result = jsonResult.getString("text");
                        } else if (jsonResult.has("content")) {
                            result = jsonResult.getString("content");
                        }
                    } catch (JSONException e) {
                        // Not JSON or couldn't parse, use the raw response
                        LogUtil.debug(getClassName(), "Response is not JSON or couldn't be parsed: " + e.getMessage());
                    }
                }
                
                // Return response
                response.setContentType("application/json");
                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put("response", result);
                response.getWriter().write(jsonResponse.toString());
                
            } catch (Exception e) {
                LogUtil.error(getClassName(), e, "Error calling LLM API: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\": \"" + StringUtil.escapeString(e.getMessage(), StringUtil.TYPE_JSON, null) + "\"}");
            }
        }
    }
}
