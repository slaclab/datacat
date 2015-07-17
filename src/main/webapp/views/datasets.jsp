<%-- 
    Document   : dscontainers
    Created on : Sep 12, 2013, 10:31:35 AM
    Author     : bvan
--%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="http://srs.slac.stanford.edu/web_datacat" prefix="web_dc" %>

<script>
    var resultInfo = {
        "total": ${datasetCount}
    }
</script>

<div class="datacat-component">
    <h3>Datasets</h3>
    <table class="table table-condensed table-hover datatable-datasets">
        <thead>
            <tr>
                <th class="table-file-info"></th>
                <th>Name</th>
                <th>Type</th>
                <th>Size</th>
                <th>Created</th>
            </tr>
        </thead>
        <tbody class="datasets">
            <c:if test="${datasets != null}">
                <c:forEach var="child" items="${datasets}">
                    <c:forEach var="location" items="${child.viewInfo.locations}" varStatus="status">
                        <c:if test="${location.isMaster().booleanValue()}">
                            <c:set var="master" value="${location}" />
                        </c:if>
                    </c:forEach>
                    <tr>
                        <td>
                            <div class="checkbox">
                                <input type="checkbox" value="${child.path}">
                            </div>
                        </td>
                        <td>
                            <a href="${endPoint}${child.path}" pk="${child.pk}">${child.name}</a>
                        </td>
                        <td>${child.dataType}</td>
                        <td>
                            <c:catch var="exception">${web_dc:formatBytes(master.size)}</c:catch>
                            <c:if test="${not empty exception}">N/A</c:if>
                        </td>
                        <td>
                            <c:catch var="exception">${web_dc:formatTimestamp(child.dateCreated)}</c:catch>
                            <c:if test="${not empty exception}">N/A</c:if>

                        </td>
                    </tr>
                </c:forEach> 
            </c:if>
        </tbody>
    </table>
</div>
