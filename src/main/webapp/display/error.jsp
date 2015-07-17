<%-- 
    Document   : error.jsp
    Created on : Nov 13, 2014, 5:26:15 PM
    Author     : Brian Van Klaveren<bvan@slac.stanford.edu>
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    </head>
    <body>
        
        <div class="error">
            Error: ${error.message} <br/>
            Error: ${error.type} <br/>
            Status: ${error.code} <br/>

        </div>
    </body>
</html>
