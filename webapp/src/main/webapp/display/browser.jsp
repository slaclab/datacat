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
        <title>Datacat Browser</title>
        <%@ include file="/views/jscontext.jsp" %>

        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/bootstrap.css">
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/browser.css">
        <script src="${pageContext.request.contextPath}/js/jquery-1.11.3.min.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/bootstrap.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/browser.js" type="text/javascript"></script>
        <script src="http://ajax.googleapis.com/ajax/libs/webfont/1/webfont.js" type="text/javascript"></script>
        <link rel="shortcut icon" type="image/x-icon" href="${pageContext.request.contextPath}/icons/favicon.png" />
    </head>

    <body>
        <div class="container-fluid">
            <div class="col-xs-12">
                <c:set var="view" value="browser"/>
                <div class="row">
                    <%@ include file="/views/tabbar.jsp" %>
                </div>

                <div class="row">
                    <%@ include file="/views/breadcrumb.jsp" %>
                </div>

                <div class="row">

                    <div class="container-fluid" id="info-views">
                        <c:choose>
                            <c:when test="${model.target.type.container || model.target == null}" >
                                <div class="col-sm-5 col-md-4 col-lg-4">
                                    <%@ include file="/views/containers.jsp" %>
                                </div>

                                <div class="col-sm-7 col-md-8 col-lg-8">
                                    <c:if test="${model.target != null}">
                                        <%@ include file="/views/container.jsp" %>
                                    </c:if>
                                </div> <!-- End right side -->
                            </c:when>
                            <c:otherwise>
                                <%@ include file="/views/dataset.jsp" %> 
                            </c:otherwise>
                        </c:choose>
                    </div> <!-- end row -->
                </div>
            </div>
        </div>
    </body>

</html>