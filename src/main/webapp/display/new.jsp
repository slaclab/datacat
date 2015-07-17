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
        <script src="${pageContext.request.contextPath}/js/dataTables.bootstrap.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/vkbeautify.0.99.00.beta.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js/dynamicTree2.js" type="text/javascript"></script>
        <script src="http://ajax.googleapis.com/ajax/libs/webfont/1/webfont.js" type="text/javascript"></script>
    </head>
    <body>

        <div class="row">
            <%@ include file="../views/breadcrumb.jsp" %>
        </div>

        <c:choose>
            <c:when test='${type eq "dataset"}'>
                <form class="form-horizontal">
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

                        <!-- Text input-->
                        <div class="form-group">
                            <label class="col-md-4 control-label" for="name">Version Number</label>  
                            <div class="col-md-5">
                                <input id="version" name="version" type="text" placeholder="1" class="form-control input-large">
                                <p class="help-block">Automatically Assigned if omitted</p>
                            </div>
                        </div>

                    </fieldset>
                </form>

            </c:when>
            <c:otherwise>
                <form class="form-horizontal">
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
                </form>
            </c:otherwise>

        </c:choose>
    </body>
</html>
