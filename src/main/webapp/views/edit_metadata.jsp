<%-- 
    Document   : metadata
    Created on : Sep 17, 2015, 12:49:27 PM
    Author     : bvan
--%>

<%@taglib uri="http://srs.slac.stanford.edu/web_datacat" prefix="web_dc" %>


<div class="clearfix">
    <button type="button" class="btn btn-success btn-med md-add">
        <span class="glyphicon glyphicon-plus" aria-hidden="true"></span> Add
    </button>
</div>

<table class="table table-condensed table-striped mdedit">
    <thead>
        <tr>
            <th class="md-action">Action</th>
            <th>Key</th>
            <th>Value</th>
            <th class="md-type">Type</th>
        </tr>
    </thead>

    <tbody class="md-editable">
        <c:if test="${mdlist != null && !empty mdlist}">

            <c:forEach var="md" items="${mdlist}" varStatus="status">
                <tr class="md-existing">
                    <td><span class="md-action glyphicon glyphicon-trash"></span>  <span class="md-action glyphicon glyphicon-refresh"></span></td>
                    <td default="${md.key}" class="md-key">${md.key}</td>
                    <td default="${md.rawValue}" class="md-value">${md.rawValue}</td>
                    <td default="${web_dc:getValueType(md.rawValue)}" class="md-type">${web_dc:getValueType(md.rawValue)}</td>
                </tr>
            </c:forEach>
        </c:if>
    </tbody>
</table>

