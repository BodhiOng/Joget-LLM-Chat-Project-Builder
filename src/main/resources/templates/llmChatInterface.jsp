<%@ page import="org.joget.apps.app.service.AppUtil"%>
<%@ page import="org.joget.commons.util.StringUtil"%>
<%@ include file="/WEB-INF/jsp/includes/taglibs.jsp" %>

<c:set var="appId" value="${appId}"/>
<c:set var="appVersion" value="${appVersion}"/>
<c:set var="key" value="${key}"/>

<%
    String contextPath = request.getContextPath();
    String customCss = StringUtil.escapeString(request.getParameter("customCss"), StringUtil.TYPE_HTML, null);
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>LLM Chat Interface</title>
    
    <!-- Include Joget's CSS and JS -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/v7.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/js/fontawesome5/css/all.min.css">
    <script src="${pageContext.request.contextPath}/js/jquery/jquery-3.5.1.min.js"></script>
    <script src="${pageContext.request.contextPath}/js/jquery/jquery-migrate-3.0.1.min.js"></script>
    
    <style>
        .chat-container {
            max-width: 800px;
            margin: 0 auto;
            padding: 20px;
            font-family: Arial, sans-serif;
        }
        
        .chat-messages {
            height: 500px;
            overflow-y: auto;
            border: 1px solid #ddd;
            border-radius: 5px;
            padding: 10px;
            margin-bottom: 15px;
            background-color: #f9f9f9;
        }
        
        .message {
            margin-bottom: 15px;
            padding: 10px;
            border-radius: 5px;
            max-width: 80%;
            word-wrap: break-word;
        }
        
        .user-message {
            background-color: #dcf8c6;
            margin-left: auto;
            margin-right: 10px;
        }
        
        .bot-message {
            background-color: #ffffff;
            margin-right: auto;
            margin-left: 10px;
            border: 1px solid #e0e0e0;
        }
        
        .message-input {
            display: flex;
            margin-top: 10px;
        }
        
        .message-input textarea {
            flex-grow: 1;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 5px;
            resize: none;
            height: 60px;
        }
        
        .message-input button {
            margin-left: 10px;
            padding: 10px 20px;
            background-color: #4CAF50;
            color: white;
            border: none;
            border-radius: 5px;
            cursor: pointer;
        }
        
        .message-input button:hover {
            background-color: #45a049;
        }
        
        .message-input button:disabled {
            background-color: #cccccc;
            cursor: not-allowed;
        }
        
        .typing-indicator {
            display: none;
            margin-left: 10px;
            padding: 10px;
            border-radius: 5px;
            background-color: #f0f0f0;
            width: 70px;
            text-align: center;
        }
        
        .loading-animation {
            display: inline-block;
            width: 50px;
            height: 10px;
            position: relative;
            background: linear-gradient(to right, #ccc 30%, #eee 50%, #ccc 70%);
            background-size: 500% 100%;
            animation: loading 1.5s ease-in-out infinite;
        }
        
        @keyframes loading {
            0% { background-position: 100% 50%; }
            100% { background-position: 0% 50%; }
        }
        
        .error-message {
            color: red;
            margin-top: 10px;
            display: none;
        }
        
        /* Custom CSS */
        <%= customCss %>
    </style>
</head>
<body>
    <div class="chat-container">
        <h2><i class="fas fa-comments"></i> LLM Chat</h2>
        
        <div class="chat-messages" id="chatMessages">
            <div class="message bot-message" style="background-color: #ffffff; margin-right: auto; margin-left: 10px; border: 1px solid #e0e0e0;">
                Hello! How can I assist you today?
            </div>
        </div>
        
        <div class="message-input">
            <textarea id="messageInput" placeholder="Type your message here..."></textarea>
            <button id="sendButton"><i class="fas fa-paper-plane"></i> Send</button>
        </div>
        
        <!-- Typing indicator moved to chat window in the sendMessage function -->
        <div id="errorMessage" style="color: red; margin-top: 10px; display: none;"></div>
        <div id="debugInfo" style="margin-top: 20px; padding: 10px; border: 1px solid #ddd; background-color: #f9f9f9;">
            <h3>Debug Information</h3>
            <p><strong>Service URL:</strong> <span id="serviceUrlDisplay"></span></p>
            <p><strong>Plugin Name:</strong> org.joget.marketplace.LlmChatUserviewMenu</p>
            <p><strong>App ID:</strong> ${appId}</p>
            <p><strong>App Version:</strong> ${appVersion}</p>
            <p><strong>Test Connection:</strong> <a href="javascript:void(0)" id="testConnectionLink">Click to Test Connection</a></p>
            <p><strong>Test Response:</strong> <span id="testResponse"></span></p>
            
            <h4>Direct Form Test</h4>
            <form id="directTestForm" action="/jw/web/json/plugin/org.joget.marketplace.LlmChatUserviewMenu/service" method="post" target="_blank">
                <input type="hidden" name="action" value="checkConnection">
                <input type="hidden" name="appId" value="${appId}">
                <input type="hidden" name="appVersion" value="${appVersion}">
                <button type="submit">Test in New Window</button>
            </form>
            
            <h4>Direct URL Test</h4>
            <p>Check Connection: <a href="/jw/web/json/plugin/org.joget.marketplace.LlmChatUserviewMenu/service?action=checkConnection&appId=${appId}&appVersion=${appVersion}" target="_blank">Test Connection</a></p>
            
            <h4>Send Message Test</h4>
            <form id="sendMessageTestForm" action="/jw/web/json/plugin/org.joget.marketplace.LlmChatUserviewMenu/service" method="post" target="_blank">
                <input type="hidden" name="action" value="sendMessage">
                <input type="hidden" name="appId" value="${appId}">
                <input type="hidden" name="appVersion" value="${appVersion}">
                <input type="text" name="message" value="Hello" style="width: 200px;">
                <button type="submit">Send Test Message</button>
            </form>
            
            <h4>Plugin Class Name Check</h4>
            <p>Full Class Name: <%= getClass().getName() %></p>
        </div>
    </div>
    
    <script>
        $(document).ready(function() {
            const chatMessages = $('#chatMessages');
            const messageInput = $('#messageInput');
            const sendButton = $('#sendButton');
            const typingIndicator = $('#typingIndicator');
            const errorMessage = $('#errorMessage');
            
            // Add a debug div
            const debugContainer = $('<div>').attr('id', 'debugContainer').css({
                'margin-top': '20px',
                'padding': '10px',
                'border': '1px solid #ccc',
                'background-color': '#f9f9f9',
                'display': 'none'
            });
            
            const debugToggle = $('<button>').text('Toggle Debug Info').css({
                'margin-top': '10px',
                'padding': '5px 10px',
                'background-color': '#ddd',
                'border': '1px solid #aaa',
                'border-radius': '3px',
                'cursor': 'pointer'
            }).click(function() {
                debugContainer.toggle();
            });
            
            $('body').append(debugToggle).append(debugContainer);
            
            // Function to log debug info
            function logDebug(title, content) {
                const entry = $('<div>').css({
                    'margin-bottom': '10px',
                    'padding': '5px',
                    'border-bottom': '1px solid #ddd'
                });
                
                entry.append($('<h4>').text(title).css('margin', '0 0 5px 0'));
                
                if (typeof content === 'object') {
                    try {
                        content = JSON.stringify(content, null, 2);
                    } catch (e) {
                        content = String(content);
                    }
                }
                
                entry.append($('<pre>').text(content).css({
                    'margin': '0',
                    'padding': '5px',
                    'background-color': '#eee',
                    'overflow-x': 'auto',
                    'white-space': 'pre-wrap',
                    'font-family': 'monospace',
                    'font-size': '12px'
                }));
                
                debugContainer.prepend(entry);
            }
            
            // Get the correct service URL with explicit plugin class name and hardcoded context path
            const serviceUrl = '/jw/web/json/plugin/org.joget.marketplace.LlmChatUserviewMenu/service';
            logDebug('Service URL', serviceUrl);
            
            // Display the service URL in the debug info
            $('#serviceUrlDisplay').text(serviceUrl);
            
            // Handle test connection link click
            $('#testConnectionLink').click(function() {
                $('#testResponse').text('Testing connection...');
                
                // Make a direct AJAX call to test the connection
                $.ajax({
                    url: serviceUrl,
                    type: 'POST',
                    data: {
                        action: 'checkConnection',
                        appId: '${appId}',
                        appVersion: '${appVersion}'
                    },
                    complete: function(xhr, status) {
                        $('#testResponse').html('<pre style="margin: 0; padding: 5px; background-color: #eee; max-height: 200px; overflow: auto;">' + 
                            'Status: ' + status + '<br>' +
                            'Response Code: ' + xhr.status + '<br>' +
                            'Response Text: ' + xhr.responseText.replace(/</g, '&lt;').replace(/>/g, '&gt;') + '</pre>');
                    }
                });
            });
            
            // Function to add a message to the chat
            function addMessage(message, isUser) {
                const messageDiv = $('<div>').addClass('message').addClass(isUser ? 'user-message' : 'bot-message');
                
                if (!isUser) {
                    messageDiv.css({
                        'background-color': '#ffffff',
                        'margin-right': 'auto',
                        'margin-left': '10px',
                        'border': '1px solid #e0e0e0'
                    });
                }
                
                messageDiv.text(message);
                chatMessages.append(messageDiv);
                chatMessages.scrollTop(chatMessages[0].scrollHeight);
            }
            
            // Function to check if Ollama is accessible
            function checkOllamaConnection(callback) {
                $.ajax({
                    url: serviceUrl,
                    type: 'POST',
                    data: {
                        action: 'checkConnection',
                        appId: '${appId}',
                        appVersion: '${appVersion}'
                    },
                    dataType: 'text', // Explicitly use text to avoid JSON parsing
                    timeout: 10000, // 10 second timeout
                    success: function(data) {
                        // Extremely simplified success handling
                        logDebug('Connection Check Response', data);
                        
                        // If we got any response, consider it a success
                        callback(true);
                    },
                    error: function(xhr, status, error) {
                        logDebug('Connection Check Error Status', status);
                        logDebug('Connection Check Error Message', error);
                        logDebug('Connection Check Raw Response', xhr.responseText);
                        
                        errorMessage.text('Unable to connect to Ollama server. Please make sure it is running.').show();
                        callback(false);
                    }
                });
            }
            
            // Function to send a message to the LLM API
            function sendMessage() {
                const message = messageInput.val().trim();
                if (!message) return;
                
                // Hide any previous error messages
                errorMessage.hide();
                
                // Add user message to chat
                addMessage(message, true);
                
                // Clear input
                messageInput.val('');
                
                // Disable send button and hide error message
                sendButton.prop('disabled', true);
                errorMessage.hide();
                
                // Create a message div for the response with loading animation
                const responseDiv = $('<div>').addClass('message').addClass('bot-message').css({
                    'background-color': '#ffffff',
                    'margin-right': 'auto',
                    'margin-left': '10px',
                    'border': '1px solid #e0e0e0'
                });
                
                // Add loading animation to the response div
                const loadingAnimation = $('<div>').addClass('loading-animation');
                responseDiv.append(loadingAnimation);
                chatMessages.append(responseDiv);
                chatMessages.scrollTop(chatMessages[0].scrollHeight);
                
                // Check if Ollama is accessible first
                checkOllamaConnection(function(isConnected) {
                    if (!isConnected) {
                        sendButton.prop('disabled', false);
                        responseDiv.text('Error: Cannot connect to Ollama server');
                        return;
                    }
                    
                    // Check if streaming is enabled
                    const useStreaming = ${param.useStreaming || false};
                    
                    if (useStreaming) {
                        // Set up server-sent events for streaming with a properly formatted URL
                        const streamUrl = '/jw/web/json/plugin/org.joget.marketplace.LlmChatUserviewMenu/service' +
                            '?action=streamMessage&message=' + encodeURIComponent(message) +
                            '&appId=${appId}&appVersion=${appVersion}';
                            
                        console.log('Stream URL:', streamUrl);
                        const eventSource = new EventSource(streamUrl);
                        
                        let fullResponse = '';
                        
                        // Handle incoming message chunks with simplified parsing
                        eventSource.onmessage = function(event) {
                            console.log('Stream event received:', event.data);
                            
                            try {
                                // Try to parse as JSON
                                const data = JSON.parse(event.data);
                                
                                // Handle chunk data
                                if (data.chunk) {
                                    fullResponse += data.chunk;
                                    // Replace loading animation with text
                                    responseDiv.empty().text(fullResponse);
                                    chatMessages.scrollTop(chatMessages[0].scrollHeight);
                                }
                                
                                // Check if this is the final message
                                if (data.done) {
                                    eventSource.close();
                                    sendButton.prop('disabled', false);
                                }
                            } catch (e) {
                                // If parsing fails, just append the raw data
                                console.error('Error parsing streaming response:', e);
                                fullResponse += event.data;
                                responseDiv.text(fullResponse);
                                chatMessages.scrollTop(chatMessages[0].scrollHeight);
                            }
                        };
                        
                        // Handle errors
                        eventSource.onerror = function() {
                            eventSource.close();
                            if (fullResponse === '') {
                                responseDiv.text('Error: Failed to get response from Ollama');
                            }
                            errorMessage.text('Error connecting to Ollama streaming API').show();
                            sendButton.prop('disabled', false);
                        };
                    } else {
                        // Use regular AJAX for non-streaming responses with explicit text dataType
                        $.ajax({
                            url: serviceUrl,
                            type: 'POST',
                            data: {
                                action: 'sendMessage',
                                message: message,
                                appId: '${appId}',
                                appVersion: '${appVersion}'
                            },
                            dataType: 'text', // Explicitly use text to avoid JSON parsing
                            success: function(data) {
                                // Add bot response to chat with extremely simplified handling
                                logDebug('Raw Success Response', data);
                                
                                // Try to extract the actual response from the text
                                let responseText = data;
                                
                                try {
                                    // Look for JSON-like content in the response
                                    if (data.includes('"response"')) {
                                        const match = data.match(/"response"\s*:\s*"([^"]*)"/i);
                                        if (match && match[1]) {
                                            responseText = match[1];
                                        }
                                    }
                                }
                                
                                // Check if this is the final message
                                if (data.done) {
                                    eventSource.close();
                                    sendButton.prop('disabled', false);
                                logDebug('Raw Error Response', xhr.responseText);
                                logDebug('Response Headers', xhr.getAllResponseHeaders());
                                
                                // Also log the full XHR object
                                logDebug('Full XHR Object', {
                                    status: xhr.status,
                                    statusText: xhr.statusText,
                                    responseType: xhr.responseType,
                                    responseURL: xhr.responseURL,
                                    readyState: xhr.readyState
                                });
                                
                                // Display a simple error message
                                let errorText = 'Error communicating with the Ollama API';
                                if (xhr.status) {
                                    errorText += ' (Status: ' + xhr.status + ')';
                                }
                                
                                // Display the error message
                                errorMessage.text(errorText).show();
                                
                                // Show a simple error in the chat
                                responseDiv.text('Error: Could not get response from Ollama. Please try again.');
                            },
                            complete: function() {
                                // Re-enable send button
                                sendButton.prop('disabled', false);
                            }
                        });
                    }
                });
            }
            
            // Send message when button is clicked
            sendButton.click(sendMessage);
            
            // Send message when Enter key is pressed (but allow Shift+Enter for new lines)
            messageInput.keydown(function(event) {
                if (event.keyCode === 13 && !event.shiftKey) {
                    event.preventDefault();
                    sendMessage();
                }
            });
        });
    </script>
</body>
</html>
