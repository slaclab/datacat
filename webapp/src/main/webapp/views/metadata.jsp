<%-- 
    Document   : metadata
    Created on : Sep 17, 2015, 12:49:27 PM
    Author     : bvan
--%>

<%@taglib uri="http://srs.slac.stanford.edu/web_datacat" prefix="web_dc" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/dataTables.bootstrap.css">
<script src="${pageContext.request.contextPath}/js/jquery.dataTables.js" type="text/javascript"></script>
<script src="${pageContext.request.contextPath}/js/dataTables.bootstrap.js" type="text/javascript"></script>


<script>
    var columns = [
        {"name": "key"},
        {"name": "value"},
        {"name": "type"}
    ];

    $("document").ready(function(){
        $('.mdtable').DataTable({
            "columns": columns,
            "lengthMenu": [ 25, 50, 100, 1000, 10000 ]
        });

    });

</script>

<c:choose>
    <c:when test="${mdlist != null && !empty mdlist}">
        <table class="table table-condensed table-striped mdtable">
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