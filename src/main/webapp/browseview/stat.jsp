<%-- 
    Document   : stat
    Created on : Sep 12, 2013, 12:17:23 PM
    Author     : bvan
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@taglib uri="http://srs.slac.stanford.edu/GroupManager" prefix="gm" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib uri="http://srs.slac.stanford.edu/web_datacat" prefix="web_dc" %>


<div class="datacat-component">
<div class="datacat-header">
    <h3>${container.name}<br>
    <small>${container.type} </small>
    <c:if test="${!empty container.description}">
        <small>${container.description}</small></h3>
    </c:if>
</div>

  <table class="table table-condensed table-hover">
      <thead>
          <tr>
              <th>Datasets</th>
              <th>Total Size</th>
              <th>Events</th>
          </tr>
      </thead>
      <tbody class="datasets">
          <tr>
              <td>${web_dc:formatEvents(container.stat.datasetCount)}</td>
              <td>${web_dc:formatBytes(container.stat.diskUsageBytes)}</td>
              <td>${web_dc:formatEvents(container.stat.eventCount)}</td>
          </tr>
      </tbody>
  </table>
            
    <c:catch var="exception">
        <h3>Metadata</h3>

        <c:choose>
            <c:when test="${container.metadata != null && !empty container.metadata}">
                <table class="table table-condensed table-striped">
                    <thead>
                        <tr>
                            <th>Key</th>
                            <th>Value</th>
                            <th>Type</th>
                        </tr>
                    </thead>

                    <tbody>
                        <c:forEach var="md" items="${container.metadata}" varStatus="status">
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