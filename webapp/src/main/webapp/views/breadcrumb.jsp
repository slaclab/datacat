<%-- 
    Document   : breadcrumb
    Created on : Jul 17, 2015, 3:42:26 PM
    Author     : bvan
--%>

<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<div class="datacat-path ">
    <div class="btn-group pull-right">
        <button id="copy-button" class="btn btn-default btn-sm pull-right" data-clipboard-text="${model.target.path}" title="Copy Path">Copy Path</button>
        <script src="${pageContext.request.contextPath}/js/ZeroClipboard.min.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/breadcrumb.js"></script>
    </div>

    <ol class="breadcrumb pull-left">
        <li><a href="${model.endPoint}">Root</a></li>
            <c:if test="${model.target ne null}">
                <c:set var="pathPart" value="" />

            <c:set var="parentPath" value='${model.target.path.lastIndexOf("/") > 1 ? 
                                             model.target.path.substring(1, model.target.path.lastIndexOf("/")) 
                                             : "/"}' />
            <c:set var="pathList" value='${parentPath.split("/")}' />

            <c:forEach var="pathElem" items="${pathList}" varStatus="status">
                <li><a href="${model.endPoint}${pathPart}/${pathElem}">${pathElem}</a></li>
                    <c:set var="pathPart" value='${pathPart}/${pathElem}' />
                </c:forEach>
            <li class="active">${model.target.name}</li>
            </c:if>
    </ol>

</div>