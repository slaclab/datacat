/*global $, window*/
$.fn.editableTableWidget = function (options) {
	'use strict';
	return $(this).each(function () {
		var buildDefaultOptions = function () {
				var opts = $.extend({}, $.fn.editableTableWidget.defaultOptions);
				opts.editor = opts.editor.clone();
				return opts;
			},
			activeOptions = $.extend(buildDefaultOptions(), options),
			ARROW_LEFT = 37, ARROW_UP = 38, ARROW_RIGHT = 39, ARROW_DOWN = 40, ENTER = 13, ESC = 27, TAB = 9,
			elements = $(this),
			defaultCloneMethod = function(active, editor){
					editor.css(active.css(activeOptions.cloneProperties))
					.width(active.width())
					.height(active.height());
			},
			cloneMethod = activeOptions.cloneMethod || defaultCloneMethod,
			showEditor = function(evt, select) {
				var active = $(evt.target);
				var editor = createEditor(active);
				if (active.length) {
					editor.val(active.text())
						.removeClass('error')
						.show()
						.offset(active.offset())
					cloneMethod(active, editor);
					editor.focus();
					if (select) {
						editor.select();
					}
				}
			},
			setActiveText = function(active, editor) {
				var text = editor.val(),
					evt = $.Event('change'),
					originalContent;
				if (active.text() === text || editor.hasClass('error')) {
					return true;
				}
				originalContent = active.html();
				active.text(text).trigger(evt, text);
				if (evt.result === false) {
					active.html(originalContent);
				}
			},
			movement = function (element, keycode) {
				if (keycode === ARROW_RIGHT) {
					return element.next('td');
				} else if (keycode === ARROW_LEFT) {
					return element.prev('td');
				} else if (keycode === ARROW_UP) {
					return element.parent().prev().children().eq(element.index());
				} else if (keycode === ARROW_DOWN) {
					return element.parent().next().children().eq(element.index());
				}
				return [];
			},
			removeEditor = function(editor, resizer){
				editor.remove();
				editor = null;
				$(window).off('resize', resizer);
                        },
			createEditor = function(active){
				var editor = activeOptions.editor
					.css('position', 'absolute')
					.attr("id", "editor")
					.appendTo(document.body);
				var resizer = function (e) {
					if (editor) {
						editor.val(active.text());
						removeEditor(editor, this);
					}
				};
                                $(window).on('resize', resizer);
				editor.blur(function () {
					setActiveText(active, editor);
					editor.remove();
					editor = null;
					$(window).off('resize', resizer);
				}).keydown(function (e) {
					if (e.which === ENTER) {
						setActiveText(active, editor);
						removeEditor(editor, resizer);
						active.focus();
						e.preventDefault();
						e.stopPropagation();
					} else if (e.which === ESC) {
						editor.val(active.text());
						e.preventDefault();
						e.stopPropagation();
						removeEditor(editor, resizer);
						active.focus();
					} else if (e.which === TAB) {
						active.focus();
					} else if (this.selectionEnd - this.selectionStart === this.value.length) {
						var possibleMove = movement(active, e.which);
						if (possibleMove.length > 0) {
							possibleMove.focus();
							e.preventDefault();
							e.stopPropagation();
						}
					}
				})
				.on('input paste', function () {
					var evt = $.Event('validate');
					active.trigger(evt, editor.val());
					if (evt.result === false) {
						editor.addClass('error');
					} else {
						editor.removeClass('error');
					}
				});
                                return editor;
			};

                elements.on('click keypress dblclick', showEditor)
		.css('cursor', 'pointer')
		.keydown(function (e) {
			var prevent = true,
				possibleMove = movement($(e.target), e.which);
			if (possibleMove.length > 0) {
				possibleMove.focus();
			} else if (e.which === ENTER) {
				showEditor(e, false);
			} else if (e.which === 17 || e.which === 91 || e.which === 93) {
				showEditor(e, true);
				prevent = false;
			} else {
				prevent = false;
			}
			if (prevent) {
				e.stopPropagation();
				e.preventDefault();
			}
		});

		elements.prop('tabindex', 1);

	});

};
$.fn.editableTableWidget.defaultOptions = {
	cloneProperties: ['padding', 'padding-top', 'padding-bottom', 'padding-left', 'padding-right',
					  'text-align', 'font', 'font-size', 'font-family', 'font-weight',
					  'border', 'border-top', 'border-bottom', 'border-left', 'border-right'],
	editor: $('<input>')
};
