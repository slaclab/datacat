<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="java.util.logging.Level" %>
<%@page import="java.util.LinkedHashMap" %>
<%@page import="java.math.BigDecimal" %>

<%@taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@taglib uri="http://displaytag.sf.net" prefix="display" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@taglib uri="http://srs.slac.stanford.edu/time" prefix="time" %>
<%@taglib uri="http://srs.slac.stanford.edu/sql" prefix="srs_sql" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%
LinkedHashMap levelMap = new LinkedHashMap();
Level[] levels = { Level.SEVERE, Level.WARNING, Level.INFO, Level.FINE, Level.FINER, Level.FINEST };
for (Level level : levels) levelMap.put(new BigDecimal(level.intValue()),level.toString());
pageContext.setAttribute("levels",levelMap);
%>

<sql:setDataSource dataSource="jdbc/datacat-prod" />
<c:set var="contextPath" value="${pageContext.servletContext.contextPath}"/>
<html>
<head>
   <title>Data Catalog Message Viewer</title> 
   <link rel="stylesheet" type="text/css" href="${contextPath}/css/ext/legacy/default.css">

</head>
<body>

<h2>Data Catalog Message Viewer</h2>
<%-- set date object for starting and ending dates --%>
<jsp:useBean id="logStartDate" class="java.util.Date" />
<jsp:useBean id="logEndDate" class="java.util.Date" />

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
</c:if> 
<%-- If no start/end dates provided use default dates: start date = current date/time - 24 hours and end date = None --%>
<c:if test="${empty minimumDate}">
   <c:set var="minimumDate" value="${logStartDate.time-24*60*60*1000}"/>
</c:if>
<c:if test="${empty maximumDate}">
   <c:set var="maximumDate" value="-1"/>
</c:if>

<c:set var="severity" value="${param.severity}"/> 
<c:if test="${!empty clear || empty severity}">
   <c:set var="severity" value="800" /> 
</c:if>

<form name="DateForm">
<table class="filtertable">
<tr>
   <td colspan="20">
      <c:if test="${!empty param.dataset}">
         <sql:query var="ds">
            select datasetname, namepath from dataset join datasetpath using (dataset) where dataset=?
            <sql:param value="${param.dataset}"/>
         </sql:query>
         Dataset: <a href="${contextPath}/display/browser${ds.rows[0].namepath}">${ds.rows[0].namepath}</a>
      </c:if>
      Severity: <select name="severity">
         <option value="0">-</option>
         <c:forEach var="level" items="${levels}">
            <option ${severity==level.key ? 'selected' : ''} value="${level.key}">${level.value}</option>
         </c:forEach>
      </select>
   </td>
</tr>
<tr>
   <td><table><tr><td>After (UTC):</td><td><time:dateTimePicker value="${minimumDate}" size="22" name="minDate" format="%d/%b/%Y %H:%M:%S" showtime="true" timezone="UTC"/></td></table></td>
   <td><table><tr><td>Before (UTC):</td><td><time:dateTimePicker value="${maximumDate}" size="22" name="maxDate" format="%d/%b/%Y %H:%M:%S" showtime="true" timezone="UTC"/></td></table></td>
   
   <td><input type="submit" value="Filter" name="submit">&nbsp;<input type="submit" value="Default" name="clear"></td>
</tr>
</table>
</form>
<srs_sql:query  var="log" defaultSortColumn="timeentered" pageSize="500">
   select l.dclog, l.log_level, l.message, l.timeentered, d.dataset, d.datasetname, case when lf.path is not null then lf.path || '/' || d.datasetname else '' end path,
   case when l.exception is null then 0 else 1 end hasException
   from dclog l
   left outer join datasetversion dv using (datasetversion)
   left outer join verdataset d on d.dataset=dv.dataset
   left outer join datasetgroup g on (d.datasetgroup = g.datasetgroup)
   left outer join logicalfolders lf on (d.datasetlogicalfolder = lf.datasetlogicalfolder or g.datasetlogicalfolder = lf.datasetlogicalfolder)
   where l.log_level > 0
   <c:if test="${!empty severity}"> 
      and l.log_level>=?
      <srs_sql:param value="${severity}"/>
   </c:if> 
   <c:if test="${minimumDate !='-1'}">
      and l.timeentered>=?
      <jsp:setProperty name="logStartDate" property="time" value="${minimumDate}"/>   
      <srs_sql:dateParam value="${logStartDate}" type="timestamp"/>
   </c:if>
   <c:if test="${maximumDate!='-1'}"> 
      and l.timeentered<=?
      <jsp:setProperty name="logEndDate" property="time" value="${maximumDate}" /> 	
      <srs_sql:dateParam value="${logEndDate}" type="timestamp"/>
   </c:if>   
   <c:if test="${!empty param.dataset}">
      and d.dataset=?
      <srs_sql:param value="${param.dataset}"/>
   </c:if>
</srs_sql:query>

<display:table class="datatable" name="${log}" sort="external" id="row">
   <display:column property="timeentered" title="Time (UTC)" sortable="true" headerClass="sortable" decorator="org.srs.web.base.decorator.TimestampColumnDecorator"/>
   <display:column title="Level" sortable="true" headerClass="sortable" >
      ${levels[row.log_level]}
   </display:column>
   <display:column property="datasetname" title="Dataset" url="/display/browser${row.path}"/>
   <display:column property="message" title="Message" class="leftAligned" />
   <display:column title="Detail" class="leftAligned">
      <c:if test="${row.hasException==1}">
         <a href="exception.jsp?log=${row.dclog}"><img src="img/error.gif"></a>
      </c:if>
   </display:column>
</display:table>
</body>
</html>
