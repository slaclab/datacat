// main.js
var client = new ZeroClipboard( document.getElementById("copy-button") );

client.on( "ready", function( readyEvent ) {
  // alert( "ZeroClipboard SWF is ready!" );

  client.on( "aftercopy", function( event ) {
    // event.target.style.display = "none";
    //alert("Copied text to clipboard: " + event.data["text/plain"] );
  } );
} );


function changeTarget(path){
    $("#copy-button").attr("text", path).attr("data-clipboard-text", path);
    var pathList = path.split(/\//).splice(1);
    var target = $(".breadcrumb");
    var root = target.find("li").first();
    target.find("li").remove();
    root.appendTo(target);
    var pathPart = "";
    for(var i = 0; i < pathList.length; i++){
        var pathElem = pathList[i];
        var li = $("<li/>");
        if(i === pathList.length - 1){
            li.toggleClass("active");
            li.text(pathElem);
        } else {
            var href = pageContext.endPoint + pathPart + "/" + pathElem;
            $('<a/>').attr("href", href).text(pathElem).appendTo(li);
        }
        console.log(li);
        li.appendTo(target);
        pathPart = pathPart + "/" + pathElem;
    }
}
