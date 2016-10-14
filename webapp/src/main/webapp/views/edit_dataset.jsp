
<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@taglib uri="http://displaytag.sf.net" prefix="display" %>
<%@taglib uri="http://srs.slac.stanford.edu/GroupManager" prefix="gm" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib uri="http://srs.slac.stanford.edu/web_datacat" prefix="web_dc" %>


<c:forEach var="location" items="${model.target.viewInfo.locations}" varStatus="status">
    <c:if test="${location.isMaster().booleanValue()}">
        <c:set var="master" value="${location}" />
    </c:if>
</c:forEach>


<script>
    $("document").ready(function () {
        $(".location-table td.ds-patchable").editableTableWidget();

        $('.location-table').on('validate', function (evt, newValue) {
            var target = $(evt.target);
            var row = target.parent();
            var original = row.data("original").toString();
            if (newValue !== original) {
                row.addClass("warning");
            } else {
                row.removeClass("warning");
            }
        });

        $(".edit-submit").on("click", function () {
            var form = $("#edit-form");
            form.attr("action", pageContext.endPoint + pageContext.target.path);

            $(".ds-patchable").each(function (i, item) {
                item = $(item);
                var row = item.parent();
                if (item.text() !== row.data("original").toString()) {
                    var input = $('<input type="hidden" value=""/>');
                    input.attr("name", row.data("key"))
                    input.val(item.text())
                    form.append(input);
                }
            });
            // Gather all the items, create a form, and submit it.
            var mdItems = mdhandler.getData();
            var mdInput = $('<input type="hidden" id="md-form-data" name="versionMetadata" value=""/>');
            form.append(mdInput);
            $("#md-form-data").val(JSON.stringify(mdItems));
            $("#edit-form").submit();
        });
    });
</script>

<div class="datacat-component">
    <div class="datacat-header">

        <h3>Editing Dataset ${model.target.name}
            <br>
            <small>Version ${model.target.versionId}</small><br>
        </h3>
    </div>

    <table class="table table-condensed table-striped location-table">
        <thead>
            <tr>
                <th>Name</th>
                <th>Value</th>
            </tr>
        </thead>
        <tbody>
            <c:catch var="exception">
                <tr><th>Created (UTC):</th><td class="edit-patchable">${web_dc:formatTimestamp(model.target.dateCreated)}</td></tr>
                    </c:catch>

            <tr><th>File Format:</th><td>${model.target.fileFormat}</td></tr>
            <tr><th>Data Type:</th><td>${model.target.dataType}</td></tr>

            <c:catch var="exception">
                <c:if test="${model.target.dataSource != null}">
                    <tr><th>Source:</th><td>${model.target.dataSource}</td></tr>
                        </c:if>
                    </c:catch>
                    <c:if test="${master != null}">
                <tr data-key="size" data-original="${web_dc:formatBytes(master.size)}">
                    <th>Size:</th>
                    <td class="ds-patchable" id="created">${web_dc:formatBytes(master.size)}</td></tr>
                <tr><th>Master Site:</th><td>${master.site}</td></tr>
                <tr>
                    <th>Master resource:</th>
                    <td class="location-resource">${master.resource}</td></tr>
                <tr data-key="runMin" data-original="${master.runMin}">
                    <th>Run Min:</th>
                    <td class="ds-patchable">${master.runMin}</td></tr>
                <tr data-key="runMax" data-original="${master.runMax}">
                    <th>Run Max:</th>
                    <td class="ds-patchable">${master.runMax}</td></tr>
                <tr data-key="eventCount" data-original="${web_dc:formatEvents(master.eventCount)}">
                    <th>Events:</th>
                    <td class="ds-patchable">${web_dc:formatEvents(master.eventCount)}</td>
                </tr>
            </c:if>
        </tbody>
    </table>

    <h3>Version Metadata</h3>
    <c:set var="mdlist" value="${model.target.versionMetadata}" />
    <%@ include file="../views/edit_metadata.jsp" %>

    <button type="button" class="btn btn-success btn-med edit-submit">
        <span class="glyphicon glyphicon-save" aria-hidden="true"></span> Submit
    </button>

    <c:catch var="exception">
        <h3>Locations</h3>
        <c:choose>
            <c:when test="${model.target.viewInfo.locations != null && !empty model.target.viewInfo.locations}">
                <table class="table table-condensed table-striped location-table">
                    <thead>
                        <tr>
                            <th class="location-site">Site</th>
                            <th class="location-status">Scan Status</th>    
                            <th class="location-ts">Created</th>
                            <th class="location-ts">Last Scanned</th>
                            <th class="location-resource"><div class="location-resource">Resource</div></th>
                        </tr>
                    </thead>

                    <tbody>
                        <c:forEach var="location" items="${model.target.viewInfo.locations}" varStatus="status">
                            <tr>
                                <td >${location.site}</td>
                                <td>${location.scanStatus}</td>
                                <td>${web_dc:formatTimestamp(location.dateCreated)}</td>
                                <td>${web_dc:formatTimestamp(location.dateScanned)}</td>
                                <td class="location-resource">${location.resource}</td>
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

<form style="display: hidden" id="edit-form" method="POST">
    <input type="hidden" name="_referer" value="${header.referer}"/>
</form>