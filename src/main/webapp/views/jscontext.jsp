<%-- 
    Document   : jscontext
    Created on : Jul 17, 2015, 4:30:42 PM
    Author     : bvan
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>

<script>
    var pageContext = new Object();
    pageContext.endPoint = "${endPoint}";
    pageContext.applicationBase = "${applicationBase}";
    pageContext.target = new Object();
    pageContext.target.name = "${target.name}";
    pageContext.target.path = "${target.path}";
    pageContext.target.type = "${target.type}";
</script>
