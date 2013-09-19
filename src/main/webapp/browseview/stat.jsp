<%-- 
    Document   : stat
    Created on : Sep 12, 2013, 12:17:23 PM
    Author     : bvan
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="http://srs.slac.stanford.edu/web_datacat" prefix="web_dc" %>

<div class="datacat-container-info">
    <div class="panel panel-info">
        <div class="panel-heading stat-heading">
            Info for <span id="stat-container-type">${selected.type}</span> 
            <span id="stat-location">${selected.path} / ${selected.name}</span>
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
                    <td>${web_dc:formatEvents(selected.stat.datasetCount)}</td>
                    <td>${web_dc:formatBytes(selected.stat.diskUsageBytes)}</td>
                    <td>${web_dc:formatEvents(selected.stat.eventCount)}</td>
                </tr>
            </tbody>
        </table>
    </div>
</div>

