<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://www.servletsuite.comf/servlets/jmxtag" prefix="jmx"%>
<%@taglib uri="http://srs.slac.stanford.edu/jmx" prefix="bean"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@taglib uri="http://srs.slac.stanford.edu/GroupManager" prefix="gm" %>
<%@taglib prefix="sql" uri="http://java.sun.com/jsp/jstl/sql"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<html>
    <sql:setDataSource dataSource="jdbc/datacat-prod" />
    <c:set var="contextPath" value="${pageContext.servletContext.contextPath}"/>

   <head>
      <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
      <title>Data Crawler JMX Admin</title>
      <style type="text/css">
         table.mbeanAttributeTable {
         background:#5ff;
         }
         table.mbeanAttributeTable thead {
         background:#5cc;
         }
         table.mbeanOperationTable {
         background:#5ff;
         }
      </style>
      <link rel="stylesheet" type="text/css" href="${contextPath}/css/ext/legacy/default.css">

   </head>
   <body>
      
      <h1>Data Crawler Admin</h1>
      <sql:query var="result">
         select * from GenericServerStatus where server='CRAWLER'
      </sql:query>
      <c:choose> 
         <c:when test="${result.rowCount==0}">
            Data Crawler is not running.
         </c:when>
         <c:otherwise>
            Data Crawler version ${result.rows[0].version} running on ${result.rows[0].host} since ${result.rows[0].started}
            <bean:JMXConnect var="server" serverURL="service:jmx:rmi:///jndi/rmi://${result.rows[0].host}:${result.rows[0].port}/jmxrmi">
               
               <h2>Control</h2>
               <bean:mbeanAttributesTable connection="${server}" mbean="org.srs.datacat.server:type=Main" updateable="false"/>
               <h2>Executors</h2>
               <jmx:forEachMBean connection="${server}" id="bean" pattern="org.srs.datacat.server:type=ThreadPoolExecutor,name=*">
                  <h3>${fn:substringAfter(bean,"name=")}</h3>
                  <bean:mbeanAttributesTable connection="${server}" mbean="${bean}" updateable="false"/> 
               </jmx:forEachMBean>  
               
               <h2>Crawlers</h2>
               <jmx:forEachMBean connection="${server}" id="bean" pattern="org.srs.datacat.server:type=SQLSchedulerMBean,name=*">
                  <h3>${fn:substringAfter(bean,"name=")}</h3>
                  <bean:mbeanAttributesTable connection="${server}" mbean="${bean}" updateable="false"/>            
               </jmx:forEachMBean>   
               
               <h2>Logger</h2>
               <bean:mbeanAttributesTable connection="${server}" mbean="org.srs.datacat.server.logger:type=JDBCHandler"/>
            </bean:JMXConnect>
         </c:otherwise>
      </c:choose>
   </body>
</html>
