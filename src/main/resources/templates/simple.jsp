<%@ page import="org.joget.apps.app.service.AppUtil"%>
<%@ include file="/WEB-INF/jsp/includes/taglibs.jsp" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Simple Test Page</title>
</head>
<body>
    <h1>Simple Test Page</h1>
    <p>If you can see this page, JSP rendering is working correctly.</p>
    
    <div style="margin-top: 20px; padding: 10px; border: 1px solid #ccc;">
        <h2>Debug Information</h2>
        <p>Current time: <%= new java.util.Date() %></p>
        <p>Request URI: <%= request.getRequestURI() %></p>
        <p>Context Path: <%= request.getContextPath() %></p>
    </div>
</body>
</html>
