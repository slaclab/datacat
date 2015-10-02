
var mdhandler = new Object();
mdhandler.addrow = function(){
    var newRow = $('\
            <tr class="md-new success">\
                <td><span class="md-action glyphicon glyphicon-trash"></span></td>\
                <td tabindex="1">key</td><td tabindex="1">Value</td><td tabindex="1">[type]</td>\
            </tr>');
    $(".md-editable").append(newRow);
    newRow.find(".md-action").on("click", function(evt, newValue){
        var td = $(evt.target).parent();
        var row = td.parent();
        row.remove();
    });
};

mdhandler.resetrow = function(row){
    row.find("td").each(function(i, item){
        item = $(item);
        item.text(item.attr("default"));
    });
    row.removeClass("info");
    row.removeClass("danger");
}

mdhandler.rmrow = function(row){
    row = $(row);
    mdhandler.resetrow(row);
    row.addClass("danger");
};

mdhandler.modified = function(){
    return $(".md-editable .md-existing").filter(".info");
}

mdhandler.neu = function(){
    return $(".md-new").filter(".success");
}

mdhandler.removed = function(){
    return $(".md-existing").filter(".danger");
}

mdhandler.getData = function(){
    var patchItems = new Array();

    mdhandler.modified().each(function(i, item){
        var patch = $(item);
        var keyData = $(patch.find("td")[1]);
        var key =  keyData.attr("default");
        var newkey =  keyData.text();
        var value = $(patch.find("td")[2]).text();
        var type = $(patch.find("td")[3]).text();

        if(key !== newkey){
            patchItems.push({"key": key, "value": null});
        }
        patchItems.push({"key": newkey, "value": value, "type":type});

    });

    mdhandler.removed().each(function(i, item){
        var rm = $(item);
        var keyData = $(rm.find("td")[1]);
        patchItems.push({"key": keyData.attr("default"), "value": null});
    });

    mdhandler.neu().each(function(i, item){
        var add = $(item);
        var key = $(add.find("td")[1]).text();
        var value = $(add.find("td")[2]).text();
        var type = $(add.find("td")[3]).text();
        patchItems.push({
            "key": key,
            "value": value,
            "type": type
        });
    });

    return patchItems;
}

$("document").ready(function(){

    $(".md-editable").editableTableWidget();

    $('.md-editable').on('validate', function(evt, newValue) {
        var target = $(evt.target);

        var _default = target.attr("default");
        var parent = target.parent();
        if(parent.hasClass("md-existing")){
            if(_default === newValue){
                parent.removeClass("info");
            } else {
                parent.addClass("info");
            }
        }
        return true;
    });
    
    $(".md-action").parent().attr("tabindex","");
    $(".md-action").on("click", function(evt, newValue){ 
        var span = $(evt.target);
        var td = span.parent();
        var row = td.parent();
        var isTrash = span.hasClass("glyphicon-trash");
        if(isTrash){
            mdhandler.rmrow(row);
        } else {
            mdhandler.resetrow(row);
        }
    });
    $(".md-add").on("click", function() { mdhandler.addrow(); } )
})
