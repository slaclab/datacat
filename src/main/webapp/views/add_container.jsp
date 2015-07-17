<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@taglib uri="http://displaytag.sf.net" prefix="display" %>
<%@taglib uri="http://srs.slac.stanford.edu/GroupManager" prefix="gm" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib uri="http://srs.slac.stanford.edu/web_datacat" prefix="web_dc" %>

<div class="datacat-component">
    <div class="datacat-header">
        <div class="pull-left">
            <h3>${target.name}            
                <br>
                <small>${target.type} </small>
                <c:if test="${!empty target.description}">
                    <small>${target.description}</small></c:if>
            </h3>
        </div>
        <div class="btn-group pull-right">
            <a href="#edit" type="button" class="btn btn-default btn-med">
                <span class="glyphicon glyphicon-pencil" aria-hidden="true"></span> Edit
            </a>
            <div class="btn-group" role="group">
                <button type="button" class="btn btn-default btn-med dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                    <span class="glyphicon glyphicon-plus" aria-hidden="true"></span> Add
                    <span class="caret"></span>
                </button>
                <ul class="dropdown-menu dropdown-menu-right">
                    <li><a href="#addDataset">Dataset</a></li>
                    <li><a href="#addContainer">Group or Folder</a></li>
                </ul>               
                </div>
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
            <c:catch var="exception">
                <tr><th>Created (UTC):</th><td>${web_dc:formatTimestamp(target.dateCreated)}</td></tr>
                <tr><th>Datasets</th><td>${web_dc:formatEvents(target.stat.datasetCount)}</td></tr>
                <tr><th>Total Size</th><td>${web_dc:formatBytes(target.stat.diskUsageBytes)}</td></tr>
                <tr><th>Events</th><td>${web_dc:formatEvents(target.stat.eventCount)}</td></tr>
                    </c:catch>
        </tbody>
    </table>
            
  <%-- LINKS --%>            
        <%--<c:choose>
            <c:when test="${links != null && !empty links}"> --%>
                <ul class="nav nav-pills">
                    <li role="presentation"><a href="#">List Files</a></li>
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
        <c:choose>
            <c:when test="${target.metadata != null && !empty target.metadata}">
                <table class="table table-condensed table-striped">
                    <thead>
                        <tr>
                            <th>Key</th>
                            <th>Value</th>
                            <th>Type</th>
                        </tr>
                    </thead>

                    <tbody>
                        <c:forEach var="md" items="${target.metadata}" varStatus="status">
                            <tr><td>${md.key}</td><td>${md.rawValue}</td>
                                <td>${web_dc:getValueType(md.rawValue)}</td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:when>
            <c:otherwise>
                Nothing to display
            </c:otherwise>
        </c:choose>
    </c:catch>

</div>

