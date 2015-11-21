<%-- 
    Document   : browser.jsp
    Created on : Sep 19, 2012, 1:21:21 PM
    Author     : bvan
--%>
<%@taglib uri="http://srs.slac.stanford.edu/web_datacat" prefix="web_dc" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
    <head>  
        <title>Datacat Tree</title>
        <%@ include file="../views/jscontext.jsp" %>

        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/bootstrap.css">
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/browser.css">
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/tree.css">
        <script src="${pageContext.request.contextPath}/js/jquery-1.11.3.min.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/bootstrap.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/browser.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/URI.min.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/dynamicTree2.js" type="text/javascript"></script>
        <script src="http://ajax.googleapis.com/ajax/libs/webfont/1/webfont.js" type="text/javascript"></script>
    </head>
    <body>
        <div class="container-fluid">
            <div class="col-xs-12">
                <c:set var="view" value="tree"/>
                <div class="row">
                    <%@ include file="../views/tabbar.jsp" %>
                </div>

                <div class="row">
                    <%@ include file="../views/breadcrumb.jsp" %>
                </div>

                <div class="row">
                    <div class="container-fluid">
                        <div class="col-sm-6 col-md-5 col-lg-4">
                            <div class="datacat-component">
                                <h3>Containers</h3>
                                <div class="tree">  
                                    <ul class="tree-root"></ul>
                                </div>
                            </div>
                        </div>
                        <div class="col-sm-6 col-md-7 col-lg-8" id="info-views">
                            <c:choose>
                                <c:when test="${target.type.container}" >
                                    <%@ include file="../views/container.jsp" %>
                                </c:when>
                                <c:otherwise>
                                    <%@ include file="../views/dataset.jsp" %> 
                                </c:otherwise>
                            </c:choose>
                        </div> <!-- End right side -->
                    </div>
                </div> <!-- end row -->
            </div>
        </div>
    </body>
</html>