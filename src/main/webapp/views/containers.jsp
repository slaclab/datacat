<%-- 
Document   : containers
Created on : Sep 12, 2013, 12:15:46 PM
Author     : bvan
--%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page contentType="text/html" pageEncoding="UTF-8"%>

<div class="datacat-component">
    <h3>Containers</h3>
    <table class="table table-condensed table-hover datatable-containers">
        <thead>
            <tr>
                <th class="table-header-title">Name</th>
            </tr>
        </thead>

        <tbody class="file-browser">
            <c:forEach var="child" items="${model.containers}">

                <c:set var="iconName" value="${child.type eq 'FOLDER' ? 'folder-open' : 'book'}" />
                <tr <c:if test="${child.pk == selected.pk}">class="success"</c:if> >
                        <td>
                            <span class="glyphicon glyphicon-${iconName} type-icon"></span>
                        <c:choose>
                            <c:when test="${child.pk == selected.pk}" >
                                <a href="#selected" />${child.name}</a>
                            </c:when>
                            <c:otherwise>
                                <a href="${model.endPoint}${child.path}">${child.name}</a>
                            </c:otherwise>
                        </c:choose>
                    </td>
                </tr>
            </c:forEach>    

        </tbody>
    </table>
</div>
