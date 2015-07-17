

(function( ldStack, $, rootPath) {
    ldStack.basePath = rootPath || "http://glastlnx08.slac.stanford.edu:8180/rest/datacat/v1";
    
    var loadStack = new Array();
    ldStack.syncLoadStack = function(rsrc, path, query, cb){
	var p = new Object();
	p.path = rsrc + path + query;
	p.cb = cb;
	loadStack.push( p );
	function run(){
	    if(!running && loadStack.length > 0){
		var par = loadStack.shift();
		function finish(items){
		    if(par.cb != null){ par.cb(items); }
		    run();
		}
		ajaxResource(par.path, finish);
	    }
	}
	run();
    }

    var running = false;
    function ajaxResource(relpath, cb){
	var path = ldStack.basePath + relpath;
	var jqXHR = $.ajax({
            url: path
	}).done(function( data, status, xhr ) {
            running = false;
	    respDebug(true, xhr, status);
            if( cb != null ) {
		cb(eval(xhr.responseText));
            }
	}).fail(function( xhr, status, code ) {
            running = false;
	    respDebug(false, xhr, status);
	});
    }
    
    function respDebug(success, xhr, status){/*
	var content = "";
	if(true){
	    var ct = xhr.getResponseHeader("content-type") || "";
	    var newHtml = ct == "application/xml" ? 
		$('<div/>').text(vkbeautify.xml(xhr.responseText)).html() : ct == "application/json" ? 
		vkbeautify.json(xhr.responseText) : $('<div/>').text(xhr.responseText).html();
	    $("#content").html(newHtml);
	} else {
            if(status == "error"){
		var ct = xhr.getResponseHeader("content-type") || "";
		$("#content").text(xhr.responseText);
            } else if(status == "timeout"){
		$("#content").text("A timeout occurred.");
            }
	}*/
	}
    
}( window.loadStack = window.loadStack || {}, jQuery, pageContext.path ));


(function( dynamicTree, $) {

    dynamicTree.dataTree = {};

    /*
    var syncLoadStack = ajaxHandler;
    
    var caseSort = false;
    dynamicTree.sortFun = function(a, b){
	a = caseSort ? a.name : a.name.toLocaleLowerCase();
	b = caseSort ? b.name : b.name.toLocaleLowerCase();
	return a.localeCompare( b );
    }*/
    
    /*function groupsAndFoldersFor(path, cb){
	var query = "?datasets=false";
	syncLoadStack("/children", path, query, cb);
    }*/

    /*function rootItems(cb){
	syncLoadStack("/root", "", "", function (items){
	    items.sort(dynamicTree.sortFun);
            for(var i = 0; i < items.length; i++){
		var item = items[i];
		dynamicTree.dataTree["/" + item.name] = item;
            }
            var ul = $(".tree-root");
	    items.forEach(function(item){
		addNode(ul, item);
	    });
	})
    }
    function pathInfo(path){
	path = "/path" + path;
	syncLoadStack("/path", path, null, null);
    }
     */

    /*function childrenCB(parent){
	return function(items){
	    parent['children'] = items;
            var ul = findNode(findPath(parent)).find("> .tree-children");
	    for(var i = 0; i < items.length; i++){
		items[i]["parent"] = parent;
                addNode(ul, items[i]);
	    }
            parent.loaded = true;
	}
    }*/
    
    function loadChildren(parent, child, lvl, max){
        child["parent"] = parent;
        var ul = findNode(findPath(parent)).find("> .tree-children");

        addNode(ul, child);
        if(child.children != null && lvl < max){
            child.children.forEach(function(item){
                loadChildren(child, item, lvl+1, max);
                child.loaded = true;
            });
        } else if (child.children == null){
            child.loaded = true;
        }
    }
    
    function loadAllRootItems(items){
        var ul = $(".tree-root");
        for(var i = 0; i < items.length; i++){
            var rItem = items[i];
            dynamicTree.dataTree["/" + rItem.name] = rItem;
            addNode(ul, rItem);
            if(rItem.children != null){
                rItem.children.forEach(function(child){
                    loadChildren(rItem, child, 0, 1);
                });
            }
            rItem.loaded = true;
        }
    }
    
    function findNode(path){
	path = path.split("\/").slice(1);
        var node = dynamicTree.dataTree["/" + path[0]]
	for(var i = 1; i < path.length; i++){
            node = node.children.filter(function(item){ return item.name === path[i]; } )[0];
	}
	return node.elem;
    }
    
    function findPath(node){
	var path = "/" + node.name;
	while(node['parent'] != null){
	    node = node['parent'];
	    path = "/" + node.name + path;
	}
	return path;
    }
    
    
    function addNode(childrenContainer,item){
	var node = $('<li class="tree-node"/>').attr("id","/" + item.name);
	var nodeAnchor = $('<a href="#" data-toggle="expand" />').text(item.name).bind("click",selected).bind("keydown", toggleOpen);
	nodeAnchor.bind("contextmenu rightclick", rewriteHREF);
	nodeAnchor.bind("copy", rewriteHREF);
	if(item.$type == "folder"){
	    nodeAnchor = nodeAnchor.addClass("container-toggle").bind("dblclick",toggleOpen);
	    node.append( $('<b class="tree-caret"></b>').bind("click",toggleOpen) );
	} else {
	    nodeAnchor = nodeAnchor.addClass("tree-file");
	    node.append( $('<b class="tree-file-caret"></b>') );
	}
	
	node.append( nodeAnchor );
	if(item.$type == "folder"){
	    node.append( $('<ul class="tree-children" />') );
	}

	node.appendTo( childrenContainer );
	item.elem = node;
	node.data("treeNode",item);
    }

    /*****************************
     *  Event Handlers 
     *****************************/
    var right = 39;
    var left = 37;
    var up = 38;
    var j = 74;
    var down = 40;
    var k = 75;
    var space = 32;
    var enter = 13;
    
    var ctrlActive = false;
    function multiSet(event){
	var s = 83;
	if( event.type == "keydown" && event.keyCode == s){
	    ctrlActive = true;
	}
	if( event.type == "keyup" && event.keyCode == s){
	    ctrlActive = false;
	}
    }
    
    function dumpSelected(){
	$(".tree-selected").each(function(i,item){
	    var p = $(item).parent();
	    var tNode = p.data("treeNode");
	    console.log( "Dumping selection: " + findPath( tNode ) );
	});
    }

    function selected(event){
        console.log(event.which);
	if( event.type == "click"){
	    if(event.which == 2){
		// orig $(this).attr("href", "#" + findPath($(this).parent().data("treeNode")));
                //** new
                var node = $(this).parent().data("treeNode");
                var path2 = "#" + findPath(node);// + "&" + node.$type + "&pk=" + node.pk;
                var path = "http://srs.slac.stanford.edu/DataCatalog/" + node.$type + ".jsp?" + node.$type + "=" + node.pk + path2;
                $(this).attr("href", path);
                //** end new
	    }
	}
	if(!ctrlActive && (event.keyCode == enter || event.type == "click")){
	    dumpSelected();
	    $(".tree-selected").each(function(i,item){
		$(item).toggleClass("tree-selected");
	    });
	}
	var p = $(this);
	p.toggleClass("tree-selected");        
        return event.which == 2 ? true : false;
    }
    
    function rewriteHREF(event){
        var node = $(this).parent().data("treeNode");
        var path2 = "#" + findPath(node);// + "&" + node.$type + "&pk=" + node.pk;
        var path = "http://srs.slac.stanford.edu/DataCatalog/" + node.$type + ".jsp?" + node.$type + "=" + node.pk + path2;
	$(this).attr("href", path);
    }

    function toggleOpen(event){
	var p = $(this).parent();
	var tNode = p.data("treeNode");

	function toggle(target){
            console.log(target);
	    if(target.loaded == null && target.children != null){
                target.children.forEach(function(child){
                    loadChildren(target, child, 0, 2);
                });
	    }  else if (p.data("treeNode").loaded == null) {
		groupsAndFoldersFor(findPath(tNode), function(items){	    
		    childrenCB(tNode)(items);
		});
            }
	    p.toggleClass("open");
	    window.location.hash = "#" + findPath(tNode);
	}
	
	if( event.type == "keydown" ){
            // Only toggle when it's open/close for right/left or enter
	    if ( !p.hasClass("open") && ( event.keyCode == right || event.keyCode == enter ) )
                if( p.hasClass("open") && ( event.keyCode == left || event.keyCode == enter ) )
                    toggle(tNode);
	} else {
	    toggle(tNode);
	}
    }

    /*****************************
     *  END Event Handlers 
     *****************************/
    
    var rawItems = null;
    dynamicTree.init = function(items, cached){
        rawItems = items;
	$(".tree").ready(    function (){
            $(".tree").bind("keydown",multiSet);
            $(".tree").bind("keyup",multiSet);
            loadAllRootItems(rawItems);
        });
    }

}( window.dynamicTree = window.dynamicTree || {}, jQuery, window.loadStack.syncLoadStack ));

window.dynamicTree.init([{"$type":"folder","name":"LSST", "path":"/LSST"}], null);

