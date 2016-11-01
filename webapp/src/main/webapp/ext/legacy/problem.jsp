<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql" %>
<%@taglib uri="http://displaytag.sf.net" prefix="display" %>
<%@taglib uri="http://srs.slac.stanford.edu/GroupManager" prefix="gm" %>
<%@taglib uri="http://srs.slac.stanford.edu/time" prefix="time" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<sql:setDataSource dataSource="jdbc/datacat-prod" />
<c:set var="contextPath" value="${pageContext.servletContext.contextPath}"/>
<html>
    <head>
        <title>Problem</title>
        <link rel="stylesheet" type="text/css" href="${contextPath}/css/ext/legacy/default.css">
    </head>

    <body>
        <h2>Problem File Summary</h2>
        <%-- set date object for starting and ending dates --%>
        <jsp:useBean id="startDate" class="java.util.Date" />
        <jsp:useBean id="endDate" class="java.util.Date" />

        <c:if test="${!empty param.dateSelection}">
            <c:set var="dataSelection" value="${param.DateSelection}"/>
        </c:if>
        <c:if test="${!empty param.scanStatus}">
            <c:set var="scanStatus" value="${param.scanStatus}"/>
        </c:if>
        <c:if test="${!empty param.minDate}">
            <c:set var="minimumDate" value="${param.minDate=='None' ? -1 : param.minDate}"/>
        </c:if>
        <c:if test="${!empty param.maxDate}">
            <c:set var="maximumDate" value="${param.maxDate=='None' ? -1 : param.maxDate}"/>
        </c:if>
        <%-- If clear button selected set start and end dates to default values --%>
        <c:set var="clear" value="${param.clear}" />
        <c:if test= "${clear =='Default'}">
            <c:set var="minimumDate" value=""/>
            <c:set var="maximumDate" value=""/>
            <c:set var="dataSelection" value=""/>
            <c:set var="scanStatus" value=""/>
        </c:if>
        <%-- If no start/end dates provided use default dates: start date = current date/time - 24 hours and end date = None --%>
        <c:if test="${empty minimumDate}">
            <c:set var="minimumDate" value="${startDate.time-7*24*60*60*1000}"/>
        </c:if>
        <c:if test="${empty maximumDate}">
            <c:set var="maximumDate" value="-1"/>
        </c:if>
        <c:if test="${empty dateSelection}">
            <c:set var="dateSelection" value="lastscanned"/>
        </c:if>
        <c:if test="${empty scanStatus}">
            <c:set var="scanStatus" value="scanstatus in ('MISSING','ContentError','HeaderError')"/>
        </c:if>

        <sql:query var="problems" dataSource="jdbc/datacat-prod">
            select l.namepath,s.* from (
            select (case when d.datasetlogicalfolder is null then g.datasetlogicalfolder else d.datasetlogicalfolder end) logicalfolder, datasetgroup, g.name, scanstatus,count(*), datasetsite
            from verdatasetlocation l
            join datasetversion dv using (datasetversion)
            join verdataset d using (dataset)
            left outer join datasetgroup g using (datasetgroup)
            where ${scanStatus}
            <c:if test="${minimumDate !='-1'}">
                and ${dateSelection}>=?
                <jsp:setProperty name="startDate" property="time" value="${minimumDate}"/>
                <sql:dateParam value="${startDate}" type="timestamp"/>
            </c:if>
            <c:if test="${maximumDate!='-1'}">
                and ${dateSelection}<=?
                <jsp:setProperty name="endDate" property="time" value="${maximumDate}" />
                <sql:dateParam value="${endDate}" type="timestamp"/>
            </c:if>
            group by d.datasetlogicalfolder, g.datasetlogicalfolder,datasetgroup,g.name, scanstatus, datasetsite) s
            left outer join logicalfolderpath l on (l.logicalfolder=s.logicalfolder)
        </sql:query>


        <form name="DateForm">
            <table class="filtertable">
                <tr>
                    <td colspan="20">
                        Status: <select name="scanStatus">
                            <option ${scanStatus=="scanstatus='MISSING'"                                    ? 'selected' : ''} value="scanstatus='MISSING'">MISSING</option>
                            <option ${scanStatus=="scanstatus='ContentError'"                               ? 'selected' : ''} value="scanstatus='ContentError'">ContentError</option>
                            <option ${scanStatus=="scanstatus='HeaderError'"                                ? 'selected' : ''} value="scanstatus='HeaderError'">HeaderError</option>
                            <option ${scanStatus=="scanstatus in ('MISSING','ContentError','HeaderError')"  ? 'selected' : ''} value="scanstatus in ('MISSING','ContentError','HeaderError')">(MISSING,ContentError,HeaderError)</option>
                            <option ${scanStatus=="scanstatus<>'OK'"                                        ? 'selected' : ''} value="scanstatus<>'OK'">not OK</option>
                        </select>
                    </td>
                </tr>
                <tr>
                    <td>
                        <select name="dateSelection">
                            <option ${dateSelection=='lastscanned'  ? 'selected' : ''} value="lastscanned" >Scanned</option>
                            <option ${dateSelection=='registered'   ? 'selected' : ''} value="registered"  >Registered</option>
                            <option ${dateSelection=='lastmodified' ? 'selected' : ''} value="lastmodified">Modified</option>
                        </select>
                    </td>
                    <td><table><tr><td>After (UTC):</td><td><time:dateTimePicker value="${minimumDate}" size="22" name="minDate" format="%d/%b/%Y %H:%M:%S" showtime="true" timezone="UTC"/></td></table></td>
                    <td><table><tr><td>Before (UTC):</td><td><time:dateTimePicker value="${maximumDate}" size="22" name="maxDate" format="%d/%b/%Y %H:%M:%S" showtime="true" timezone="UTC"/></td></table></td>
                    <td><input type="submit" value="Filter" name="submit">&nbsp;<input type="submit" value="Default" name="clear"></td>
                </tr>
            </table>
        </form>

        <display:table class="datatable" id="row" name="${problems.rows}" defaultsort="1" defaultorder="ascending">
            <display:column property="namepath" title="Folder" class="leftAligned" sortable="true" headerClass="sortable" href="${contextPath}/display/datasets${row.namepath}"/>
            <display:column property="name" title="Group" sortable="true" headerClass="sortable" href="group.jsp" paramId="group" paramProperty="datasetgroup"/>
            <display:column property="SCANSTATUS" title="Status" sortable="true" headerClass="sortable"/>
            <display:column property="datasetsite" title="Site" sortable="true" headerClass="sortable"/>
            <display:column title="Count" sortProperty="COUNT(*)" sortable="true" headerClass="sortable">
                <a href='${contextPath}/display/datasets${row.namepath}?filter=scanStatus+=+"${row.SCANSTATUS}"+and+site+=+"${row.datasetsite}"'>${row["COUNT(*)"]}</a>
            </display:column>
        </display:table>
    </body>
</html>
