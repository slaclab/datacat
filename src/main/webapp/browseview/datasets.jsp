<%-- 
    Document   : dscontainers
    Created on : Sep 12, 2013, 10:31:35 AM
    Author     : bvan
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="http://srs.slac.stanford.edu/web_datacat" prefix="web_dc" %>

<div class="datasets">
    <div class="panel panel-default">
        <div class="panel-heading">

            List of datasets for <span id="datasets-location">${selected.path} / ${selected.name}</span>
            <div class="pull-right">
                <a class="accordion-toggle" data-toggle="collapse" data-target="#ds-collapse"><span class="glyphicon glyphicon-th-list" style="font-size:14px;" data-toggle="tooltip" title="Show/Hide Datasets"></span></a>
            </div>
        </div>
        <!--<button class="btn btn-primary btn-sm" id="paginate-datasets-button" onclick="paginateDatasets()">
            Paginate?
        </button> -->
        <div id="ds-collapse" class="panel-collapse in">
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
                        <tr>
                            <td>
                                <span class="glyphicon glyphicon-info-sign"></span>
                                <span class="glyphicon glyphicon-download-alt"></span>
                            </td>
                            <td>
                                <a href="${pageContext.request.contextPath}/browser${child.path}/${child.name}" pk="${child.pk}">${child.name}</a>
                            </td>
                            <td>${child.dataType}</td>
                            <td>${web_dc:formatBytes(child.fsize)}</td>
                            <td><fmt:formatDate value="${child.versionRegistrationDate}"  pattern="yyyy-MM-dd HH:mm z" /></td>
                        </tr>
                    </c:forEach> 
                </c:if>

                </tbody>
            </table>
        </div>
    </div>
</div> <!-- end datasets -->
