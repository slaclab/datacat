/* Set the defaults for DataTables initialisation */
$.extend( true, $.fn.dataTable.defaults, {
    "sDom": "<'row'<'col-sm-12'<'pull-right'f><'pull-left'i>r<'clearfix'>>>t<'row'<'col-sm-12'<'pull-left'l><'pull-right'p><'clearfix'>>>",
    "sPaginationType": "bs_normal",
    "_iDisplayLength" :15,
    "oLanguage": {
        "sLengthMenu" : 
        '<div class="btn-group" data-toggle="buttons">'+
        '  <label class="btn btn-primary btn-sm active">'+
        '    <input type="radio" name="options" id="option1" value="15" onchange="updatePagination()" checked>15'+
        '  </label>'+
        '  <label class="btn btn-primary btn-sm">'+
        '    <input type="radio" name="options" id="option2" value="25" onchange="updatePagination()">25'+
        '  </label>'+
        '  <label class="btn btn-primary btn-sm">'+
        '    <input type="radio" name="options" id="option3" value="50" onchange="updatePagination()">50'+
        '  </label> '+
        '</div> ',
        "sSearch": ""
    }
} );

/* Default class modification */
$.extend( $.fn.dataTableExt.oStdClasses, {
    "sWrapper": "dataTables_wrapper form-inline"
} );

/* API method to get paging information */
$.fn.dataTableExt.oApi.fnPagingInfo = function ( oSettings )
{
    return {
        "iStart":         oSettings._iDisplayStart,
        "iEnd":           oSettings.fnDisplayEnd(),
        "iLength":        oSettings._iDisplayLength,
        "iTotal":         oSettings.fnRecordsTotal(),
        "iFilteredTotal": oSettings.fnRecordsDisplay(),
        "iPage":          oSettings._iDisplayLength === -1 ?
        0 : Math.ceil( oSettings._iDisplayStart / oSettings._iDisplayLength ),
        "iTotalPages":    oSettings._iDisplayLength === -1 ?
        0 : Math.ceil( oSettings.fnRecordsDisplay() / oSettings._iDisplayLength )
    };
};

/* Bootstrap style pagination control */
$.extend( $.fn.dataTableExt.oPagination, {
    "bs_normal": {
        "fnInit": function( oSettings, nPaging, fnDraw ) {
            var oLang = oSettings.oLanguage.oPaginate;
            var fnClickHandler = function ( e ) {
                e.preventDefault();
                if ( oSettings.oApi._fnPageChange(oSettings, e.data.action) ) {
                    fnDraw( oSettings );
                }
            };
            $(nPaging).append(
                '<ul class="pagination pagination-sm">'+
                '<li class="prev disabled"><a href="#"><span class="glyphicon glyphicon-chevron-left"></span>&nbsp;'+oLang.sPrevious+'</a></li>'+
                '<li class="next disabled"><a href="#">'+oLang.sNext+'&nbsp;<span class="glyphicon glyphicon-chevron-right"></span></a></li>'+
                '</ul>'
                );
            var els = $('a', nPaging);
            $(els[0]).bind( 'click.DT', {
                action: "previous"
            }, fnClickHandler );
            $(els[1]).bind( 'click.DT', {
                action: "next"
            }, fnClickHandler );
        },
        "fnUpdate": function ( oSettings, fnDraw ) {
            var iListLength = 5;
            var oPaging = oSettings.oInstance.fnPagingInfo();
            var an = oSettings.aanFeatures.p;
            var i, ien, j, sClass, iStart, iEnd, iHalf=Math.floor(iListLength/2);
            if ( oPaging.iTotalPages < iListLength) {
                iStart = 1;
                iEnd = oPaging.iTotalPages;
            }
            else if ( oPaging.iPage <= iHalf ) {
                iStart = 1;
                iEnd = iListLength;
            } else if ( oPaging.iPage >= (oPaging.iTotalPages-iHalf) ) {
                iStart = oPaging.iTotalPages - iListLength + 1;
                iEnd = oPaging.iTotalPages;
            } else {
                iStart = oPaging.iPage - iHalf + 1;
                iEnd = iStart + iListLength - 1;
            }
            for ( i=0, ien=an.length ; i<ien ; i++ ) {
                $('li:gt(0)', an[i]).filter(':not(:last)').remove();
                for ( j=iStart ; j<=iEnd ; j++ ) {
                    sClass = (j==oPaging.iPage+1) ? 'class="active"' : '';
                    $('<li '+sClass+'><a href="#">'+j+'</a></li>')
                    .insertBefore( $('li:last', an[i])[0] )
                    .bind('click', function (e) {
                        e.preventDefault();
                        oSettings._iDisplayStart = (parseInt($('a', this).text(),15)-1) * oPaging.iLength;
                        fnDraw( oSettings );
                    } );
                }
                if ( oPaging.iPage === 0 ) {
                    $('li:first', an[i]).addClass('disabled');
                } else {
                    $('li:first', an[i]).removeClass('disabled');
                }

                if ( oPaging.iPage === oPaging.iTotalPages-1 || oPaging.iTotalPages === 0 ) {
                    $('li:last', an[i]).addClass('disabled');
                } else {
                    $('li:last', an[i]).removeClass('disabled');
                }
            }
        }
    }
} );


/*
 * TableTools Bootstrap compatibility
 * Required TableTools 2.1+
 */
if ( $.fn.DataTable.TableTools ) {
    // Set the classes that TableTools uses to something suitable for Bootstrap
    $.extend( true, $.fn.DataTable.TableTools.classes, {
        "container": "DTTT btn-group",
        "buttons": {
            "normal": "btn-sm",
            "disabled": "disabled"
        },
        "collection": {
            "container": "DTTT_dropdown dropdown-menu",
            "buttons": {
                "normal": "",
                "disabled": "disabled"
            }
        },
        "print": {
            "info": "DTTT_print_info modal"
        },
        "select": {
            "row": "active"
        }
    } );

    // Have the collection use a bootstrap compatible dropdown
    $.extend( true, $.fn.DataTable.TableTools.DEFAULTS.oTags, {
        "collection": {
            "container": "ul",
            "button": "li",
            "liner": "a"
        }
    } );
}
