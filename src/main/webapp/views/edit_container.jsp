<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@taglib uri="http://displaytag.sf.net" prefix="display" %>
<%@taglib uri="http://srs.slac.stanford.edu/GroupManager" prefix="gm" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib uri="http://srs.slac.stanford.edu/web_datacat" prefix="web_dc" %>

<div class="datacat-component">
    <div class="datacat-header clearfix">
        <a href="${applicationBase}/browser${target.path}" type="button" class="btn btn-default btn-med pull-right">
            <span class="glyphicon glyphicon-th-list" aria-hidden="true"></span> Browser
        </a>
        <div class="pull-left">
            <h3>Editing ${target.type} ${target.name} </h3>
        </div>
    </div>

    <div class="clearfix">
        <form class="form-horizontal" method="POST">
            <fieldset>
                <!-- Form Name -->
                <legend>Edit Container Attributes</legend>

                <div class="control-group">
                    <label class="control-label" for="description">Description</label>
                    <div class="controls">
                        <input id="description" name="description" type="text" placeholder="${target.description}" class="input-xlarge">
                    </div>
                </div>

            </fieldset>
        </form>
    </div>

    <table class="table table-condensed table-striped location-table">
        <!-- <small>${target.description}</small> -->
        <thead>
            <tr>
                <th>Name</th>
                <th>Value</th>
            </tr>
        </thead>
        <tbody>
            <%-- TODO: Handle Create dates
            <th>Created (UTC):</th><td>${web_dc:formatTimestamp(target.created)}</td></tr>
            --%>
            <tr><th>Datasets</th><td>${web_dc:formatEvents(target.stat.datasetCount)}</td></tr>
            <tr><th>Total Size</th><td>${web_dc:formatBytes(target.stat.diskUsageBytes)}</td></tr>
            <tr><th>Events</th><td>${web_dc:formatEvents(target.stat.eventCount)}</td></tr>
        </tbody>
    </table>

    <div class="clearfix">
        <h3>Metadata</h3>
    </div>
    <div class="clearfix">
        <button type="button" class="btn btn-success btn-med">
            <span class="glyphicon glyphicon-plus" aria-hidden="true"></span> Add
        </button>
        <button type="button" class="btn btn-danger btn-med">
            <span class="glyphicon glyphicon-minus" aria-hidden="true"></span> Remove
        </button>
    </div>


    <table class="table table-condensed table-striped table-hover">
        <thead>
            <tr>
                <th>Key</th>
                <th>Value</th>
                <th>Type</th>
            </tr>
        </thead>

        <tbody id="metadata">
            <c:forEach var="md" items="${target.metadata}" varStatus="status">
                <tr><td>${md.key}</td><td>${md.rawValue}</td>
                    <td>${web_dc:getValueType(md.rawValue)}</td>
                </tr>
            </c:forEach>
        </tbody>
    </table>

</div>

