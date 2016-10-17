<%-- 
    Document   : browser.jsp
    Created on : Sep 19, 2012, 1:21:21 PM
    Author     : bvan
--%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
    <head>
        <title>Datacat Datasets</title>
        <%@ include file="/views/jscontext.jsp" %>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/bootstrap.css">
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/browser.css">
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
                <c:set var="view" value="datasets"/>
                <div class="row">
                    <%@ include file="/views/tabbar.jsp" %>
                </div>

                <div class="row">
                    <%@ include file="/views/breadcrumb.jsp" %>
                </div>

                <div class="row">
                    <div class="container-fluid" id="info-views">

                        <c:choose>
                            <c:when test="${!model.target.type.container}" > <%@ include file="/views/dataset.jsp" %> </c:when>
                            <c:otherwise>
                                <%@ include file="/views/datasets.jsp" %>
                            </c:otherwise>
                        </c:choose>
                    </div> <!-- End right side -->
                </div> <!-- end row -->
            </div>
        </div>
    </body>
</html>