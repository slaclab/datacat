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
    <div class="row">
        <div class="col-xs-7 pull-left">
            <div class="btn-group" role="group">
                <button type="button" class="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                    <span class="glyphicon glyphicon-check" aria-hidden="true" aria-lable="Select"></span>
                    <span class="caret"></span>
                </button>
                <ul class="dropdown-menu">
                    <li><a href="javascript:void(0)" onclick='$(".datatable-datasets input:checkbox").prop("checked", true)'>All</a></li>
                    <li><a href="javascript:void(0)" onclick='$(".datatable-datasets input:checkbox").prop("checked", false)'>None</a></li>
                    <li><a href="javascript:void(0)" onclick='$(".datatable-datasets input:checkbox").prop("checked", function (_, checked) {
                                return !checked;
                            })'>Toggle</a></li>
                </ul>
            </div>

            <div class="btn-group">
                <a id="dl" onclick="downloadUrl()" type="button" class="btn btn-default btn-sm">
                    <span class="glyphicon glyphicon-download-alt" aria-hidden="true" aria-label="Download"></span> Download
                </a>
                <div class="btn-group" role="group">
                    <button type="button" class="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                        More <span class="caret"></span>
                    </button>
                    <ul class="dropdown-menu">
                        <li><a href=href="javascript:void(0)">Skim</a></li>
                        <li><a href=href="javascript:void(0)"><span class="glyphicon glyphicon-option-horizontal" aria-hidden="true"></span> </a></li>
                    </ul>
                </div>
            </div>
        </div>

        <div class="col-xs-5 pull-right" id="DataTables_Table_0_filter">
            <div class="input-group">
                <input type="search" class="form-control input-sm" aria-controls="DataTables_Table_0" placeholder="Enter Filter...">
                <span class="input-group-btn">
                    <button class="btn btn-default btn-sm" type="button">Search</button>
                </span>
            </div>
        </div>

    </div>

    <div class="row">
        <table class="table table-ultracondensed table-hover datatable-datasets">
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
                                <input type="checkbox" value="${child.path}">
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
</div>
