package org.joget.marketplace;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
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

    /**
     * Utility method to send a JSON response
     * 
     * @param response The HttpServletResponse object
     * @param key      The key for the JSON object
     * @param value    The value for the JSON object
     * @throws IOException If there's an error writing to the response
     */
    private void sendJsonResponse(HttpServletResponse response, String key, String value) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        try {
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put(key, value);
            response.getWriter().write(jsonResponse.toString());
        } catch (JSONException e) {
            // Fallback if JSON creation fails
            response.getWriter().write("{\"" + key + "\":\"" +
                    StringUtil.escapeString(value, StringUtil.TYPE_JSON, null) + "\"}");
        }
    }

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
        html.append("<div class=\"llm-chat-container\" style=\"max-width: 1400px; margin: 0 auto; padding: 20px;\">\n");
        html.append("    <h2><i class=\"fas fa-comments\"></i> LLM Chat</h2>\n");
        html.append(
                "    <div id=\"chatMessages\" style=\"height: 500px; overflow-y: auto; border: 1px solid #ddd; border-radius: 5px; padding: 10px; margin-bottom: 15px; background-color: #f9f9f9;\">\n");
        html.append(
                "        <div class=\"message bot-message\"\n");
        html.append(
                "            style=\"background-color: #ffffff; margin-right: auto; margin-left: 10px; border: 1px solid #e0e0e0;\">\n");
        html.append("            Hello! How can I assist you today?\n");
        html.append("        </div>\n");
        html.append("    </div>\n");
        html.append("    <div style=\"display: flex; margin-top: 10px;\">\n");
        html.append(
                "        <textarea id=\"messageInput\" placeholder=\"Type your message here...\" style=\"flex-grow: 1; padding: 10px; border: 1px solid #ddd; border-radius: 5px; resize: none; height: 60px;\"></textarea>\n");
        html.append(
                "        <button id=\"sendButton\" style=\"margin-left: 10px; padding: 10px 20px; background-color: #4CAF50; color: white; border: none; border-radius: 5px; cursor: pointer;\"><i class=\"fas fa-paper-plane\"></i> Send</button>\n");
        html.append("    </div>\n");
        html.append("    <style>\n");
        html.append("        .loading-animation {\n");
        html.append("            display: inline-block;\n");
        html.append("            width: 50px;\n");
        html.append("            height: 10px;\n");
        html.append("            position: relative;\n");
        html.append("            background: linear-gradient(to right, #ccc 30%, #eee 50%, #ccc 70%);\n");
        html.append("            background-size: 500% 100%;\n");
        html.append("            animation: loading 1.5s ease-in-out infinite;\n");
        html.append("        }\n");
        html.append(
                "        @keyframes loading { 0% { background-position: 100% 50%; } 100% { background-position: 0% 50%; } }\n");
        html.append("        \n");
        html.append("        /* Markdown formatting styles */\n");
        html.append("        .bot-message pre {\n");
        html.append("            background-color: #f5f5f5;\n");
        html.append("            padding: 10px;\n");
        html.append("            border-radius: 4px;\n");
        html.append("            overflow-x: auto;\n");
        html.append("            margin: 10px 0;\n");
        html.append("            font-family: monospace;\n");
        html.append("            font-size: 14px;\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        .bot-message code {\n");
        html.append("            background-color: #f5f5f5;\n");
        html.append("            padding: 2px 4px;\n");
        html.append("            border-radius: 3px;\n");
        html.append("            font-family: monospace;\n");
        html.append("            font-size: 14px;\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        .bot-message blockquote {\n");
        html.append("            border-left: 4px solid #ddd;\n");
        html.append("            padding-left: 10px;\n");
        html.append("            margin-left: 0;\n");
        html.append("            color: #666;\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        .bot-message ul, .bot-message ol {\n");
        html.append("            padding-left: 20px;\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        .bot-message table {\n");
        html.append("            border-collapse: collapse;\n");
        html.append("            width: 100%;\n");
        html.append("            margin: 10px 0;\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        .bot-message th, .bot-message td {\n");
        html.append("            border: 1px solid #ddd;\n");
        html.append("            padding: 8px;\n");
        html.append("            text-align: left;\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        .bot-message th {\n");
        html.append("            background-color: #f2f2f2;\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        /* Download button styles */\n");
        html.append("        .response-download-btn {\n");
        html.append("            display: inline-block;\n");
        html.append("            padding: 5px 10px;\n");
        html.append("            background-color: #007bff;\n");
        html.append("            color: white;\n");
        html.append("            border: none;\n");
        html.append("            border-radius: 4px;\n");
        html.append("            cursor: pointer;\n");
        html.append("            font-size: 12px;\n");
        html.append("            float: right;\n");
        html.append("            margin-top: 15px; /* Add space above the button */\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        /* Remove absolute positioning from bot-message */\n");
        html.append("        .bot-message {\n");
        html.append("            position: static;\n");
        html.append("            padding-bottom: 15px; /* Normal padding */\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        /* Add a clearfix to contain floated elements */\n");
        html.append("        .bot-message::after {\n");
        html.append("            content: \"\";\n");
        html.append("            display: table;\n");
        html.append("            clear: both;\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        .response-download-btn:hover {\n");
        html.append("            background-color: #0056b3;\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        .response-download-btn:disabled {\n");
        html.append("            background-color: #cccccc;\n");
        html.append("            cursor: not-allowed;\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        .response-actions {\n");
        html.append("            clear: both;\n");
        html.append("            padding-top: 5px;\n");
        html.append("            margin-top: 5px;\n");
        html.append("            border-top: 1px solid #eee;\n");
        html.append("        }\n");
        html.append("    </style>\n");
        html.append("    <div id=\"errorMessage\" style=\"color: red; margin-top: 10px; display: none;\"></div>\n");
        html.append("</div>\n");

        // Add JavaScript for chat functionality
        html.append("<script src=\"https://cdn.jsdelivr.net/npm/marked/marked.min.js\"></script>\n");
        html.append("<script>\n");
        html.append("$(document).ready(function() {\n");
        html.append("    const chatMessages = $('#chatMessages');\n");
        html.append("    const messageInput = $('#messageInput');\n");
        html.append("    const sendButton = $('#sendButton');\n");
        html.append("    const errorMessage = $('#errorMessage');\n");
        html.append("    \n");
        html.append("    // Function to format LLM responses with markdown and handle special characters\n");
        html.append("    function formatLLMResponse(text) {\n");
        html.append("        // First, temporarily replace <br> tags with a placeholder\n");
        html.append("        let processedText = text.replace(/<br\\s*\\/?>/gi, '{{BR_PLACEHOLDER}}');\n");
        html.append("        \n");
        html.append("        // Escape any HTML to prevent XSS\n");
        html.append("        processedText = processedText\n");
        html.append("            .replace(/&/g, '&amp;')\n");
        html.append("            .replace(/</g, '&lt;')\n");
        html.append("            .replace(/>/g, '&gt;');\n");
        html.append("        \n");
        html.append("        // Restore <br> tags from placeholders\n");
        html.append("        processedText = processedText.replace(/{{BR_PLACEHOLDER}}/g, '<br>');\n");
        html.append("        \n");
        html.append("        try {\n");
        html.append("            // Use marked.js to convert markdown to HTML\n");
        html.append("            const formattedText = marked.parse(processedText);\n");
        html.append("            return formattedText;\n");
        html.append("        } catch (e) {\n");
        html.append("            console.error('Error formatting response:', e);\n");
        html.append("            return '<p>' + processedText + '</p>';\n");
        html.append("        }\n");
        html.append("    }\n");
        html.append("    \n");
        html.append("    // Function to add a message to the chat\n");
        html.append("    function addMessage(message, isUser) {\n");
        html.append(
                "        const messageDiv = $('<div>').addClass('message').addClass(isUser ? 'user-message' : 'bot-message').css({\n");
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
        html.append("                'border': '1px solid #e0e0e0',\n");
        html.append("                'padding-bottom': '15px' /* Add extra padding at the bottom */\n");
        html.append("            });\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        if (!isUser) {\n");
        html.append("            // Format LLM responses with markdown\n");
        html.append("            messageDiv.html(formatLLMResponse(message));\n");
        html.append("            \n");
        html.append("            // Add download button if the message contains code\n");
        html.append(
                "            if (message.includes('```') || message.includes('<pre>') || message.includes('<code>')) {\n");
        html.append("                const divider = $('<div>').addClass('download-divider');\n");
        html.append("                messageDiv.append(divider);\n");
        html.append("                \n");
        html.append("                const downloadBtn = $('<button>').addClass('response-download-btn')\n");
        html.append("                    .html('<i class=\"fas fa-download\"></i> Download Code')\n");
        html.append("                    .attr('title', 'Download code from this response');\n");
        html.append("                messageDiv.append(downloadBtn);\n");
        html.append("                \n");
        html.append("                // Add click handler for the download button\n");
        html.append("                downloadBtn.on('click', function() {\n");
        html.append("                    // Disable the button during download\n");
        html.append("                    downloadBtn.prop('disabled', true);\n");
        html.append(
                "                    downloadBtn.html('<i class=\"fas fa-spinner fa-spin\"></i> Preparing...');\n");
        html.append("                    \n");
        html.append("                    try {\n");
        html.append("                        // Get only this response's content\n");
        html.append("                        const responseContent = messageDiv.html();\n");
        html.append("                        \n");
        html.append("                        // Create a form to submit the download request\n");
        html.append("                        const form = $('<form>');\n");
        html.append("                        form.attr('method', 'post');\n");
        html.append(
                "                        form.attr('action', '/jw/web/json/plugin/org.joget.marketplace.LlmChatUserviewMenu/service');\n");
        html.append("                        form.attr('target', '_blank');\n");
        html.append("                        \n");
        html.append("                        // Add the action parameter\n");
        html.append("                        const actionInput = $('<input>');\n");
        html.append("                        actionInput.attr('type', 'hidden');\n");
        html.append("                        actionInput.attr('name', 'action');\n");
        html.append("                        actionInput.attr('value', 'downloadCode');\n");
        html.append("                        form.append(actionInput);\n");
        html.append("                        \n");
        html.append("                        // Add the chat content parameter (only this response)\n");
        html.append("                        const chatContentInput = $('<input>');\n");
        html.append("                        chatContentInput.attr('type', 'hidden');\n");
        html.append("                        chatContentInput.attr('name', 'chatContent');\n");
        html.append("                        chatContentInput.attr('value', responseContent);\n");
        html.append("                        form.append(chatContentInput);\n");
        html.append("                        \n");
        html.append("                        // Add the app ID and version parameters\n");
        html.append("                        const appIdInput = $('<input>');\n");
        html.append("                        appIdInput.attr('type', 'hidden');\n");
        html.append("                        appIdInput.attr('name', 'appId');\n");
        html.append("                        appIdInput.attr('value', '${appId}');\n");
        html.append("                        form.append(appIdInput);\n");
        html.append("                        \n");
        html.append("                        const appVersionInput = $('<input>');\n");
        html.append("                        appVersionInput.attr('type', 'hidden');\n");
        html.append("                        appVersionInput.attr('name', 'appVersion');\n");
        html.append("                        appVersionInput.attr('value', '${appVersion}');\n");
        html.append("                        form.append(appVersionInput);\n");
        html.append("                        \n");
        html.append("                        // Append the form to the body and submit it\n");
        html.append("                        $('body').append(form);\n");
        html.append("                        form.submit();\n");
        html.append("                        form.remove();\n");
        html.append("                        \n");
        html.append("                        // Re-enable the button after a delay\n");
        html.append("                        setTimeout(function() {\n");
        html.append("                            downloadBtn.prop('disabled', false);\n");
        html.append(
                "                            downloadBtn.html('<i class=\"fas fa-download\"></i> Download Code');\n");
        html.append("                        }, 2000);\n");
        html.append("                    } catch (error) {\n");
        html.append("                        console.error('Error downloading code:', error);\n");
        html.append("                        alert('Error downloading code: ' + error.message);\n");
        html.append("                        \n");
        html.append("                        // Re-enable the button\n");
        html.append("                        downloadBtn.prop('disabled', false);\n");
        html.append(
                "                        downloadBtn.html('<i class=\"fas fa-download\"></i> Download Code');\n");
        html.append("                    }\n");
        html.append("                });\n");
        html.append("                // Append button directly to the message div for bottom-right positioning\n");
        html.append("                messageDiv.append(downloadBtn);\n");
        html.append("            }\n");
        html.append("        } else {\n");
        html.append("            // User messages are displayed as plain text\n");
        html.append("            messageDiv.text(message);\n");
        html.append("        }\n");
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
        html.append("        // Disable send button and hide error message\n");
        html.append("        sendButton.prop('disabled', true);\n");
        html.append("        errorMessage.hide();\n");
        html.append("        \n");
        html.append("        // Create a message div for the response with loading animation\n");
        html.append("        const responseDiv = $('<div>').addClass('message').addClass('bot-message').css({\n");
        html.append("            'background-color': '#ffffff',\n");
        html.append("            'margin-right': 'auto',\n");
        html.append("            'margin-left': '10px',\n");
        html.append("            'border': '1px solid #e0e0e0',\n");
        html.append("            'margin-bottom': '15px',\n");
        html.append("            'padding': '10px',\n");
        html.append("            'padding-bottom': '15px', /* Add extra padding at the bottom */\n");
        html.append("            'border-radius': '5px',\n");
        html.append("            'max-width': '80%',\n");
        html.append("            'word-wrap': 'break-word'\n");
        html.append("        });\n");
        html.append("        \n");
        html.append("        // Add loading animation to the response div\n");
        html.append("        const loadingAnimation = $('<div>').addClass('loading-animation');\n");
        html.append("        responseDiv.append(loadingAnimation);\n");
        html.append("        chatMessages.append(responseDiv);\n");
        html.append("        chatMessages.scrollTop(chatMessages[0].scrollHeight);\n");
        html.append("        \n");
        html.append("        // Send message to server\n");
        html.append("        $.ajax({\n");
        html.append("            url: '/jw/web/json/plugin/org.joget.marketplace.LlmChatUserviewMenu/service',\n");
        html.append("            type: 'POST',\n");
        html.append("            data: {\n");
        html.append("                action: 'sendMessage',\n");
        html.append("                message: message,\n");
        html.append("                appId: '${appId}',\n");
        html.append("                appVersion: '${appVersion}'\n");
        html.append("            },\n");
        html.append("            dataType: 'text',\n");
        html.append("            success: function(data) {\n");
        html.append("                // Add bot response to chat\n");
        html.append("                console.log('Raw success response:', data);\n");
        html.append("                try {\n");
        html.append("                    // Try to parse as JSON\n");
        html.append("                    const jsonData = JSON.parse(data);\n");
        html.append("                    // Replace loading animation with formatted response text\n");
        html.append("                    responseDiv.empty().html(formatLLMResponse(jsonData.response));\n");
        html.append("                    \n");
        html.append("                    // Add download button if the response contains code\n");
        html.append("                    const responseText = jsonData.response || '';\n");
        html.append(
                "                    if (responseText.includes('```') || responseText.includes('<pre>') || responseText.includes('<code>')) {\n");
        html.append("                        const actionsDiv = $('<div>').addClass('response-actions');\n");
        html.append("                        const downloadBtn = $('<button>').addClass('response-download-btn')\n");
        html.append("                            .html('<i class=\"fas fa-download\"></i> Download Code')\n");
        html.append("                            .attr('title', 'Download code from this response');\n");
        html.append("                        \n");
        html.append("                        // Add click handler for the download button\n");
        html.append("                        downloadBtn.on('click', function() {\n");
        html.append("                            // Disable the button during download\n");
        html.append("                            downloadBtn.prop('disabled', true);\n");
        html.append(
                "                            downloadBtn.html('<i class=\"fas fa-spinner fa-spin\"></i> Preparing...');\n");
        html.append("                            \n");
        html.append("                            try {\n");
        html.append("                                // Get only this response's content\n");
        html.append("                                const responseContent = responseDiv.html();\n");
        html.append("                                \n");
        html.append("                                // Create a form to submit the download request\n");
        html.append("                                const form = $('<form>');\n");
        html.append("                                form.attr('method', 'post');\n");
        html.append(
                "                                form.attr('action', '/jw/web/json/plugin/org.joget.marketplace.LlmChatUserviewMenu/service');\n");
        html.append("                                form.attr('target', '_blank');\n");
        html.append("                                \n");
        html.append("                                // Add the action parameter\n");
        html.append("                                const actionInput = $('<input>');\n");
        html.append("                                actionInput.attr('type', 'hidden');\n");
        html.append("                                actionInput.attr('name', 'action');\n");
        html.append("                                actionInput.attr('value', 'downloadCode');\n");
        html.append("                                form.append(actionInput);\n");
        html.append("                                \n");
        html.append("                                // Add the chat content parameter (only this response)\n");
        html.append("                                const chatContentInput = $('<input>');\n");
        html.append("                                chatContentInput.attr('type', 'hidden');\n");
        html.append("                                chatContentInput.attr('name', 'chatContent');\n");
        html.append("                                chatContentInput.attr('value', responseContent);\n");
        html.append("                                form.append(chatContentInput);\n");
        html.append("                                \n");
        html.append("                                // Add the app ID and version parameters\n");
        html.append("                                const appIdInput = $('<input>');\n");
        html.append("                                appIdInput.attr('type', 'hidden');\n");
        html.append("                                appIdInput.attr('name', 'appId');\n");
        html.append("                                appIdInput.attr('value', '${appId}');\n");
        html.append("                                form.append(appIdInput);\n");
        html.append("                                \n");
        html.append("                                const appVersionInput = $('<input>');\n");
        html.append("                                appVersionInput.attr('type', 'hidden');\n");
        html.append("                                appVersionInput.attr('name', 'appVersion');\n");
        html.append("                                appVersionInput.attr('value', '${appVersion}');\n");
        html.append("                                form.append(appVersionInput);\n");
        html.append("                                \n");
        html.append("                                // Append the form to the body and submit it\n");
        html.append("                                $('body').append(form);\n");
        html.append("                                form.submit();\n");
        html.append("                                form.remove();\n");
        html.append("                                \n");
        html.append("                                // Re-enable the button after a delay\n");
        html.append("                                setTimeout(function() {\n");
        html.append("                                    downloadBtn.prop('disabled', false);\n");
        html.append(
                "                                    downloadBtn.html('<i class=\"fas fa-download\"></i> Download Code');\n");
        html.append("                                }, 2000);\n");
        html.append("                            } catch (error) {\n");
        html.append("                                console.error('Error downloading code:', error);\n");
        html.append("                                alert('Error downloading code: ' + error.message);\n");
        html.append("                                \n");
        html.append("                                // Re-enable the button\n");
        html.append("                                downloadBtn.prop('disabled', false);\n");
        html.append(
                "                                downloadBtn.html('<i class=\"fas fa-download\"></i> Download Code');\n");
        html.append("                            }\n");
        html.append("                        });\n");
        html.append("                        actionsDiv.append(downloadBtn);\n");
        html.append("                        responseDiv.append(actionsDiv);\n");
        html.append("                    }\n");
        html.append("                } catch (e) {\n");
        html.append("                    console.error('Error parsing response:', e);\n");
        html.append("                    // Just use the raw data but still try to format it\n");
        html.append("                    responseDiv.empty().html(formatLLMResponse(data));\n");
        html.append("                    \n");
        html.append("                    // Check for code in raw data\n");
        html.append(
                "                    if (data.includes('```') || data.includes('<pre>') || data.includes('<code>')) {\n");
        html.append("                        const actionsDiv = $('<div>').addClass('response-actions');\n");
        html.append("                        const downloadBtn = $('<button>').addClass('response-download-btn')\n");
        html.append("                            .html('<i class=\"fas fa-download\"></i> Download Code')\n");
        html.append("                            .attr('title', 'Download code from this response');\n");
        html.append("                        \n");
        html.append("                        // Add click handler for the download button (same as above)\n");
        html.append("                        downloadBtn.on('click', function() {\n");
        html.append("                            // Disable the button during download\n");
        html.append("                            downloadBtn.prop('disabled', true);\n");
        html.append(
                "                            downloadBtn.html('<i class=\"fas fa-spinner fa-spin\"></i> Preparing...');\n");
        html.append("                            \n");
        html.append("                            try {\n");
        html.append("                                // Get only this response's content\n");
        html.append("                                const responseContent = responseDiv.html();\n");
        html.append("                                \n");
        html.append("                                // Create a form to submit the download request\n");
        html.append("                                const form = $('<form>');\n");
        html.append("                                form.attr('method', 'post');\n");
        html.append(
                "                                form.attr('action', '/jw/web/json/plugin/org.joget.marketplace.LlmChatUserviewMenu/service');\n");
        html.append("                                form.attr('target', '_blank');\n");
        html.append("                                \n");
        html.append("                                // Add the action parameter\n");
        html.append("                                const actionInput = $('<input>');\n");
        html.append("                                actionInput.attr('type', 'hidden');\n");
        html.append("                                actionInput.attr('name', 'action');\n");
        html.append("                                actionInput.attr('value', 'downloadCode');\n");
        html.append("                                form.append(actionInput);\n");
        html.append("                                \n");
        html.append("                                // Add the chat content parameter (only this response)\n");
        html.append("                                const chatContentInput = $('<input>');\n");
        html.append("                                chatContentInput.attr('type', 'hidden');\n");
        html.append("                                chatContentInput.attr('name', 'chatContent');\n");
        html.append("                                chatContentInput.attr('value', responseContent);\n");
        html.append("                                form.append(chatContentInput);\n");
        html.append("                                \n");
        html.append("                                // Add the app ID and version parameters\n");
        html.append("                                const appIdInput = $('<input>');\n");
        html.append("                                appIdInput.attr('type', 'hidden');\n");
        html.append("                                appIdInput.attr('name', 'appId');\n");
        html.append("                                appIdInput.attr('value', '${appId}');\n");
        html.append("                                form.append(appIdInput);\n");
        html.append("                                \n");
        html.append("                                const appVersionInput = $('<input>');\n");
        html.append("                                appVersionInput.attr('type', 'hidden');\n");
        html.append("                                appVersionInput.attr('name', 'appVersion');\n");
        html.append("                                appVersionInput.attr('value', '${appVersion}');\n");
        html.append("                                form.append(appVersionInput);\n");
        html.append("                                \n");
        html.append("                                // Append the form to the body and submit it\n");
        html.append("                                $('body').append(form);\n");
        html.append("                                form.submit();\n");
        html.append("                                form.remove();\n");
        html.append("                                \n");
        html.append("                                // Re-enable the button after a delay\n");
        html.append("                                setTimeout(function() {\n");
        html.append("                                    downloadBtn.prop('disabled', false);\n");
        html.append(
                "                                    downloadBtn.html('<i class=\"fas fa-download\"></i> Download Code');\n");
        html.append("                                }, 2000);\n");
        html.append("                            } catch (error) {\n");
        html.append("                                console.error('Error downloading code:', error);\n");
        html.append("                                alert('Error downloading code: ' + error.message);\n");
        html.append("                                \n");
        html.append("                                // Re-enable the button\n");
        html.append("                                downloadBtn.prop('disabled', false);\n");
        html.append(
                "                                downloadBtn.html('<i class=\"fas fa-download\"></i> Download Code');\n");
        html.append("                            }\n");
        html.append("                        });\n");
        html.append("                        actionsDiv.append(downloadBtn);\n");
        html.append("                        responseDiv.append(actionsDiv);\n");
        html.append("                    }\n");
        html.append("                }\n");
        html.append("                chatMessages.scrollTop(chatMessages[0].scrollHeight);\n");
        html.append("            },\n");
        html.append("            error: function(xhr, status, error) {\n");
        html.append("                // Show error message\n");
        html.append("                let errorText = 'Error communicating with the LLM API';\n");
        html.append("                try {\n");
        html.append("                    console.log('  or response:', xhr.responseText);\n");
        html.append("                    console.log('Status code:', xhr.status);\n");
        html.append("                    console.log('Error:', error);\n");
        html.append("                    \n");
        html.append("                    const debugDiv = $('<div>').css({\n");
        html.append("                        'background-color': '#ffeeee',\n");
        html.append("                        'border': '1px solid #ff0000',\n");
        html.append("                        'padding': '10px',\n");
        html.append("                        'margin-top': '10px',\n");
        html.append("                        'white-space': 'pre-wrap',\n");
        html.append("                        'font-family': 'monospace',\n");
        html.append("                        'font-size': '12px'\n");
        html.append("                    });\n");
        html.append("                    debugDiv.text('Raw Response:\\n' + xhr.responseText);\n");
        html.append("                    errorMessage.after(debugDiv);\n");
        html.append("                    \n");
        html.append("                    try {\n");
        html.append("                        const response = JSON.parse(xhr.responseText);\n");
        html.append("                        if (response.error) {\n");
        html.append("                            errorText = response.error;\n");
        html.append("                        }\n");
        html.append("                    } catch (e) {\n");
        html.append("                        console.error('Error parsing error response:', e);\n");
        html.append("                        if (xhr.responseText && xhr.responseText.length < 200) {\n");
        html.append("                            errorText = xhr.responseText;\n");
        html.append("                        }\n");
        html.append("                    }\n");
        html.append("                } catch (e) {\n");
        html.append("                    console.error('Error parsing error response:', e);\n");
        html.append("                }\n");
        html.append("                errorMessage.text(errorText).show();\n");
        html.append("                \n");
        html.append("                // Replace loading animation with error message\n");
        html.append(
                "                responseDiv.empty().text('Error: Could not get response from Ollama. Please try again.');\n");
        html.append("            },\n");
        html.append("            complete: function() {\n");
        html.append("                // Re-enable send button\n");
        html.append("                sendButton.prop('disabled', false);\n");
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

        // Add CSS to adjust chat message width
        html.append("<style>\n");
        html.append("    .message {\n");
        html.append("        margin-bottom: 15px;\n");
        html.append("        padding: 10px;\n");
        html.append("        border-radius: 5px;\n");
        html.append("        max-width: 90%;\n");
        html.append("        width: fit-content;\n");
        html.append("        word-wrap: break-word;\n");
        html.append("    }\n");
        html.append("    \n");
        html.append("    .user-message {\n");
        html.append("        background-color: #dcf8c6;\n");
        html.append("        margin-left: auto;\n");
        html.append("        margin-right: 10px;\n");
        html.append("    }\n");
        html.append("    \n");
        html.append("    .bot-message {\n");
        html.append("        background-color: #ffffff;\n");
        html.append("        margin-right: auto;\n");
        html.append("        margin-left: 10px;\n");
        html.append("        border: 1px solid #e0e0e0;\n");
        html.append("    }\n");
        html.append("</style>\n");

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

        Object[] arguments = new Object[] { appId, appVersion };

        String json = AppUtil.readPluginResource(getClass().getName(), "/properties/llmChatUserviewMenu.json",
                arguments, true, "messages/llmChatUserviewMenu");
        return json;
    }

    @Override
    public String getDecoratedMenu() {
        // Ensure URL is properly formatted
        String url = getUrl();
        // Make sure there are no & characters in the URL that should be ? instead
        if (url.contains("&") && !url.contains("?")) {
            url = url.replace("&", "?");
        }
        return "<a href=\"" + url
                + "\" class=\"menu-link\"><span class=\"menu-icon\"><i class=\"fas fa-comments\"></i></span><span class=\"menu-label\">"
                + getPropertyString("label") + "</span></a>";
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
    public void webService(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");

        // Log the request parameters for debugging
        LogUtil.info(getClassName(), "webService called with action: " + action);
        LogUtil.info(getClassName(), "Request URL: " + request.getRequestURL()
                + (request.getQueryString() != null ? "?" + request.getQueryString() : ""));
        LogUtil.info(getClassName(), "Content-Type: " + request.getContentType());
        LogUtil.info(getClassName(), "Plugin Name: " + getClass().getName());

        // Log all request parameters
        java.util.Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            LogUtil.info(getClassName(), "Parameter: " + paramName + " = " + paramValue);
        }

        if ("checkConnection".equals(action)) {
            // Check if Ollama is accessible
            response.setContentType("application/json;charset=UTF-8");
            String apiEndpoint = getPropertyString("apiEndpoint");
            if (apiEndpoint == null || apiEndpoint.trim().isEmpty()) {
                apiEndpoint = "http://localhost:11434/api/generate";
            }

            try {
                // Try to connect to the Ollama API
                URL url = new URL(apiEndpoint.replace("/generate", "/tags"));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000); // 5 seconds timeout

                // Always return a simple JSON response
                response.getWriter().write("{\"status\":\"ok\"}");
            } catch (Exception e) {
                LogUtil.error(getClassName(), e, "Error checking Ollama connection: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                // Always return a simple JSON error response
                response.getWriter().write("{\"error\":\"Cannot connect to Ollama server\"}");
            }
        } else if ("downloadCode".equals(action)) {
            try {
                LogUtil.info(getClassName(), "=== DOWNLOAD CODE ACTION STARTED ===");

                // Get chat content from request
                String chatContent = request.getParameter("chatContent");

                // Log chat content size
                LogUtil.info(getClassName(), "Chat content size: " +
                        (chatContent != null ? chatContent.length() : "null") + " characters");

                if (chatContent == null || chatContent.trim().isEmpty()) {
                    LogUtil.info(getClassName(), "Chat content is empty or null");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    sendJsonResponse(response, "error", "Chat content is required");
                    return;
                }

                // Log a sample of the chat content
                LogUtil.info(getClassName(), "Chat content sample (first 500 chars): " +
                        (chatContent.length() > 500 ? chatContent.substring(0, 500) + "..." : chatContent));

                // Create zip file from chat content
                LogUtil.info(getClassName(), "Calling ZipFileUtil.createZipFromChatContent");
                byte[] zipData = ZipFileUtil.createZipFromChatContent(chatContent);

                // Log zip data size
                LogUtil.info(getClassName(),
                        "Zip data size: " + (zipData != null ? zipData.length : "null") + " bytes");

                if (zipData == null || zipData.length == 0) {
                    LogUtil.info(getClassName(), "Generated zip data is empty");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                    // Instead of JSON response, send HTML with debug info for easier diagnosis
                    response.setContentType("text/html;charset=UTF-8");
                    PrintWriter writer = response.getWriter();
                    writer.write("<html><head><title>Debug Info</title></head><body>");
                    writer.write("<h1>Debug Information</h1>");
                    writer.write("<p>Failed to generate zip file: No code snippets found</p>");
                    writer.write("<h2>Chat Content Sample</h2>");
                    writer.write("<pre>" +
                            StringUtil.escapeString(
                                    chatContent.length() > 1000 ? chatContent.substring(0, 1000) + "..." : chatContent,
                                    StringUtil.TYPE_HTML, null)
                            +
                            "</pre>");
                    writer.write("<h2>Patterns Used</h2>");
                    writer.write("<ul>");
                    writer.write("<li>&lt;pre&gt;&lt;code class=\"language-xxx\"&gt;...&lt;/code&gt;&lt;/pre&gt;</li>");
                    writer.write("<li>&lt;pre&gt;&lt;code&gt;...&lt;/code&gt;&lt;/pre&gt;</li>");
                    writer.write("<li>&lt;div class=\"code-block-container\"&gt;&lt;pre&gt;...&lt;/pre&gt;</li>");
                    writer.write("<li>&lt;pre&gt;...&lt;/pre&gt;</li>");
                    writer.write("<li>&lt;pre&gt;&lt;code [any attributes]&gt;...&lt;/code&gt;&lt;/pre&gt;</li>");
                    writer.write("<li>```language\ncode\n```</li>");
                    writer.write("</ul>");
                    writer.write("</body></html>");
                    writer.flush();
                    return;
                }

                // Set response headers for file download
                response.setContentType("application/zip");
                response.setHeader("Content-Disposition", "attachment; filename=code_snippets.zip");
                response.setContentLength(zipData.length);

                LogUtil.info(getClassName(), "Writing zip data to response");

                // Write zip data to response
                ServletOutputStream sos = response.getOutputStream();
                sos.write(zipData);
                sos.flush();
                sos.close();

                LogUtil.info(getClassName(), "=== DOWNLOAD CODE ACTION COMPLETED SUCCESSFULLY ===");

            } catch (Exception e) {
                LogUtil.error(getClassName(), e, "Error creating zip file: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                // Send error response
                sendJsonResponse(response, "error", "Error creating zip file: " + e.getMessage());
            }
        } else if ("sendMessage".equals(action)) {
            try {
                // Get message from request
                String message = request.getParameter("message");
                String apiEndpoint = getPropertyString("apiEndpoint");
                String model = getPropertyString("model");
                String systemPrompt = getPropertyString("systemPrompt");

                // Get temperature with default value
                double temperature = 0.7;

                try {
                    String tempStr = getPropertyString("temperature");
                    if (tempStr != null && !tempStr.isEmpty()) {
                        temperature = Double.parseDouble(tempStr);
                    }
                } catch (NumberFormatException e) {
                    LogUtil.warn(getClassName(), "Invalid temperature value, using default 0.7");
                }

                // Call Ollama API
                String result;
                try {
                    result = OllamaApiClient.callOllamaApi(
                            message,
                            apiEndpoint,
                            model,
                            systemPrompt,
                            temperature);

                    // Log the response for debugging
                    LogUtil.info(getClassName(), "Sending response to client: " + result);

                    // Return a proper JSON response
                    response.setContentType("application/json;charset=UTF-8");
                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put("response", result);
                    response.getWriter().write(jsonResponse.toString());
                } catch (Exception e) {
                    LogUtil.error(getClassName(), e, "Error calling Ollama API: " + e.getMessage());
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                    // Send a proper JSON error response
                    response.setContentType("application/json;charset=UTF-8");
                    String errorMessage = e.getMessage() != null ? e.getMessage()
                            : "Error connecting to Ollama API";
                    JSONObject jsonError = new JSONObject();
                    jsonError.put("error", errorMessage);
                    response.getWriter().write(jsonError.toString());
                }

            } catch (Exception e) {
                LogUtil.error(getClassName(), e, "Error in webService: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                // Use the utility method to ensure valid JSON
                String errorMessage = e.getMessage();
                if (errorMessage == null || errorMessage.trim().isEmpty()) {
                    errorMessage = "Unknown error occurred";
                }

                // Log the response we're sending for debugging
                LogUtil.debug(getClassName(), "Sending error response: " + errorMessage);

                // Send the error response
                sendJsonResponse(response, "error", errorMessage);
            }
        }
    }
}