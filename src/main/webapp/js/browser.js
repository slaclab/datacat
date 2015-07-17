

function paginateContainers(){
    $("#paginate-containers-button").remove();
    $('.datatable-containers').dataTable({ "sPaginationType": "bs_normal", "iDisplayLength": 15 });	
    $('.datatable-containers').each(function(){
        var datatable = $(this);
        // SEARCH - Add the placeholder for Search and Turn this into in-line form control
        var search_input = datatable.closest('.dataTables_wrapper').find('div[id$=_filter] input');
        search_input.attr('placeholder', 'Search');
        search_input.addClass('form-control input-sm');
        // LENGTH - Inline-Form control
        var length_sel = datatable.closest('.dataTables_wrapper').find('div[id$=_length] select');
        length_sel.addClass('form-control input-sm');
    });
}

var ajaxMethod = function(data, callback, settings){
    var state = new Object();
    var columns = data.columns;
    
    state.offset = data.start;
    state.length = data.length;
    state.orderArray = [];
    state.filterObject = data.search;
    
    for(var i = 0; i < data.order.length; i++){
        var idx = data.order[i].column;
        var desc = data.order[i].dir === "desc";
        var sort = columns[idx].name + (desc ? "-" : "");
        state.orderArray.push(sort);
    }
    
    var uri = URI(document.URL);
    var newUri = uri.search({
        offset: state.offset, 
        max: state.length, 
        filter: state.filterObject.value,
        sort: state.orderArray
    });
    window.location = newUri.toString();
};

function paginateDatasets(){
    var qstr = URI(document.URL).query(true);
    
    var offset = (qstr.offset ? parseInt(qstr.offset) : null) || 0;
    var length = (qstr.max ? parseInt(qstr.max) : null) || 100;
    var sort = qstr.sort || "";
    var search = qstr.filter || "";
    var desc = false;
    var order = [];

    var columnIndex = {
        "name" : 1,
        "dataType" : 2,
        "size" : 3,
        "created" : 4
    }
    
    var columns = [
          { "name": "dl" },
          { "name": "name" },
          { "name": "dataType" },
          { "name": "size" },
          { "name": "created" }
        ];
    
    if(sort.length){
        if(sort.charAt(sort.length-1) === "-"){
            desc = true;
            sort = sort.substr(0, sort.length-1);
        }
        if(columnIndex.hasOwnProperty(sort)){
            order.push([columnIndex[sort], desc ? "desc" : "asc"]);
        }
    }

    var t = $('.datatable-datasets').DataTable({ 
        "columns": columns,
        "pagingType": "full_numbers",
        "serverSide": true, 
        "deferLoading": resultInfo.total,
        "displayStart": offset,
        "pageLength": length,
        "order": order,
        "search": {
            "search" : search
        },
        "searchDelay": 1000,
        "ajax": ajaxMethod 
    });
    
    var searchbox = $('.datacat-component input');
    searchbox.unbind();
    searchbox.bind('keyup', function (e) {
        if (e.keyCode == 13) {
            t.search(this.value).draw();
        }
    });
}

function checkAll(){
    $("input:checkbox").prop("checked", true);
}

function unCheckAll(){
    $("input:checkbox").prop("checked", false);
}

function getChecked(){
    return $("input:checkbox:checked").map(function(){ return this.value; });
}

function updatePagination(){
    var dt = $(".datatable-containers").dataTable();
    var val = $('input[name=options]:checked').val();
    dt.fnSettings()._iDisplayLength = parseInt(val);
    dt.fnDraw();
}

/*$(".datatable-containers").ready(
    function(){
        $(".needsInfo").each(function(idx, item){
            var path = $(item).attr("asyncpath");
            $.ajax({
                dataType: "json",
                url: path
            }).done(function( data, status, xhr ) {
                // Should be a tr
                var row = $(item).parent().parent();
                var cells = row.find("td");
                var pathItem = eval(data);
                var cStat = pathItem["stat"];
                if(pathItem["$type"] == "folder"){
                    $(cells[2]).text(cStat["folderCount"]);
                    $(cells[3]).text(cStat["groupCount"]);
                }
                $(cells[4]).text(cStat["datasetCount"]);
            }).fail(function( xhr, status, code ) {

                });
        });
    }
);*/


$("document").ready(function(){
    $(function () {
        $(".glyphicon").tooltip();
        paginateDatasets();
    });
});

/*
 $('#metadata').append($('<tr class="danger"><td>Key</td><td>Value</td><td>(String)</td></tr>'))

var toggleRemove = function(){
    
}

$('#metadata tr').on('click', function(evt, newValue) { console.log(evt); $(evt.target).parent().toggleClass("selected") } )

$("#metadata").editableTableWidget();
$("#metadata").on('validate', function(evt, newValue) { $(evt.target).parent().toggleClass("info"); } )
 */