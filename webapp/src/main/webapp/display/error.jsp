<%-- 
    Document   : error.jsp
    Created on : Nov 13, 2014, 5:26:15 PM
    Author     : Brian Van Klaveren<bvan@slac.stanford.edu>
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <title>Error</title>
        <%@ include file="../views/jscontext.jsp" %>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/browser.css">
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/bootstrap.css">
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/dataTables.bootstrap.css">
        <script src="${pageContext.request.contextPath}/js/jquery-1.11.3.min.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/URI.min.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/bootstrap.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/jquery.dataTables.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/dataTables.bootstrap.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/datasets.js" type="text/javascript"></script>
        <script src="http://ajax.googleapis.com/ajax/libs/webfont/1/webfont.js" type="text/javascript"></script>
    </head>
    <body>

        <div class="container-fluid">

            <div class="row">
                <%@ include file="../views/tabbar.jsp" %>
            </div>

            <div class="row">

                <div class="container-fluid" id="info-views">
                    <div class="error">
                        Error: ${error.message} <br/>
                        Error detail: ${error.cause} <br/>

                    </div>
                </div>
            </div>
        </div>
    </body>
</html>
