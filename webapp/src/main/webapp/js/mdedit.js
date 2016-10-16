
var cloneProperties =  
        ["text-align", "font", "font-size", "font-family", "font-weight", "border", "border-top", "border-bottom", "border-left", "border-right"];
var cloneMethod = function(active, editor){
        editor
            .css(active.css(cloneProperties))
            .width(active.outerWidth())
            .height(active.outerHeight());
    };

var mdhandler = new Object();
mdhandler.addrow = function(){
    var newRow = $('\
            <tr class="md-new success">\
                <td><span class="md-action glyphicon glyphicon-trash"></span></td>\
                <td tabindex="1" class="md-key">[key]</td>\n\
                <td tabindex="1" class="md-value">[value]</td>\n\
                <td tabindex="1" class="md-type">string</td>\
            </tr>');
    $(".md-editable").append(newRow);
    newRow.find(".md-type").editableTableWidget({
        editor: $('<select class="form-control"><option>decimal</option><option>integer</option><option>string</option></select>'), 
        editorSelector:"select",
        cloneMethod: cloneMethod
    });
    newRow.find(".md-key", newRow)
            .add(".md-value", newRow)
            .editableTableWidget({errorClass: "has-error"});
    newRow.find(".md-action").on("click", function(evt, newValue){
        var td = $(evt.target).parent();
        var row = td.parent();
        row.remove();
    });
};

mdhandler.resetrow = function(row, type, value){
    row.find("td").each(function(i, item){
        item = $(item);
        if(item.data("id")){
            item.text(row.data(item.data("id")));
        }
    });
    row.removeClass("warning");
    row.removeClass("danger");
};

mdhandler.rmrow = function(row){
    row = $(row);
    mdhandler.resetrow(row);
    row.addClass("danger");
};

mdhandler.modified = function(){
    return $(".md-editable .md-existing").filter(".warning");
};

mdhandler.neu = function(){
    return $(".md-new").filter(".success");
};

mdhandler.removed = function(){
    return $(".md-existing").filter(".danger");
};

mdhandler.getData = function(){
    var patchItems = new Array();

    mdhandler.modified().each(function(i, item){
        var patchRow = $(item);
        var key =  patchRow.data("key");
        var newkey =  $(patchRow.find("td")[1]).text();
        var value = $(patchRow.find("td")[2]).text();
        var type = $(patchRow.find("td")[3]).text();

        if(key !== newkey){
            patchItems.push({"key": key, "value": null});
        }
        patchItems.push({"key": newkey, "value": value, "type":type});
    });

    mdhandler.removed().each(function(i, item){
        var rmRow = $(item);
        patchItems.push({"key": rmRow.data("key"), "value": null});
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
};

$("document").ready(function(){
    $(".md-editable .md-type").editableTableWidget({
        editor: $('<select class="form-control"><option>decimal</option><option>integer</option><option>string</option></select>'), 
        editorSelector:"select",
        cloneMethod: cloneMethod
    });
    
    $(".md-editable .md-value").editableTableWidget({errorClass: "has-error"});
    
    function validateValue(target, row, changedField){
        var currentValue = $(row.find("td")[2]).text();
        var currentType = $(row.find("td")[3]).text();
        if(target.hasClass("md-type")){
            var type = changedField;
            var value = currentValue;
        }
        if(target.hasClass("md-value")){
            var type = currentType;
            var value = changedField;
        }
        if(type === "integer" || type === "decimal"){
            try {
                var d = new Decimal(value);
            } catch (e) {
                var currentValue = $(row.find("td")[2]).text(currentValue);
                var currentType = $(row.find("td")[3]).text(currentType);
                return false;
            }
            if(type === "integer" && !d.isInteger()){
                var currentValue = $(row.find("td")[2]).text(currentValue);
                var currentType = $(row.find("td")[3]).text(currentType);
                return false;
            }
        }
        return true;
    }

    $('.md-editable').on('validate', function(evt, newValue) {
        var target = $(evt.target);
        var row = target.parent();
        if(!validateValue(target, row, newValue)){
            return false;
        }
        
        var parent = target.parent();
        if(parent.hasClass("md-existing")){
            var changed = false;
            $.each(row.data(), function(id){
                var originalValue = row.data(id);
                var textVal = row.find("[data-id='" + id + "']").text();
                if(target.data("id") === id){
                    textVal = newValue;
                }
                if(originalValue.toString() !== textVal){
                    changed |= true;
                }
            });
            if(!changed){
                parent.removeClass("warning");
            } else {
                parent.addClass("warning");
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
    $(".md-add").on("click", function() { mdhandler.addrow(); } );
});
