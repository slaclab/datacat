<%-- 
    Document   : folder
    Created on : Sep 19, 2012, 1:21:21 PM
    Author     : bvan
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=MacRoman">
        <title>JSP Page</title>

        <script src="jquery-1.8.1.js" type="text/javascript"></script>
        <script src="vkbeautify.0.99.00.beta.js" type="text/javascript"></script>
        <script type="text/javascript">
            
            var baseUrl = "${initParam.restResourceBaseURL}";
            var running = false;
            function loadNew(path){
                var jqXHR = $.ajax({
                    url: path//,dataType: "json" // Alternatively, json, if you want to specify.
                }).done(function( data, status, xhr ) {
                    running = false;
                    var ct = xhr.getResponseHeader("content-type") || "";
                    $("#query").append("<br/>" + ct + "<br/>");
                    $("#query").append("<br/>" + status + "<br/>");
                    
                    var newHtml = ct == "application/xml" ? 
                        $('<div/>').text(vkbeautify.xml(xhr.responseText)).html() : ct == "application/json" ? 
                        vkbeautify.json(xhr.responseText) : $('<div/>').text(xhr.responseText).html();
                    $("#content").html(newHtml);
                }).fail(function( xhr, status, code ) {
                    running = false;
                    if(status == "error"){
                        var ct = xhr.getResponseHeader("content-type") || "";
                        $("#query").append("<br/>" + ct + "<br/>");
                        $("#query").append("<br/>" + status + "<br/>");
                        $("#query").append("<br/>" + code + "<br/>");
                        $("#content").text(xhr.responseText);
                    } else if(status == "timeout"){
                        $("#content").text("A timeout occurred.");
                    }
                });
            }
            
            function hashChanged(){
                var s = new String(window.location.hash);
                if(s.substr("#path").length > 5){
                    var restPath = s.slice(5);
                    running = true;
                    $("#content").text("Your query is running ...");
                    $("#query").text(restPath);
                    var path = baseUrl + "/children" + restPath;
                    loadNew(path);
                }
            };
            
            window.onhashchange = hashChanged;
            
            // path box
            $(function() {
                function run(){
                    if(!running){
                        running = true;
                        $("#content").text("Your query is running ...");
                        var restPath = $('#path').val();
                        var path = baseUrl + "/children" + restPath;
                        window.location.hash = "#path" + restPath;
                        $("#query").text($('#path').val());
                        loadNew(path);
                    }
                }
                
                $("#path").keyup(function(event){
                    if(event.keyCode == 13){
                        $("#path").dblclick();
                    }
                });
                
                $( "#path" ).bind( "dblclick", run );
                
                // once the path element is ready, assume page was newly loaded
                // and check the hash
                $("#path").ready(hashChanged);
            });
            
            // Filter stuff
            $(function() {
                function enc(){
                    $("#encodedFilter").text("&filter=" + encodeURIComponent($('#filter').val()));
                }
                $("#filter").keyup(function(event){
                    if(event.keyCode == 13){
                        $("#filter").dblclick();
                    }
                });
                $( "#filter" ).bind( "dblclick", enc );
            });
        </script>

    </head>
    <body>
        <div class="control">
            <b>Path:</b>
            <input id="path" type="text" size="160"> <br/>
            <b>Datacatalog filter:</b>
            <input id="filter" type="text" size="160">
            <div id="encodedFilter">

            </div>
        </div>
        <br/>
        <div id="query">

        </div>
        <br/>        
        <pre class="content" id="content">

        </pre>
    </body>
</html>
