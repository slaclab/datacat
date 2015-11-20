
var ajaxMethod = function (data, callback, settings) {
    var state = new Object();
    var columns = data.columns;

    state.offset = data.start;
    state.length = data.length;
    state.orderArray = [];
    state.filterObject = data.search;

    for (var i = 0; i < data.order.length; i++) {
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

function doSearch(){
    var filter = $('.datacat-component input').val();
    var uri = URI(document.URL);
    var newUri = uri.search({
        "filter": filter,
    });
    window.location = newUri.toString();
}

function paginateDatasets() {
    var qstr = URI(document.URL).query(true);

    var offset = (qstr.offset ? parseInt(qstr.offset) : null) || 0;
    var length = (qstr.max ? parseInt(qstr.max) : null) || 100;
    var sort = qstr.sort || "";
    var search = qstr.filter || "";
    var desc = false;
    var order = [];

    var columnIndex = {
        "name": 1,
        "dataType": 2,
        "size": 3,
        "created": 4
    }

    var columns = [
        {"name": "dl"},
        {"name": "name"},
        {"name": "dataType"},
        {"name": "size"},
        {"name": "created"}
    ];

    if (sort.length) {
        if (sort.charAt(sort.length - 1) === "-") {
            desc = true;
            sort = sort.substr(0, sort.length - 1);
        }
        if (columnIndex.hasOwnProperty(sort)) {
            order.push([columnIndex[sort], desc ? "desc" : "asc"]);
        }
    }

    /*$.extend(true, DataTable.defaults, {
     dom:
     "<'row'<'col-sm-6'l><'col-sm-6'f>>" +
     "<'row'<'col-sm-12'tr>>" +
     "<'row'<'col-sm-5'i><'col-sm-7'p>>",
     renderer: 'bootstrap'
     });*/

    var t = $('.datatable-datasets').DataTable({
        "columns": columns,
        "pagingType": "full_numbers",
        "serverSide": true,
        "deferLoading": resultInfo.total,
        "displayStart": offset,
        "pageLength": length,
        "order": order,
        "search": {
            "search": search
        },
        "searchDelay": 1000,
        "ajax": ajaxMethod,
        "dom":
                "<'row'<'col-sm-12'tr>>" +
                "<'row'<'col-sm-3'i><'col-sm-2'l><'col-sm-7'p>>",
        "lengthMenu": [ 25, 50, 100, 1000, 10000 ]
    });

    var searchbox = $('.datacat-component input');
    searchbox.val(search);
    searchbox.unbind();
    searchbox.bind('keyup', function (e) {
        if (e.keyCode === 13) {
            t.search(this.value).draw();
        }
    });
    $(".input-group-btn").on("click", doSearch);
}

function doCheck(val) {
    $(".datatable-datasets input:checkbox").prop("checked", val);
    updateCount();
}

function getChecked() {
    return $(".datatable-datasets input:checkbox:checked").map(function () {
        return this.value;
    });
}

var count = 0;
function updateCount(){
    count = $(".datatable-datasets input:checkbox:checked").size();
    $("#count").text(count);
    if(count){
        $("#dl-actions").show();
    } else {
        $("#dl-actions").hide();
    }
}

function downloadUrl(){
    $("#dl").attr("href", makeTextFile());
    $("#dl").attr("download", pageContext.target.name);
}

var textFile = null;
function makeTextFile() {
    var text = getChecked().toArray().join("\n") + "\n";
    var data = new Blob([text], {type: 'text/plain'});

    // If we are replacing a previously generated file we need to
    // manually revoke the object URL to avoid memory leaks.
    if (textFile !== null) {
        window.URL.revokeObjectURL(textFile);
    }

    textFile = window.URL.createObjectURL(data);

    // returns a URL you can use as a href
    return textFile;
};

$("document").ready(function () {
    $(function () {
        $(".glyphicon").tooltip();
        paginateDatasets();
        updateCount();
        $(".datatable-datasets input:checkbox").click(updateCount);
    });
});
