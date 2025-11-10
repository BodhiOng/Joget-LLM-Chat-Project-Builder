<%@ page import="org.joget.apps.app.service.AppUtil" %>
    <%@ page import="org.joget.commons.util.StringUtil" %>
        <%@ include file="/WEB-INF/jsp/includes/taglibs.jsp" %>

            <c:set var="appId" value="${appId}" />
            <c:set var="appVersion" value="${appVersion}" />
            <c:set var="key" value="${key}" />

            <% String contextPath=request.getContextPath(); String
                customCss=StringUtil.escapeString(request.getParameter("customCss"), StringUtil.TYPE_HTML, null); %>

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
                    <!-- Include marked.js for markdown formatting -->
                    <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>

                    <style>
                        .chat-container {
                            max-width: 1400px;
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
                            max-width: 90%;
                            width: fit-content;
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
                            padding-bottom: 15px;
                            position: static;
                        }

                        .bot-message::after {
                            content: "";
                            display: table;
                            clear: both;
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
                            0% {
                                background-position: 100% 50%;
                            }

                            100% {
                                background-position: 0% 50%;
                            }
                        }

                        .error-message {
                            color: red;
                            margin-top: 10px;
                            display: none;
                        }

                        /* Markdown formatting styles */
                        .bot-message pre {
                            background-color: #f5f5f5;
                            padding: 10px;
                            border-radius: 4px;
                            overflow-x: auto;
                            margin: 10px 0;
                            font-family: monospace;
                            font-size: 14px;
                        }

                        .bot-message code {
                            background-color: #f5f5f5;
                            padding: 2px 4px;
                            border-radius: 3px;
                            font-family: monospace;
                            font-size: 14px;
                        }

                        .bot-message blockquote {
                            border-left: 4px solid #ddd;
                            padding-left: 10px;
                            margin-left: 0;
                            color: #666;
                        }

                        .bot-message ul,
                        .bot-message ol {
                            padding-left: 20px;
                        }

                        .bot-message table {
                            border-collapse: collapse;
                            width: 100%;
                            margin: 10px 0;
                        }

                        .bot-message th,
                        .bot-message td {
                            border: 1px solid #ddd;
                            padding: 8px;
                            text-align: left;
                        }

                        .bot-message th {
                            background-color: #f2f2f2;
                        }

                        /* Response download button styles */
                        .response-download-btn {
                            display: inline-block;
                            padding: 5px 10px;
                            background-color: #007bff;
                            color: white;
                            border: none;
                            border-radius: 4px;
                            cursor: pointer;
                            font-size: 12px;
                            float: right;
                            margin-top: 15px;
                        }

                        .response-download-btn:hover {
                            background-color: #0056b3;
                        }

                        .response-download-btn:disabled {
                            background-color: #cccccc;
                            cursor: not-allowed;
                        }

                        /* Code block styles with copy button */
                        .code-block-container {
                            position: relative;
                            margin: 10px 0;
                        }

                        .copy-button {
                            position: absolute;
                            top: 5px;
                            right: 5px;
                            padding: 2px 8px;
                            background-color: #f8f9fa;
                            border: 1px solid #dee2e6;
                            border-radius: 3px;
                            font-size: 12px;
                            cursor: pointer;
                            opacity: 0.7;
                        }

                        .copy-button:hover {
                            opacity: 1;
                            background-color: #e9ecef;
                        }
                    </style>
                </head>

                <body>
                    <div class="chat-container">
                        <h2><i class="fas fa-comments"></i> LLM Chat</h2>

                        <div class="chat-messages" id="chatMessages">
                            <div class="message bot-message"
                                style="background-color: #ffffff; margin-right: auto; margin-left: 10px; border: 1px solid #e0e0e0; max-width: 80%;">
                                Hello! How can I assist you today?
                            </div>
                        </div>

                        <div class="message-input">
                            <textarea id="messageInput" placeholder="Type your message here..."></textarea>
                            <button id="sendButton"><i class="fas fa-paper-plane"></i> Send</button>
                        </div>

                        <!-- Typing indicator moved to chat window in the sendMessage function -->
                        <div id="errorMessage" style="color: red; margin-top: 10px; display: none;"></div>
                        <div id="debugInfo"
                            style="margin-top: 20px; padding: 10px; border: 1px solid #ddd; background-color: #f9f9f9;">
                            <h3>Debug Information</h3>
                            <p><strong>Service URL:</strong> <span id="serviceUrlDisplay"></span></p>
                            <p><strong>Plugin Name:</strong> org.joget.marketplace.LlmChatUserviewMenu</p>
                            <p><strong>App ID:</strong> ${appId}</p>
                            <p><strong>App Version:</strong> ${appVersion}</p>
                            <p><strong>Test Connection:</strong> <a href="javascript:void(0)"
                                    id="testConnectionLink">Click to Test Connection</a></p>
                            <p><strong>Test Response:</strong> <span id="testResponse"></span></p>

                            <h4>Direct Form Test</h4>
                            <form id="directTestForm"
                                action="/jw/web/json/plugin/org.joget.marketplace.LlmChatUserviewMenu/service"
                                method="post" target="_blank">
                                <input type="hidden" name="action" value="checkConnection">
                                <input type="hidden" name="appId" value="${appId}">
                                <input type="hidden" name="appVersion" value="${appVersion}">
                                <button type="submit">Test in New Window</button>
                            </form>

                            <h4>Direct URL Test</h4>
                            <p>Check Connection: <a
                                    href="/jw/web/json/plugin/org.joget.marketplace.LlmChatUserviewMenu/service?action=checkConnection&appId=${appId}&appVersion=${appVersion}"
                                    target="_blank">Test Connection</a></p>

                            <h4>Send Message Test</h4>
                            <form id="sendMessageTestForm"
                                action="/jw/web/json/plugin/org.joget.marketplace.LlmChatUserviewMenu/service"
                                method="post" target="_blank">
                                <input type="hidden" name="action" value="sendMessage">
                                <input type="hidden" name="appId" value="${appId}">
                                <input type="hidden" name="appVersion" value="${appVersion}">
                                <input type="text" name="message" value="Hello" style="width: 200px;">
                                <button type="submit">Send Test Message</button>
                            </form>

                            <h4>Plugin Class Name Check</h4>
                            <p>Full Class Name: <%= getClass().getName() %>
                            </p>
                        </div>
                    </div>

                    <script>
                        $(document).ready(function () {
                            const chatMessages = $('#chatMessages');
                            const messageInput = $('#messageInput');
                            const sendButton = $('#sendButton');
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
                            }).click(function () {
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
                            $('#testConnectionLink').click(function () {
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
                                    complete: function (xhr, status) {
                                        $('#testResponse').html('<pre style="margin: 0; padding: 5px; background-color: #eee; max-height: 200px; overflow: auto;">' +
                                            'Status: ' + status + '<br>' +
                                            'Response Code: ' + xhr.status + '<br>' +
                                            'Response Text: ' + xhr.responseText.replace(/</g, '&lt;').replace(/>/g, '&gt;') + '</pre>');
                                    }
                                });
                            });

                            // Function to format LLM responses with markdown and handle special characters
                            function formatLLMResponse(text) {
                                // First, temporarily replace <br> tags with a placeholder
                                let processedText = text.replace(/<br\s*\/?>/gi, '{{BR_PLACEHOLDER}}');

                                // Escape any HTML to prevent XSS
                                processedText = processedText
                                    .replace(/&/g, '&amp;')
                                    .replace(/</g, '&lt;')
                                    .replace(/>/g, '&gt;');

                                // Restore <br> tags from placeholders
                                processedText = processedText.replace(/{{BR_PLACEHOLDER}}/g, '<br>');

                                try {
                                    // Use marked.js to convert markdown to HTML
                                    const formattedText = marked.parse(processedText);
                                    return formattedText;
                                } catch (e) {
                                    console.error('Error formatting response:', e);
                                    return '<p>' + processedText + '</p>';
                                }
                            }

                            // Function to add a message to the chat
                            function addMessage(message, isUser) {
                                const messageDiv = $('<div>').addClass('message').addClass(isUser ? 'user-message' : 'bot-message');

                                if (!isUser) {
                                    messageDiv.css({
                                        'background-color': '#ffffff',
                                        'margin-right': 'auto',
                                        'margin-left': '10px',
                                        'border': '1px solid #e0e0e0',
                                        'padding-bottom': '15px'
                                    });

                                    // Format LLM responses with markdown
                                    messageDiv.html(formatLLMResponse(message));

                                    // Add download button if the message contains code
                                    if (message.includes('```') || message.includes('<pre>') || message.includes('<code>')) {
                                        const downloadBtn = $('<button>').addClass('response-download-btn')
                                            .html('<i class="fas fa-download"></i> Download Code')
                                            .attr('title', 'Download code from this response');

                                        // Add click handler for the download button
                                        downloadBtn.on('click', function () {
                                            // Disable the button during download
                                            downloadBtn.prop('disabled', true);
                                            downloadBtn.html('<i class="fas fa-spinner fa-spin"></i> Preparing...');

                                            try {
                                                // Get only this response's content
                                                const responseContent = messageDiv.html();

                                                // Create a form to submit the download request
                                                const form = $('<form>');
                                                form.attr('method', 'post');
                                                form.attr('action', '/jw/web/json/plugin/org.joget.marketplace.LlmChatUserviewMenu/service');
                                                form.attr('target', '_blank');

                                                // Add the action parameter
                                                const actionInput = $('<input>');
                                                actionInput.attr('type', 'hidden');
                                                actionInput.attr('name', 'action');
                                                actionInput.attr('value', 'downloadCode');
                                                form.append(actionInput);

                                                // Add the chat content parameter (only this response)
                                                const chatContentInput = $('<input>');
                                                chatContentInput.attr('type', 'hidden');
                                                chatContentInput.attr('name', 'chatContent');
                                                chatContentInput.attr('value', responseContent);
                                                form.append(chatContentInput);

                                                // Add the app ID and version parameters
                                                const appIdInput = $('<input>');
                                                appIdInput.attr('type', 'hidden');
                                                appIdInput.attr('name', 'appId');
                                                appIdInput.attr('value', '${appId}');
                                                form.append(appIdInput);

                                                const appVersionInput = $('<input>');
                                                appVersionInput.attr('type', 'hidden');
                                                appVersionInput.attr('name', 'appVersion');
                                                appVersionInput.attr('value', '${appVersion}');
                                                form.append(appVersionInput);

                                                // Append the form to the body and submit it
                                                $('body').append(form);
                                                form.submit();
                                                form.remove();

                                                // Re-enable the button after a delay
                                                setTimeout(function () {
                                                    downloadBtn.prop('disabled', false);
                                                    downloadBtn.html('<i class="fas fa-download"></i> Download Code');
                                                }, 2000);
                                            } catch (error) {
                                                console.error('Error downloading code:', error);
                                                alert('Error downloading code: ' + error.message);

                                                // Re-enable the button
                                                downloadBtn.prop('disabled', false);
                                                downloadBtn.html('<i class="fas fa-download"></i> Download Code');
                                            }
                                        });
                                        messageDiv.append(downloadBtn);
                                    }
                                } else {
                                    // User messages are displayed as plain text
                                    messageDiv.text(message);
                                }

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
                                    success: function (data) {
                                        // Extremely simplified success handling
                                        logDebug('Connection Check Response', data);

                                        // If we got any response, consider it a success
                                        callback(true);
                                    },
                                    error: function (xhr, status, error) {
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
                                    'border': '1px solid #e0e0e0',
                                    'padding-bottom': '15px'
                                });

                                // Add loading animation to the response div
                                const loadingAnimation = $('<div>').addClass('loading-animation');
                                responseDiv.append(loadingAnimation);
                                chatMessages.append(responseDiv);
                                chatMessages.scrollTop(chatMessages[0].scrollHeight);

                                // Check if Ollama is accessible first
                                checkOllamaConnection(function (isConnected) {
                                    if (!isConnected) {
                                        sendButton.prop('disabled', false);
                                        responseDiv.text('Error: Cannot connect to Ollama server');
                                        return;
                                    }

                                    // Use AJAX for responses with explicit text dataType
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
                                        success: function (data) {
                                            // Add bot response to chat with extremely simplified handling
                                            logDebug('Raw Success Response', data);

                                            try {
                                                // Try to parse as JSON
                                                const jsonData = JSON.parse(data);
                                                // Replace loading animation with formatted response text
                                                responseDiv.empty().html(formatLLMResponse(jsonData.response));

                                                // Add download button if the response contains code
                                                const responseText = jsonData.response || '';
                                                if (responseText.includes('```') || responseText.includes('<pre>') || responseText.includes('<code>')) {
                                                    const downloadBtn = $('<button>').addClass('response-download-btn')
                                                        .html('<i class="fas fa-download"></i> Download Code')
                                                        .attr('title', 'Download code from this response');

                                                    // Add click handler for the download button (same as above)
                                                    downloadBtn.on('click', function () {
                                                        // Disable the button during download
                                                        downloadBtn.prop('disabled', true);
                                                        downloadBtn.html('<i class="fas fa-spinner fa-spin"></i> Preparing...');

                                                        try {
                                                            // Get only this response's content
                                                            const responseContent = responseDiv.html();

                                                            // Create a form to submit the download request
                                                            const form = $('<form>');
                                                            form.attr('method', 'post');
                                                            form.attr('action', '/jw/web/json/plugin/org.joget.marketplace.LlmChatUserviewMenu/service');
                                                            form.attr('target', '_blank');

                                                            // Add the action parameter
                                                            const actionInput = $('<input>');
                                                            actionInput.attr('type', 'hidden');
                                                            actionInput.attr('name', 'action');
                                                            actionInput.attr('value', 'downloadCode');
                                                            form.append(actionInput);

                                                            // Add the chat content parameter (only this response)
                                                            const chatContentInput = $('<input>');
                                                            chatContentInput.attr('type', 'hidden');
                                                            chatContentInput.attr('name', 'chatContent');
                                                            chatContentInput.attr('value', responseContent);
                                                            form.append(chatContentInput);

                                                            // Add the app ID and version parameters
                                                            const appIdInput = $('<input>');
                                                            appIdInput.attr('type', 'hidden');
                                                            appIdInput.attr('name', 'appId');
                                                            appIdInput.attr('value', '${appId}');
                                                            form.append(appIdInput);

                                                            const appVersionInput = $('<input>');
                                                            appVersionInput.attr('type', 'hidden');
                                                            appVersionInput.attr('name', 'appVersion');
                                                            appVersionInput.attr('value', '${appVersion}');
                                                            form.append(appVersionInput);

                                                            // Append the form to the body and submit it
                                                            $('body').append(form);
                                                            form.submit();
                                                            form.remove();

                                                            // Re-enable the button after a delay
                                                            setTimeout(function () {
                                                                downloadBtn.prop('disabled', false);
                                                                downloadBtn.html('<i class="fas fa-download"></i> Download Code');
                                                            }, 2000);
                                                        } catch (error) {
                                                            console.error('Error downloading code:', error);
                                                            alert('Error downloading code: ' + error.message);

                                                            // Re-enable the button
                                                            downloadBtn.prop('disabled', false);
                                                            downloadBtn.html('<i class="fas fa-download"></i> Download Code');
                                                        }
                                                    });
                                                    // Append button directly to the message div for bottom-right positioning
                                                    responseDiv.append(downloadBtn);
                                                }
                                            } catch (e) {
                                                console.error('Error parsing response:', e);
                                                // Just use the raw data but still try to format it
                                                responseDiv.empty().html(formatLLMResponse(data));

                                                // Check for code in raw data
                                                if (data.includes('```') || data.includes('<pre>') || data.includes('<code>')) {
                                                    const downloadBtn = $('<button>').addClass('response-download-btn')
                                                        .html('<i class="fas fa-download"></i> Download Code')
                                                        .attr('title', 'Download code from this response');

                                                    // Add click handler for the download button (same as above)
                                                    downloadBtn.on('click', function () {
                                                        // Disable the button during download
                                                        downloadBtn.prop('disabled', true);
                                                        downloadBtn.html('<i class="fas fa-spinner fa-spin"></i> Preparing...');

                                                        try {
                                                            // Get only this response's content
                                                            const responseContent = responseDiv.html();

                                                            // Create a form to submit the download request
                                                            const form = $('<form>');
                                                            form.attr('method', 'post');
                                                            form.attr('action', '/jw/web/json/plugin/org.joget.marketplace.LlmChatUserviewMenu/service');
                                                            form.attr('target', '_blank');

                                                            // Add the action parameter
                                                            const actionInput = $('<input>');
                                                            actionInput.attr('type', 'hidden');
                                                            actionInput.attr('name', 'action');
                                                            actionInput.attr('value', 'downloadCode');
                                                            form.append(actionInput);

                                                            // Add the chat content parameter (only this response)
                                                            const chatContentInput = $('<input>');
                                                            chatContentInput.attr('type', 'hidden');
                                                            chatContentInput.attr('name', 'chatContent');
                                                            chatContentInput.attr('value', responseContent);
                                                            form.append(chatContentInput);

                                                            // Add the app ID and version parameters
                                                            const appIdInput = $('<input>');
                                                            appIdInput.attr('type', 'hidden');
                                                            appIdInput.attr('name', 'appId');
                                                            appIdInput.attr('value', '${appId}');
                                                            form.append(appIdInput);

                                                            const appVersionInput = $('<input>');
                                                            appVersionInput.attr('type', 'hidden');
                                                            appVersionInput.attr('name', 'appVersion');
                                                            appVersionInput.attr('value', '${appVersion}');
                                                            form.append(appVersionInput);

                                                            // Append the form to the body and submit it
                                                            $('body').append(form);
                                                            form.submit();
                                                            form.remove();

                                                            // Re-enable the button after a delay
                                                            setTimeout(function () {
                                                                downloadBtn.prop('disabled', false);
                                                                downloadBtn.html('<i class="fas fa-download"></i> Download Code');
                                                            }, 2000);
                                                        } catch (error) {
                                                            console.error('Error downloading code:', error);
                                                            alert('Error downloading code: ' + error.message);

                                                            // Re-enable the button
                                                            downloadBtn.prop('disabled', false);
                                                            downloadBtn.html('<i class="fas fa-download"></i> Download Code');
                                                        }
                                                    });
                                                    // Append button directly to the message div for bottom-right positioning
                                                    responseDiv.append(downloadBtn);
                                                }
                                            }
                                            chatMessages.scrollTop(chatMessages[0].scrollHeight);
                                        },
                                        error: function (xhr, status, error) {
                                            // Show error message
                                            let errorText = 'Error communicating with the LLM API';
                                            try {
                                                console.log('Error response:', xhr.responseText);
                                                console.log('Status code:', xhr.status);
                                                console.log('Error:', error);

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

                                                const debugDiv = $('<div>').css({
                                                    'background-color': '#ffeeee',
                                                    'border': '1px solid #ff0000',
                                                    'padding': '10px',
                                                    'margin-top': '10px',
                                                    'white-space': 'pre-wrap',
                                                    'font-family': 'monospace',
                                                    'font-size': '12px'
                                                });
                                                debugDiv.text('Raw Response:\n' + xhr.responseText);
                                                errorMessage.after(debugDiv);

                                                try {
                                                    const response = JSON.parse(xhr.responseText);
                                                    if (response.error) {
                                                        errorText = response.error;
                                                    }
                                                } catch (e) {
                                                    console.error('Error parsing error response:', e);
                                                    if (xhr.responseText && xhr.responseText.length < 200) {
                                                        errorText = xhr.responseText;
                                                    }
                                                }
                                            } catch (e) {
                                                console.error('Error parsing error response:', e);
                                            }
                                            errorMessage.text(errorText).show();

                                            // Replace loading animation with error message
                                            responseDiv.empty().text('Error: Could not get response from Ollama. Please try again.');
                                        },
                                        complete: function () {
                                            // Re-enable send button
                                            sendButton.prop('disabled', false);
                                        }
                                    });
                                }
                            }
                        }

                        // Send message when button is clicked
                        sendButton.click(sendMessage);

                        // Send message when Enter key is pressed (but allow Shift+Enter for new lines)
                        messageInput.keydown(function (event) {
                            if (event.keyCode === 13 && !event.shiftKey) {
                                event.preventDefault();
                                sendMessage();
                            }
                        });

                        // Add copy buttons to code blocks
                        function addCopyButtonsToCodeBlocks() {
                            // Find all pre elements within bot messages
                            $('.bot-message pre').each(function () {
                                // Only add button if it doesn't already have one
                                if ($(this).parent('.code-block-container').length === 0) {
                                    // Wrap the pre element in a container div
                                    $(this).wrap('<div class="code-block-container"></div>');

                                    // Add a copy button
                                    const copyButton = $('<button class="copy-button"><i class="fas fa-copy"></i> Copy</button>');
                                    $(this).parent().append(copyButton);

                                    // Add click event to copy button
                                    copyButton.click(function () {
                                        // Get the text content of the pre element
                                        const codeText = $(this).siblings('pre').text();

                                        // Create a temporary textarea element to copy the text
                                        const textarea = $('<textarea>');
                                        textarea.val(codeText).css({
                                            position: 'absolute',
                                            left: '-9999px'
                                        }).appendTo('body');

                                        // Select and copy the text
                                        textarea.select();
                                        document.execCommand('copy');

                                        // Remove the textarea
                                        textarea.remove();

                                        // Change button text temporarily
                                        const originalText = $(this).html();
                                        $(this).html('<i class="fas fa-check"></i> Copied!');

                                        // Reset button text after a delay
                                        setTimeout(() => {
                                            $(this).html(originalText);
                                        }, 2000);
                                    });
                                }
                            });
                        }

                        // Add copy buttons to code blocks when the DOM is ready
                        addCopyButtonsToCodeBlocks();

                        // Add copy buttons to code blocks when new messages are added
                        const originalAddMessage = addMessage;
                        addMessage = function (message, isUser) {
                            originalAddMessage(message, isUser);
                            if (!isUser) {
                                // Add copy buttons to code blocks after a short delay to ensure the DOM is updated
                                setTimeout(addCopyButtonsToCodeBlocks, 100);
                            }
                        };
            
        });
                    </script>
                </body>

                </html>