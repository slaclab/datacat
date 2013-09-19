

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

function paginateDatasets(){
    $("#paginate-datasets-button").remove();
    $('.datatable-datasets').dataTable({ "sPaginationType": "bs_normal", "iDisplayLength": 15 });	
    $('.datatable-datasets').each(function(){
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

function updatePagination(){
    var dt = $(".datatable-containers").dataTable();
    var val = $('input[name=options]:checked').val();
    dt.fnSettings()._iDisplayLength = parseInt(val);
    dt.fnDraw();
}

$(".datatable-containers").ready(
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
    );


$("document").ready(function(){
    $(function () {
        $(".glyphicon").tooltip();
    });
});
