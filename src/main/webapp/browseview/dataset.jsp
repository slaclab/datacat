<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@taglib uri="http://displaytag.sf.net" prefix="display" %>
<%@taglib uri="http://srs.slac.stanford.edu/GroupManager" prefix="gm" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib uri="http://srs.slac.stanford.edu/web_datacat" prefix="web_dc" %>

        
<c:forEach var="location" items="${dataset.viewInfo.locations}" varStatus="status">
    <c:if test="${location.isMaster().booleanValue()}">
        <c:set var="master" value="${location}" />
    </c:if>
</c:forEach>

<%--
    <c:if test="${param.action=='download'}">            
        <datacat:downloadLink datasetList="${paramValues.dataset}" datasetVersionList="${paramValues.datasetVersion}" redirect="true"/>                    
    </c:if>
--%>
<h2>Dataset ${dataset.name} version ${dataset.versionId}</h2>    

<h3>Standard Data</h3>

<table class="table table-condensed table-striped">
    <thead>
        <tr>
            <th>Name</th>
            <th>Value</th>
        </tr>
    </thead>
    <tbody>
        <c:catch var="exception">
            <tr><th>Created (UTC):</th><td>${dataset.versionCreated}</td></tr>
        </c:catch>
            
        <tr><th>File Format:</th><td>${dataset.fileFormat}</td></tr>
        <tr><th>Data Type:</th><td>${dataset.dataType}</td></tr>

        <c:catch var="exception">
            <tr><th>Source:</th><td>${dataset.dataSource}</td></tr>
        </c:catch>
        <c:catch var="exception">
            <tr><th>Size:</th><td>${web_dc:formatBytes(master.size)}<%--${datacat:formatBytes(master.size)}--%></td></tr>
        </c:catch>
        <c:catch var="exception">
            <tr><th>Master Site:</th><td>${master.site}</td></tr>
        </c:catch>
        <c:catch var="exception">
            <tr><th>Master resource:</th><td>${master.resource}</td></tr>
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
            <c:if test="${empty dataset.processInstance}">
                <th>Task:</th><td><a target="_top" href="${appVariables.pipelineUrl}/pi.jsp?pi=${dataset.processInstance}&experiment=${appVariables.experiment}">${dataset.taskName}</a></td>
            </c:if>
        </c:catch>
            <c:if test="${!empty dataset.rootversion}">
                <th>Root Version:</th><td>${dataset.rootversion}</td>
                <th>Tree Name:</th><td>${dataset.ttreename}</td>
                <th>SoLib Version:</th><td>${dataset.solibversion}</td>
            </c:if>
            <th>Links</th>
            <td>
                <datacat:downloadLink dataset="${dataset.pk}"/>
                <c:if test="${dataset.dataType == 'MERIT'}">
                    <datacat:skimLink path="${folderName}" group="${groupName}" dataset="${dataset.DatasetName}"/>
                </c:if>
                <a href="logViewer.jsp?minDate=-1&severity=0&dataset=${dataset.dataset}" target="_top">History</a>
            </td>
        --%>
    </tbody>
</table>

<h3>Version Metadata</h3>

<c:forEach var="location" items="${dataset.versionMetadata}" varStatus="status">
    <display:table class="dataTable" name="${results.rows}" defaultsort="1" defaultorder="ascending">
        <display:column property="metaname"  title="Name" sortable="true" headerClass="sortable"/>
        <display:column property="metavalue" title="Value" sortable="true" headerClass="sortable" />
        <display:column property="type"  title="Type" sortable="true" headerClass="sortable" />
    </display:table>  
</c:forEach>

<%--
    <c:if test="${gm:isUserInGroup(pageContext,'DataCatalogAdmin')}">
        <a href="metadata.jsp?datasetVersion=${dataset.datasetVersion}" class="edit">Edit meta-data</a>
    </c:if>

--%>

<h3>Locations</h3>

<table class="table table-condensed table-striped">
    <thead>
        <tr>
            <th>Site</th>
            <th>Scan Status</th>
            <th>Checked (UTC)</th>
            <th>Resource</th>
        </tr>
    </thead>
    
    <tbody>
        <c:forEach var="location" items="${dataset.viewInfo.locations}" varStatus="status">
            <tr><td>${location.site}</td><td>${location.scanStatus}</td><td>${location.dateScanned}</td><td>${location.resource}</td></tr>
        </c:forEach>
    </tbody>
</table>
<%--</table>
    <display:table class="dataTable" name="${results.rows}" defaultsort="1" defaultorder="ascending" decorator="org.srs.datacatalog.web.decorators.LocationDecorator">
        <display:column property="DatasetSite"  title="Site" sortable="true" headerClass="sortable"/>
        <display:column property="ScanStatus"  title="Status" sortable="true" headerClass="sortable"/>
        <display:column property="LastScanned" title="Checked (UTC)" sortable="true" headerClass="sortable" decorator="org.srs.web.base.decorator.TimestampColumnDecorator"/>
        <display:column property="location"  title="Location" sortable="true" headerClass="sortable" />
    </display:table> 
</c:forEach>
--%>