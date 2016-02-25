
/* global URI, resultInfo */

var ajaxMethod = function(data){
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
    var qstr = new URI(document.URL).query(true);

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
    };

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
        if (e.keyCode === 13) {
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


$("document").ready(function(){
    $(function () {
        $(".glyphicon").tooltip();
        if($('.datatable-datasets').length){
            paginateDatasets();
        }
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