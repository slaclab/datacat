
<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@taglib uri="http://displaytag.sf.net" prefix="display" %>
<%@taglib uri="http://srs.slac.stanford.edu/GroupManager" prefix="gm" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib uri="http://srs.slac.stanford.edu/web_datacat" prefix="web_dc" %>


<c:forEach var="location" items="${target.viewInfo.locations}" varStatus="status">
    <c:if test="${location.isMaster().booleanValue()}">
        <c:set var="master" value="${location}" />
    </c:if>
</c:forEach>

<%--
    <c:if test="${param.action=='download'}">            
        <datacat:downloadLink datasetList="${paramValues.dataset}" datasetVersionList="${paramValues.datasetVersion}" redirect="true"/>                    
    </c:if>
--%>
<div class="datacat-component">
    <div class="datacat-header">

        <h3>${target.name}
            <a href="${applicationBase}/edit${target.path}" type="button" class="btn btn-default btn-med pull-right">
                <span class="glyphicon glyphicon-pencil" aria-hidden="true"></span> Edit
            </a>
            <br>
            <small>Version ${target.versionId}</small><br>
            <small>Dataset</small><br>
        </h3>
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
                    </c:catch>

            <tr><th>File Format:</th><td>${target.fileFormat}</td></tr>
            <tr><th>Data Type:</th><td>${target.dataType}</td></tr>

            <c:catch var="exception">
                <c:if test="${target.dataSource != null}">
                    <tr><th>Source:</th><td>${target.dataSource}</td></tr>
                        </c:if>
                    </c:catch>
                    <c:catch var="exception">
                <tr><th>Size:</th><td>${web_dc:formatBytes(master.size)}<%--${datacat:formatBytes(master.size)}--%></td></tr>
                    </c:catch>
                    <c:catch var="exception">
                <tr><th>Master Site:</th><td>${master.site}</td></tr>
                    </c:catch>
                    <c:catch var="exception">
                <tr><th>Master resource:</th><td class="location-resource">${master.resource}</td></tr>
                    </c:catch>
                    <c:catch var="exception">
                <tr><th>Run Min:</th><td>${master.runMin}</td></tr>
                    </c:catch>
                    <c:catch var="exception">
                <tr><th>Run Max:</th><td>${master.runMax}</td></tr>
                    </c:catch>
                    <c:catch var="exception">
                <tr><th>Events:</th><td>${web_dc:formatEvents(master.eventCount)}</td></tr>
                    </c:catch>
                    <%--
                    <c:catch var="exception">
                        <c:if test="${empty target.processInstance}">
                            <th>Task:</th><td><a target="_top" href="${appVariables.pipelineUrl}/pi.jsp?pi=${target.processInstance}&experiment=${appVariables.experiment}">${target.taskName}</a></td>
                        </c:if>
                    </c:catch>
                        <c:if test="${!empty target.rootversion}">
                            <th>Root Version:</th><td>${target.rootversion}</td>
                            <th>Tree Name:</th><td>${target.ttreename}</td>
                            <th>SoLib Version:</th><td>${target.solibversion}</td>
                        </c:if>
                        <th>Links</th>
                        <td>
                            <datacat:downloadLink dataset="${target.pk}"/>
                            <c:if test="${target.dataType == 'MERIT'}">
                                <datacat:skimLink path="${folderName}" group="${groupName}" dataset="${target.name}"/>
                            </c:if>
                            <a href="logViewer.jsp?minDate=-1&severity=0&dataset=${target.pk}" target="_top">History</a>
                        </td>
                    --%>
        </tbody>
    </table>

    <c:catch var="exception">
        <h3>Version Metadata</h3>

        <c:choose>
            <c:when test="${target.versionMetadata != null && !empty target.versionMetadata}">
                <table class="table table-condensed table-striped">
                    <thead>
                        <tr>
                            <th>Key</th>
                            <th>Value</th>
                            <th>Type</th>
                        </tr>
                    </thead>

                    <tbody>
                        <c:forEach var="md" items="${target.versionMetadata}" varStatus="status">
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
    <%--
        <c:if test="${gm:isUserInGroup(pageContext,'DataCatalogAdmin')}">
            <a href="metadata.jsp?datasetVersion=${dataset.datasetVersion}" class="edit">Edit meta-data</a>
        </c:if>
    --%>

    <c:catch var="exception">
        <h3>Locations</h3>
        <c:choose>
            <c:when test="${target.viewInfo.locations != null && !empty target.viewInfo.locations}">
                <table class="table table-condensed table-striped location-table">
                    <thead>
                        <tr>
                            <th class="location-site">Site</th>
                            <th class="location-status">Scan Status</th>    
                            <th class="location-ts">Created</th>
                            <th class="location-ts">Last Scanned</th>
                            <th class="location-resource"><div class="location-resource">Resource</div></th>
                    </tr>
                    </thead>

                    <tbody>
                        <c:forEach var="location" items="${target.viewInfo.locations}" varStatus="status">
                            <tr>
                                <td >${location.site}</td>
                                <td>${location.scanStatus}</td>
                                <td>${web_dc:formatTimestamp(location.dateCreated)}</td>
                                <td>${web_dc:formatTimestamp(location.dateScanned)}</td>
                                <td class="location-resource">${location.resource}</td>
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