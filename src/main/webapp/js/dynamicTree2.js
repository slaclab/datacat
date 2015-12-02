(function( ldStack, $, rootPath) {
    ldStack.basePath = rootPath || pageContext.contextPath + "/r";
    
    ldStack.lock = false;
    var loadStack = new Array();
    ldStack.syncLoadStack = function(rsrc, path, query, cb){
	(function(){
	    var p = new Object();
	    p.path = rsrc + path + query;
	    p.cb = cb;
	    loadStack.push( p );
	    function run(){
		if(!running && loadStack.length > 0 && !ldStack.lock){
		    var par = loadStack.shift();
		    function finish(items){
			if(par.cb !== null){ par.cb(items); }
			run();
		    }
		    ajaxResource(par.path, finish);
		}
	    }
	    run();
	})();
    };

    var running = false;
    function ajaxResource(relpath, cb){
	var path = ldStack.basePath + relpath;
	$.ajax({
            url: path
	}).done(function( data, status, xhr ) {
            running = false;
            if( cb !== null ) {
		cb(eval(xhr.responseText));
            }
	}).fail(function( xhr, status, code ) {
            running = false;
	});
    }
    
}( window.loadStack = window.loadStack || {}, jQuery, pageContext.contextPath + "/r"));


(function( dynamicTree, $, ajaxHandler) {
    dynamicTree.dataTree = {};
    var caseSort = false;
    var syncLoadStack = ajaxHandler;
    
    dynamicTree.sortFun = function(a, b){
	a = caseSort ? a.name : a.name.toLocaleLowerCase();
	b = caseSort ? b.name : b.name.toLocaleLowerCase();
	return a.localeCompare( b );
    };
    
    function groupsAndFoldersFor(path, cb){
	syncLoadStack("/path.json", path + ";children=containers", "", cb);
    }
    

    var loadPaths = new Array();
    
    function rootItems(){

        var path = "";
        pageContext.target.path.split("\/").splice(1).forEach(function(item){
            path = path + "/" + item;
            loadPaths.push(path);
        });
        
	syncLoadStack("/path.json", "/;children", "", function (items){
	    items.sort(dynamicTree.sortFun);
            for(var i = 0; i < items.length; i++){
		var item = items[i];
		dynamicTree.dataTree["/" + item.name] = item;
            }
            var ul = $(".tree-root");
	    items.forEach(function(item){
		addNode(ul, item);
	    });
	});
    }
    
    function findNode(path){
	path = path.split("\/").slice(1);
	var node = dynamicTree.dataTree["/" + path[0]];
	for(var i = 1; i < path.length; i++){
	    node = node["/" + path[i]];
	}
	return node.elem;
    }
    
    function findPath(node){
	var path = "/" + node.name;
	while(node['parent'] !== undefined){
	    node = node['parent'];
	    path = "/" + node.name + path;
	}
	return path;
    }

    dynamicTree.openRecursive = function(path){
	path = path.split("\/").slice(1);
	for(var ixx = 0; ixx < path.length; ixx++){
	    (function(){
		var npath ="/" + path.slice(0,ixx+1).join("/") + ";children=containers";
		var query = "";
		var cb = function(items){
		    childrenCB(npath)(items);
		    findNode(npath).toggleClass("open");
		    syncLoadStack.lock = false;
		};
		syncLoadStack.wait = true;
		syncLoadStack("/path.json", npath, query, cb);
	    })();
	}

    };
        
    function loadContainer(evt){
        $("#info-views .datacat-component").remove();
        $("#info-views").append($("<h3>Loading...</h3>"));
        var target = $(evt.target);
        var ajaxHref = target.data("ajax-href");
        window.history.pushState(null, null, pageContext.contextPath + "/display/tree" + target.data("path"));
        changeTarget(target.data("path"));
        $.ajax({
            url: ajaxHref
	}).done(function( data, status, xhr ) {
            $("#info-views h3").remove();
            $(data).appendTo($("#info-views"));
	});
    }
    
    function addNode(childrenContainer,item){
	var node = $('<li class="tree-node"/>').attr("id","/" + item.name);
        var href = pageContext.applicationBase + "/ajax/container" + item.path;
	var nodeAnchor = $('<a data-toggle="expand" />')
                //.attr("href", "#")
                .data("ajax-href", href)
                .data("path", item.path)
                .text(item.name)
                .bind("click", loadContainer)
                .bind("keydown", toggleOpen);
	if(item._type === "folder"){
	    node.append( $('<span class="glyphicon tree-caret"></span>').bind("click", toggleOpen) );
	} else {
	    nodeAnchor = nodeAnchor.addClass("tree-file");
	    node.append( $('<span class="glyphicon tree-file-caret"></span>') );
	}
	
	node.append( nodeAnchor );
	if(item._type === "folder"){
	    node.append( $('<ul class="tree-children" />') );
	}
	
	node.appendTo( childrenContainer );
	item.elem = node;
	node.data("treeNode",item);
        if(loadPaths.indexOf(item.path) >= 0){
            $(node).find("span").click();
        }
    }

    function childrenCB(parentPath){

	return function(items){
	    var parent = findNode(parentPath).data("treeNode");
	    items.sort(dynamicTree.sortFun);
	    parent["children"] = items;
	    for(var i = 0; i < items.length; i++){
		var item = items[i];
		parent["/" + item.name] = item;
		item["parent"] = parent;
	    }
            var ul = findNode(findPath(parent)).find(".tree-children");
	    parent.children.forEach(function(item){
		addNode(ul, item);
            });
	};
    }

    /* Event Handlers */
    var right = 39;
    var left = 37;
    var up = 38;
    var j = 74;
    var down = 40;
    var k = 75;
    var space = 32;
    var enter = 13;
       
    var openStack = new Array();
    
    function toggleOpen(event){
	
	var p = $(event.target).parent();
	var tNode = p.data("treeNode");
        
	function toggle(){
	    if(p.data("treeNode").children === undefined){
		var path = findPath(tNode);
		groupsAndFoldersFor(path, function(items){
		    childrenCB(path)(items);
		});
	    }
	    if(p.hasClass("open")) {
		openStack.splice(openStack.indexOf( p.data("treeNode").pk), 1 );
	    } else {
		openStack.push(	p.data("treeNode").pk );
	    }
	    p.toggleClass("open");
	}
	
	if( event.type === "keydown" ){
	    if ( !p.hasClass("open") && ( event.keyCode === right || event.keyCode === enter ) )
		toggle();
	    else if( p.hasClass("open") && ( event.keyCode === left || event.keyCode === enter ) ){
		toggle();
            }
	} else {
	    toggle();
	}
    }
    
    dynamicTree.init = function(){
        if($(".tree") !== null){
            $(".tree").ready(rootItems);
        }
    };
    
}( window.dynamicTree = window.dynamicTree || {}, jQuery, window.loadStack.syncLoadStack ));

window.dynamicTree.init();
