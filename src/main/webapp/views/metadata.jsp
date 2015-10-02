<%-- 
    Document   : metadata
    Created on : Sep 17, 2015, 12:49:27 PM
    Author     : bvan
--%>

<%@taglib uri="http://srs.slac.stanford.edu/web_datacat" prefix="web_dc" %>

<c:choose>
    <c:when test="${mdlist != null && !empty mdlist}">
        <table class="table table-condensed table-striped">
            <thead>
                <tr>
                    <th>Key</th>
                    <th>Value</th>
                    <th>Type</th>
                </tr>
            </thead>

            <tbody>
                <c:forEach var="md" items="${mdlist}" varStatus="status">
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