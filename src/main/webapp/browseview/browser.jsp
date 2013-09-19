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
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/browser.css">
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/datatables.css">
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/bootstrap.css">
        <script src="${pageContext.request.contextPath}/js/jquery-1.8.1.min.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/bootstrap.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/browser.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/jquery.dataTables.min.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/datatables.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/vkbeautify.0.99.00.beta.js" type="text/javascript"></script>
        <script src="http://ajax.googleapis.com/ajax/libs/webfont/1/webfont.js" type="text/javascript"></script>
    </head>
    <body>

        <div class="col-xs-12">
            <div class="row">

                <div class="col-md-5 containers-view">
                    <%@ include file="containers.jsp" %>
                </div>

                <div class="col-md-7" id="info-views">
                    <c:if test="${selected != null}" > <%@ include file="stat.jsp" %> </c:if>
                    <%@ include file="datasets.jsp" %>
                </div> <!-- End right side -->
            </div> <!-- end row -->
        </div>
    </body>
</html>