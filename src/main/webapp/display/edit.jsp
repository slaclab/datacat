<%-- 
    Document   : browser.jsp
    Created on : Sep 19, 2012, 1:21:21 PM
    Author     : bvan
--%>
<%@taglib uri="http://srs.slac.stanford.edu/web_datacat" prefix="web_dc" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html lang="en">
    <head>  
        <script>
            var pageContext = new Object();
            pageContext.endPoint = "${endPoint}";
            pageContext.applicationBase = "${applicationBase}";
            pageContext.target = new Object();
            pageContext.target.name = "${target.name}";
            pageContext.target.path = "${target.path}";
            pageContext.target.type = "${target.type}";
        </script>

        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/browser.css">
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/bootstrap.css">
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/dataTables.bootstrap.css">
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/tree.css">
        <script src="${pageContext.request.contextPath}/js/jquery-1.11.3.min.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/URI.min.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/bootstrap.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/browser.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/mindmup-editabletable.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/jquery.dataTables.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/dataTables.bootstrap.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/vkbeautify.0.99.00.beta.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/dynamicTree2.js" type="text/javascript"></script>
        <script src="http://ajax.googleapis.com/ajax/libs/webfont/1/webfont.js" type="text/javascript"></script>
    </head>
    <body>

        <div class="col-xs-12">
            
            <div class="row">
                <%@ include file="../views/breadcrumb.jsp" %>
            </div>
            
            <div class="row">
                <div class=" col-xs-12" id="info-views">
                    <c:choose>
                        <c:when test="${target.type.container}" > <%@ include file="../views/edit_container.jsp" %> </c:when>
                        <%-- <c:when test="${overflow != null}" > Overflow </c:when> --%>
                        <c:otherwise>
                            <%@ include file="../views/edit_dataset.jsp" %>
                        </c:otherwise>
                    </c:choose>
                </div> <!-- End right side -->
            </div> <!-- end row -->
        </div>
    </body>
</html>