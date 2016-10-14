<%-- 
    Document   : tabbar
    Created on : Nov 19, 2015, 5:13:56 PM
    Author     : bvan
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:catch var="exception"><c:set var="path" value="${model.target.path}"/></c:catch>

<c:if test="${not empty exception}">
    <c:set var="path" value="/"/>
</c:if>

<ul class="nav nav-tabs">
    <li role="presentation" class="home ${view == 'browser' ? 'active' : ''}">
        <a data-placement="right" href="${pageContext.request.contextPath}/display/browser${path}" title="Browser">
            <span class="glyphicon glyphicon-th-list" aria-hidden="true"></span>
            <span class="nav-word">Browser</span>
        </a>
    </li>
    <li role="presentation" class="${view == 'tree' ? 'active' : ''}">
        <a data-placement="right" href="${pageContext.request.contextPath}/display/tree${path}" title="Tree">
            <span class="glyphicon glyphicon-tree-deciduous" aria-hidden="true"></span>
            <span class="nav-word">Tree</span>
        </a>
    </li>
    <li role="presentation" class="${view == 'datasets' ? 'active' : ''}">
        <a data-placement="right" href="${pageContext.request.contextPath}/display/datasets${path}" title="Datasets">
            <span class="glyphicon glyphicon-duplicate" aria-hidden="true"></span>
            <span class="nav-word">Datasets</span>
        </a>
    </li>
    <!--
    <li role="presentation" class="${view == 'search' ? 'active' : ''}">
        <a data-placement="right" href="${pageContext.request.contextPath}/display/search" title="Search">
            <span class="glyphicon glyphicon-search" aria-hidden="true"></span>
            <span class="nav-word">Search</span>
        </a>
    </li>-->
</ul>