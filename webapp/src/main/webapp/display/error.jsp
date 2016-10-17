<%-- 
    Document   : error.jsp
    Created on : Nov 13, 2014, 5:26:15 PM
    Author     : Brian Van Klaveren<bvan@slac.stanford.edu>
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
    <head>
        <title>Error</title>
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
            <div class="col-xs-12">
                <c:set var="view" value="error"/>
                <div class="row">
                    <%@ include file="/views/tabbar.jsp" %>
                </div>

                <div class="row">
                    <div class="container-fluid" id="info-views">
                        <div class="panel panel-danger"> 
                            <div class="panel-heading"> <h3 class="panel-title">Error</h3> </div> 
                            <div class="panel-body">
                                ${model.message}
                            </div> 
                        </div>
                        <button class="btn btn-primary" type="button" data-toggle="collapse" data-target="#collapseExample" aria-expanded="false" aria-controls="collapseExample">
                            More details....
                        </button>
                        <div class="collapse" id="collapseExample"> <div class="well">
                                <h2>Cause Message: </h2>
                                <pre>
                                    ${model.cause.message}
                                </pre>
                                <h2>Cause</h2> <br>
                                <pre>
                                    ${model.cause}
                                </pre>
                            </div> 
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
