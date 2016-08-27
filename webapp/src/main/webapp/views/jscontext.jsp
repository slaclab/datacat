<%-- 
    Document   : jscontext
    Created on : Jul 17, 2015, 4:30:42 PM
    Author     : bvan
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>

<script>
    var pageContext = new Object();
    pageContext.endPoint = "${model.endPoint}";
    pageContext.applicationBase = "${model.applicationBase}";
    pageContext.contextPath = "${model.contextPath}";
    pageContext.target = new Object();
    pageContext.target.name = "${model.target.name}";
    pageContext.target.path = "${model.target.path}";
    pageContext.target.type = "${model.target.type}";
</script>
