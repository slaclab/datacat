<%-- 
    Document   : folder
    Created on : Sep 19, 2012, 1:21:21 PM
    Author     : bvan
--%>

<%@page contentType="text/html" pageEncoding="MacRoman"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=MacRoman">
        <title>API page</title>
        
        <script src="js/jquery-1.8.1.js" type="text/javascript"></script>
        <script src="js/vkbeautify.0.99.00.beta.js" type="text/javascript"></script>
        <style type="text/css">
            .api {
                padding: 2px;
                }
            .resource {
                font-family: Verdana, Geneva, Arial, Helvetica, sans-serif;
                font-size: 12px;
                line-height: 18px;
                background-color: #F8F0DA;
                margin-left: 2px;
                padding: 2px;
            }
            .method {
                background-color: #FeFaF3;
                margin: 4px;
                padding: 4px;
            }
            .method label {
                float: left;
                width: 120px;
                text-align: right;
                margin-right: 8px;
            }
            .method span {
                width: 60px;
                text-align: left;
            }
        </style>
        <script type="text/javascript">
            var running = false;
            var baseUrl = "${initParam.restResourceBaseURL}";
            
            function loadNew(){
                var path = baseUrl + "/meta";
                var jqXHR = $.ajax({
                    url: path, dataType: "json" // Alternatively, json, if you want to specify.
                }).done(function( data, status, xhr ) {
                    running = false;
                    appendAll(jQuery.parseJSON(xhr.responseText));
                }).fail(function( xhr, status, code ) { running = false; });
            }

            function appendAll(resources){
                var api = $("#api");
                
                resources.sort(function(a,b){
                    return a.path.localeCompare(b.path);
                });
                $.each(resources, function(i, resource){
                    console.log(resource.path);
                    var rsrc = $("<div/>", {"class" : "resource"});
                    $(api).append("<h3><a href=\"" + baseUrl + resource.path + "\">" + resource.path + "</a></h3>");
                    
                    resource.methods.sort(function(a,b){
                        return a.hasOwnProperty("path") && b.hasOwnProperty("path")
                            ? a.path.localeCompare(b.path)
                        : a.hasOwnProperty("path") ? a.path.localeCompare(b) : 0;
                    });
                    

                    $.each(resource.methods, function(j, method){
                        var meth = $("<div/>", {"class" : "method"});
                        function app(label, object){
                            if( object != null ){
                                meth.append($("<label/>").text(label)).append($("<span/>").text(object)).append("<br/>");
                            }
                        };
                        var m = [];
                        app("HTTP Methods :", method.httpMethods);
                        app("Path :", method.path);
                        app("Query Params", method.queryParams);
                        app("Produces :", method.produces);
                        app("Returns :", method.returns);
                        $(rsrc).append(meth);
                    });
                    $(api).append(rsrc);
                });
            };            
            $("#api").ready(loadNew);
        </script>
        
    </head>
    <body>
        <h1>REST API</h1>
        <div id="api" class="api">
        </div>
    </body>
</html>
