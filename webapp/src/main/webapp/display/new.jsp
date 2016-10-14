<%-- 
    Document   : browser.jsp
    Created on : Sep 19, 2012, 1:21:21 PM
    Author     : bvan
--%>
<%@taglib uri="http://srs.slac.stanford.edu/web_datacat" prefix="web_dc" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html lang="en">
    <head>  
        <%@ include file="../views/jscontext.jsp" %>

        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/browser.css">
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/bootstrap.css">
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/dataTables.bootstrap.css">
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/tree.css">
        <script src="${pageContext.request.contextPath}/js/jquery-1.11.3.min.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/URI.min.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/bootstrap.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/browser.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/jquery.dataTables.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/mdedit.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/mindmup-editabletable.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/dataTables.bootstrap.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/dynamicTree2.js" type="text/javascript"></script>
        <script src="http://ajax.googleapis.com/ajax/libs/webfont/1/webfont.js" type="text/javascript"></script>
    </head>
    <body>

        <script>
            $("document").ready(function () {
                $(".new-submit").on("click", function () {
                    var form = $("#new-form");
                    form.attr("action", pageContext.endPoint + pageContext.target.path);
                    var mdItems = mdhandler.getData();
                    var mdInput = $('<input type="hidden" id="md-form-data" name="versionMetadata" value=""/>');
                    form.append(mdInput);
                    $("#md-form-data").val(JSON.stringify(mdItems));
                    $("#new-form").submit();
                });
            });
        </script>

        <div class="row">
            <%@ include file="../views/breadcrumb.jsp" %>
        </div>

        <c:choose>
            <c:when test='${param.type eq "dataset"}'>
                <form class="form-horizontal" id="new-form" method="POST">
                    <fieldset>

                        <!-- Form Name -->
                        <legend>Create new Dataset</legend>

                        <!-- Text input-->
                        <div class="form-group">
                            <label class="col-md-4 control-label" for="name">Dataset Name</label>  
                            <div class="col-md-5">
                                <input id="name" name="name" type="text" placeholder="dataset0001.dat" class="form-control input-xlarge" required="">

                            </div>
                        </div>

                        <!-- Text input-->
                        <div class="form-group">
                            <label class="col-md-4 control-label" for="name">Data Type</label>  
                            <div class="col-md-5">
                                <input id="dataType" name="dataType" type="text" placeholder="FT1, MERIT, MC" class="form-control input-xlarge">

                            </div>
                        </div>

                        <!-- Text input-->
                        <div class="form-group">
                            <label class="col-md-4 control-label" for="name">File Format</label>  
                            <div class="col-md-5">
                                <input id="fileFormat" name="fileFormat" type="text" placeholder=".fits, .tar.gz, .dat" class="form-control input-xlarge">

                            </div>
                        </div>

                        <!-- Text input-->
                        <div class="form-group">
                            <label class="col-md-4 control-label" for="name">Site</label>  
                            <div class="col-md-5">
                                <input id="site" name="site" type="text" placeholder="SLAC, SLAC_XROOT, BNL" class="form-control input-xlarge" required="">

                            </div>
                        </div>

                        <!-- Text input-->
                        <div class="form-group">
                            <label class="col-md-4 control-label" for="name">Resource path</label>  
                            <div class="col-md-6">
                                <input id="resource" name="resource" type="text" placeholder="/nfs/slac/g/dataset0001.dat, root://slac.stanford.edu//glast2/dataset0001.dat" class="form-control input-xxlarge" required="">

                            </div>
                        </div>

                    </fieldset>
                    <h3>Metadata</h3>
                    <c:set var="mdlist" value="${null}" />
                    <%@ include file="../views/edit_metadata.jsp" %>

                </form>

            </c:when>
            <c:otherwise>
                <form class="form-horizontal" action="submit" method="POST">
                    <fieldset>

                        <!-- Form Name -->
                        <legend>Create new Container</legend>

                        <!-- Text input-->
                        <div class="form-group">
                            <label class="col-md-4 control-label" for="name">Container Name</label>  
                            <div class="col-md-5">
                                <input id="name" name="name" type="text" placeholder="Runs" class="form-control input-md" required="">

                            </div>
                        </div>

                        <!-- Select Basic -->
                        <div class="form-group">
                            <label class="col-md-4 control-label" for="type">Container Type</label>
                            <div class="col-md-4">
                                <select id="type" name="type" class="form-control">
                                    <option value="Folder">Folder</option>
                                    <option value="Group">Group</option>
                                </select>
                            </div>
                        </div>

                        <!-- Text input-->
                        <div class="form-group">
                            <label class="col-md-4 control-label" for="description">Description</label>  
                            <div class="col-md-5">
                                <input id="description" name="description" type="text" placeholder="Runs for the XYZ experiment" class="form-control input-md">

                            </div>
                        </div>

                    </fieldset>
                    <h3>Version Metadata</h3>
                    <c:set var="mdlist" value="${null}" />
                    <%@ include file="../views/edit_metadata.jsp" %>

                    <input type="hidden" name="_referer" value="${header.referer}"/>
                </form>
            </c:otherwise>

        </c:choose>
        
        <button type="button" class="btn btn-success btn-med new-submit">
            <span class="glyphicon glyphicon-save" aria-hidden="true"></span> Submit
        </button>

    </body>
</html>
