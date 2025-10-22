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
            font-style: italic;
            color: #666;
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
            <div class="message bot-message">
                Hello! How can I assist you today?
            </div>
        </div>
        
        <div class="message-input">
            <textarea id="messageInput" placeholder="Type your message here..."></textarea>
            <button id="sendButton"><i class="fas fa-paper-plane"></i> Send</button>
        </div>
        
        <div class="typing-indicator" id="typingIndicator">
            AI is thinking...
        </div>
        
        <div class="error-message" id="errorMessage"></div>
    </div>
    
    <script>
        $(document).ready(function() {
            const chatMessages = $('#chatMessages');
            const messageInput = $('#messageInput');
            const sendButton = $('#sendButton');
            const typingIndicator = $('#typingIndicator');
            const errorMessage = $('#errorMessage');
            
            // Function to add a message to the chat
            function addMessage(message, isUser) {
                const messageDiv = $('<div>').addClass('message').addClass(isUser ? 'user-message' : 'bot-message');
                messageDiv.text(message);
                chatMessages.append(messageDiv);
                chatMessages.scrollTop(chatMessages[0].scrollHeight);
            }
            
            // Function to send a message to the LLM API
            function sendMessage() {
                const message = messageInput.val().trim();
                if (!message) return;
                
                // Add user message to chat
                addMessage(message, true);
                
                // Clear input
                messageInput.val('');
                
                // Disable send button and show typing indicator
                sendButton.prop('disabled', true);
                typingIndicator.show();
                errorMessage.hide();
                
                // Send message to server
                $.ajax({
                    url: '${pageContext.request.contextPath}/web/json/plugin/${param.pluginName}/service',
                    type: 'POST',
                    data: {
                        action: 'sendMessage',
                        message: message,
                        appId: '${appId}',
                        appVersion: '${appVersion}'
                    },
                    dataType: 'json',
                    success: function(data) {
                        // Add bot response to chat
                        addMessage(data.response, false);
                    },
                    error: function(xhr, status, error) {
                        // Show error message
                        let errorText = 'Error communicating with the LLM API';
                        try {
                            const response = JSON.parse(xhr.responseText);
                            if (response.error) {
                                errorText = response.error;
                            }
                        } catch (e) {
                            console.error('Error parsing error response:', e);
                        }
                        errorMessage.text(errorText).show();
                    },
                    complete: function() {
                        // Re-enable send button and hide typing indicator
                        sendButton.prop('disabled', false);
                        typingIndicator.hide();
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
