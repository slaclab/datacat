    <%-- 
    Document   : containers
    Created on : Sep 12, 2013, 12:15:46 PM
    Author     : bvan
--%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="http://srs.slac.stanford.edu/web_datacat" prefix="web_dc" %>

<div class="datact-component">
    <h3>Containers</h3>
    <table class="table table-condensed table-hover datatable-containers">
        <thead>
            <tr>
                <%--<th></th>--%>
                <th class="table-header-title">Name</th>
                <%--<th class="table-header-title"><span class="glyphicon glyphicon-folder-open" data-toggle="tooltip" data-placement="top" data-original-title="Folders" title="Folders"></span></th>
                <th class="table-header-title"><span class="glyphicon glyphicon-book" data-toggle="tooltip" data-placement="top" data-original-title="Groups" title="Groups"></span></th>
                <th class="table-header-title"><span class="glyphicon glyphicon-file" data-toggle="tooltip" data-placement="top" data-original-title="Datasets" title="Datasets"></span></th>--%>
            </tr>
        </thead>
        
        <tbody class="file-browser">
            <%-- Only groups or datasets should ever be these children --%>
            <c:forEach var="child" items="${containers}">

                <c:set var="isFolder" value='${child.getClass().simpleName eq "LogicalFolder"}' />
                <c:set var="isContainer" value='${child.getClass().simpleName eq "LogicalFolder" or child.getClass().simpleName eq "DatasetGroup"}' />
                <c:set var="needsInfo" value="${child.stat == null}" />
                <c:set var="hasContainers" value='${isFolder eq true and needsInfo eq false and child.stat.childContainerCount > 0}' />
                <c:set var="hasDatasets" value='${isContainer eq true and needsInfo eq false and child.stat.datasetCount > 0}' />
                <c:set var="hasChildren" value='${isContainer eq true and needsInfo eq false and child.stat.childCount > 0}' />
                <c:set var="iconName" value="${isFolder?'folder-open':'book'}" />
                <tr <c:if test="${child.pk == selected.pk}">class="success"</c:if> >
                    <%--<td>
                        <span class="glyphicon glyphicon-info-sign"></span>
                        <c:if test='${isFolder eq true and hasDatasets eq true and hasContainers eq true}' >
                            <a href="${pageContext.request.contextPath}/browser${parentURL}#datasets=${parentURL}/${child.name}">
                                <span class="glyphicon glyphicon-list" style="margin-left: 8px;" title="List datasets for ${child.name}"></span>
                            </a>
                        </c:if>
                    </td>--%>
                    <td>
                        <span class="glyphicon glyphicon-${iconName} type-icon"></span>
                        <c:choose>
                            <c:when test="${child.pk == selected.pk}" >
                                <a href="#selected" />${child.name}</a>
                            </c:when>
                            <c:otherwise>
                                <a href="${pageContext.request.contextPath}/browser${child.path}">${child.name}</a>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <%--<c:choose>
                        <c:when test="${needsInfo eq true}">
                            <td></td>
                            <td></td>
                            <td></td>
                        </c:when>
                        <c:otherwise>
                            <td><c:if test="${isFolder eq true}" >${child.stat.folderCount}</c:if></td>
                            <td><c:if test="${isFolder eq true}" >${child.stat.groupCount}</c:if></td>
                            <td>${child.stat.datasetCount}</td>
                        </c:otherwise>                            
                    </c:choose>--%>
                </tr>
            </c:forEach>    

        </tbody>
    </table>
    <button class="btn btn-primary btn-xs" id="paginate-containers-button" onclick="paginateContainers()">
        Paginate?
    </button>
</div>
