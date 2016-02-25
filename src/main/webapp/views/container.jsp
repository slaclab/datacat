<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib uri="http://srs.slac.stanford.edu/web_datacat" prefix="web_dc" %>


<div class="datacat-component">
    <div class="datacat-header">
        <div class="pull-left">
            <h3>${model.target.name}            
                <br>
                <small>${model.target.type} </small>
                <c:if test="${!empty model.target.description}">
                    <small>${model.target.description}</small></c:if>
                </h3>
            </div>
            <c:if test="${model.writable}">
            <div class="btn-group pull-right">
                <a href="${model.applicationBase}/edit${model.target.path}" type="button" class="btn btn-default btn-med">
                <span class="glyphicon glyphicon-pencil" aria-hidden="true"></span> Edit
            </a>
            </c:if>
            <c:if test="${model.insertable}" >
            <c:choose>
                <c:when   test="${model.target.type == 'FOLDER'}">
                    <div class="btn-group" role="group">
                        <button type="button" class="btn btn-default btn-med dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                            <span class="glyphicon glyphicon-plus" aria-hidden="true"></span> Add
                            <span class="caret"></span>
                        </button>
                        <ul class="dropdown-menu dropdown-menu-right">
                            <li><a href="${model.applicationBase}/new${model.target.path}?type=dataset">Dataset</a></li>
                            <li><a href="${model.applicationBase}/new${model.target.path}?type=container">Group or Folder</a></li>
                        </ul>
                    </div>
                </c:when>
                <c:otherwise>
                    <a href="${model.applicationBase}/new${model.target.path}?type=dataset" type="button" class="btn btn-default btn-med">
                        <span class="glyphicon glyphicon-plus" aria-hidden="true"></span> Add Dataset
                    </a>
                </c:otherwise>
            </c:choose>
            </c:if>
        </div>
    </div>

    <table class="table table-condensed table-striped location-table">
        <thead>
            <tr>
                <th>Name</th>
                <th>Value</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <%-- TODO: Handle Create dates
                <th>Created (UTC):</th><td>${web_dc:formatTimestamp(target.created)}</td></tr>
                --%>
                <c:if test="${model.target.stat ne null}">
                    <tr><th>Datasets</th><td>${web_dc:formatEvents(model.target.stat.datasetCount)}</td></tr>
                    <tr><th>Total Size</th><td>${web_dc:formatBytes(model.target.stat.diskUsageBytes)}</td></tr>
                    <tr><th>Events</th><td>${web_dc:formatEvents(model.target.stat.eventCount)}</td></tr>
                </c:if>
        </tbody>
    </table>

    <%-- LINKS --%>            
    <%--<c:choose>
        <c:when test="${links != null && !empty links}"> --%>
    <ul class="nav nav-pills">
        <li role="presentation"><a href="${model.contextPath}/display/datasets${model.target.path}">List Datasets</a></li>
        <li role="presentation"><a href="#">Download Files</a></li>
        <li role="presentation"><a href="#">Dump File List</a></li>
        <li role="presentation"><a href="#">More...</a></li>
    </ul>
    <%--</c:when>
    <c:otherwise>
        Nothing to display
    </c:otherwise>
</c:choose> --%>

    <c:catch var="exception">
        <div class="clearfix">
            <h3>Metadata</h3>
        </div>
        <c:set var="mdlist" value="${model.target.metadata}" />
        <%@ include file="../views/metadata.jsp" %>
    </c:catch>

</div>

