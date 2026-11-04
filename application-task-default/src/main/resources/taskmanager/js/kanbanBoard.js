/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
require(['jquery', 'bootstrap'], function ($) {

  /**
   * Handles the fetching of the modal and the lazy loading of the css/js.
   */
  $(document).on('click', '.space-picker-kanban-parameter', function () {
    let URL = new XWiki.Document('SpacePicker', 'TaskManager').getURL('get');
    $.get(URL)
      .done(function (data) {
        $('body').append(`<div class='spacePickerKnbModal'> ${data} </div>`);
        $('.kanbanSpacePicker').modal('show');
        // Initialize UI elements that are lazy loaded.
        $(document).trigger('xwiki:dom:updated', { 'elements': $('.spacePickerKnbModal').toArray()});
      })
      .fail(function(){
        console.error('Failed to retrieve the space picker modal');
      });
  })

  $(document).on('show.bs.modal', '.kanbanSpacePicker', function () {
    $('.modal.macro-editor-modal.in').hide();
  })

  $(document).on('hidden.bs.modal', '.kanbanSpacePicker', function () {
    $('.modal.macro-editor-modal.in').show();
    // We remove the modal from the page
    $('.spacePickerKnbModal').remove();
  })
  /**
   * Handles the select button of the modal. If a space is selected it will return it's reference otherwise it will
   * return an empty string.
   */
  $(document).on('click', '.kanbanSpaceSelector', function () {
    // We remove the modal again
    selectedSpace = $('.xtree .jstree-clicked:last').attr('id');
    //Root element and we need to replace with an empty string so we can select all the tasks.
    if (selectedSpace === 'wiki:xwiki_anchor' || selectedSpace === 'document:xwiki:Main.WebHome_anchor') {
      selectedSpace = "";
    }
    else {
      selectedSpace = selectedSpace.replace('space:xwiki:', "").replace("_anchor", "");

    }
    $('.space-picker-kanban-parameter').val(selectedSpace);
    $('.kanbanSpacePicker').modal('hide');
    $('.modal.macro-editor-modal.in').show();
  })

  // The named colors supported by the Kanban macro, with the values it displays for them.
  const kanbanColors = {green: '#88cc44', blue: '#00aacc', orange: '#ff9911', yellow: '#ffcc33', red: '#ee4433'};
  // The color used by the Kanban macro for the columns that don't have one.
  const missingColor = 'blue';
  // Used to generate the ids of the column names, which describe the controls of each column.
  let columnCounter = 0;

  /**
   * Converts a color, as specified in the colors parameter, to the #rrggbb format required by the color inputs.
   */
  const toHexColor = function (color) {
    const kanbanColor = kanbanColors[color.toLowerCase()];
    if (kanbanColor) {
      return kanbanColor;
    }
    // Let the browser parse the CSS color. Invalid colors are ignored, and colors with transparency are returned in the
    // rgba() format, so we fall back on black for both.
    const context = document.createElement('canvas').getContext('2d');
    context.fillStyle = '#000000';
    context.fillStyle = color;
    return context.fillStyle.startsWith('#') ? context.fillStyle : '#000000';
  };

  const cloneTemplate = function (template) {
    return $(template.prop('content').firstElementChild.cloneNode(true));
  };

  /**
   * Replaces the columns and colors parameters in the macro editor with a list of the selected columns, where the
   * columns are added (from a searchable list of statuses), removed, reordered (by drag and drop or with the arrow
   * keys) and colored. The value of both parameters is computed from the list, so the colors stay paired with the columns.
   */
  const initColumnsEditor = function (colorsInput) {
    const columnsSelect = colorsInput.closest('.macro-parameters').find('select[name="columns"]');
    if (!columnsSelect.length) {
      // Keep the colors text input when there are no columns to pick the colors for.
      return;
    }
    // The colors are paired with the columns by position, so wait for the columns picker to restore the order of the
    // columns from the macro call (see taskStatusPicker.js).
    if (columnsSelect.data('taskStatusPickerReady')) {
      createColumnsEditor(colorsInput, columnsSelect);
    } else {
      columnsSelect.one('taskStatusPicker:ready', function () {
        createColumnsEditor(colorsInput, columnsSelect);
      });
    }
  };

  const createColumnsEditor = function (colorsInput, columnsSelect) {
    const selectize = columnsSelect[0].selectize;
    // All the statuses, in the order they are listed by the picker.
    const statuses = Object.values(selectize.options).sort(function (alice, bob) {
      return alice.$order - bob.$order;
    }).map(function (option) {
      return {value: option.value, label: option.label || option.text || option.value};
    });
    const getLabel = function (value) {
      const status = statuses.find(function (status) {
        return status.value === value;
      });
      return status ? status.label : value;
    };

    const selectizeControl = columnsSelect.next('.selectize-control').hide();
    colorsInput.closest('.macro-parameter').hide();
    const editor = cloneTemplate(colorsInput.siblings('template.kanban-columns-editor-template'))
      .insertAfter(selectizeControl);
    const columnTemplate = colorsInput.siblings('template.kanban-column-template');
    const suggestionTemplate = colorsInput.siblings('template.kanban-column-suggestion-template');
    const list = editor.find('.kanban-columns');
    const addDropdown = editor.find('.kanban-columns-add');
    const addButton = addDropdown.find('.dropdown-toggle');
    const searchInput = addDropdown.find('.kanban-columns-search input');
    const noMatch = addDropdown.find('.kanban-columns-no-match');

    const getColumns = function () {
      return list.children().map(function () {
        return $(this).attr('data-value');
      }).get();
    };

    // The color of each column, including the removed ones, so that they get their color back when added again.
    const columnColors = {};
    const colors = colorsInput.val().split(',');
    (columnsSelect.val() || []).forEach(function (column, index) {
      columnColors[column] = (colors[index] || '').trim() || missingColor;
    });
    const getColor = function (column) {
      if (!columnColors[column]) {
        // Give the added columns the first Kanban color that is not used yet.
        const usedColors = getColumns().map(function (column) {
          return (columnColors[column] || '').toLowerCase();
        });
        columnColors[column] = Object.keys(kanbanColors).find(function (color) {
          return !usedColors.includes(color);
        }) || missingColor;
      }
      return columnColors[column];
    };

    const createColumn = function (value) {
      const column = cloneTemplate(columnTemplate).attr('data-value', value);
      const nameId = 'kanban-column-name-' + columnCounter++;
      column.find('.kanban-column-name').attr('id', nameId).text(getLabel(value));
      column.find('.kanban-column-handle, .kanban-column-color, .kanban-column-remove')
        .attr('aria-describedby', nameId);
      column.find('.kanban-column-color').val(toHexColor(getColor(value)));
      return column;
    };

    const isDropdownOpen = function () {
      return addDropdown.hasClass('open');
    };

    const closeDropdown = function () {
      if (isDropdownOpen()) {
        addButton.dropdown('toggle');
      }
    };

    const createSuggestion = function (status) {
      const suggestion = cloneTemplate(suggestionTemplate).attr('data-value', status.value);
      suggestion.find('.kanban-column-suggestion-name').text(status.label);
      return suggestion;
    };

    // Lists the statuses that are not displayed yet and match the search query.
    const updateSuggestions = function () {
      const columns = getColumns();
      const availableStatuses = statuses.filter(function (status) {
        return !columns.includes(status.value);
      });
      if (!availableStatuses.length) {
        // Bootstrap doesn't toggle the dropdown of a disabled button.
        closeDropdown();
      }
      addButton.prop('disabled', !availableStatuses.length);
      const query = searchInput.val().trim().toLowerCase();
      const matchingStatuses = availableStatuses.filter(function (status) {
        return status.label.toLowerCase().includes(query) || status.value.toLowerCase().includes(query);
      });
      addDropdown.find('.kanban-column-suggestion').remove();
      noMatch.before(matchingStatuses.map(createSuggestion)).toggle(!matchingStatuses.length);
    };

    // Computes the value of the columns and colors parameters from the list of columns.
    const update = function () {
      const columns = getColumns();
      // Selectize updates the select options to match the given order, which the macro editor serializes.
      selectize.setValue(columns, true);
      colorsInput.val(columns.map(getColor).join(','));
      list.toggle(columns.length > 0);
      editor.find('.kanban-columns-empty').toggle(!columns.length);
      updateSuggestions();
    };

    const addColumn = function (value) {
      const column = createColumn(value).appendTo(list);
      update();
      if (addButton.prop('disabled')) {
        // All the statuses are displayed, so the dropdown is closed. Keep the focus near the added column.
        column.find('.kanban-column-handle').focus();
      }
    };

    initColumnsList(list, update);

    list.on('input change', '.kanban-column-color', function () {
      columnColors[$(this).closest('.kanban-column').attr('data-value')] = $(this).val();
      colorsInput.val(getColumns().map(getColor).join(','));
    }).on('click', '.kanban-column-remove', function () {
      const column = $(this).closest('.kanban-column');
      // Keep the focus in the list, so that the keyboard users can remove several columns in a row.
      const nextFocus = (column.next().length ? column.next() : column.prev()).find('.kanban-column-remove');
      column.remove();
      update();
      (nextFocus.length ? nextFocus : addButton).focus();
    });

    addDropdown.on('shown.bs.dropdown', function () {
      searchInput.val('');
      updateSuggestions();
      searchInput.focus();
    }).on('input', '.kanban-columns-search input', updateSuggestions).on('click', '.kanban-column-suggestion a',
      function (event) {
        // Keep the dropdown open, so that several columns can be added in a row.
        event.preventDefault();
        event.stopPropagation();
        const suggestion = $(this).parent();
        const index = addDropdown.find('.kanban-column-suggestion').index(suggestion);
        addColumn(suggestion.attr('data-value'));
        if (isDropdownOpen()) {
          // Focus the suggestion that took the place of the added one, or the search input when there is none.
          const suggestions = addDropdown.find('.kanban-column-suggestion a');
          (suggestions.length ? suggestions.eq(Math.min(index, suggestions.length - 1)) : searchInput).focus();
        }
      }).on('keydown', function (event) {
      handleDropdownKeys(event, addDropdown, searchInput, closeDropdown);
    });

    // Display the selected columns.
    list.append((columnsSelect.val() || []).map(createColumn));
    update();
  };

  /**
   * Handles the keyboard navigation in the dropdown listing the statuses to add, before the Bootstrap dropdown and the
   * macro editor modal get the events.
   */
  const handleDropdownKeys = function (event, addDropdown, searchInput, closeDropdown) {
    const suggestions = addDropdown.find('.kanban-column-suggestion a');
    const inSearchInput = searchInput.is(event.target);
    if (event.key === 'Escape' && addDropdown.hasClass('open')) {
      // Close only the dropdown, not the macro editor modal.
      closeDropdown();
      addDropdown.find('.dropdown-toggle').focus();
    } else if (event.key === 'Enter' && inSearchInput) {
      // Add the first matching status, and keep searching.
      suggestions.first().click();
      if (addDropdown.hasClass('open')) {
        searchInput.focus();
      }
    } else if (event.key === 'ArrowDown' && inSearchInput) {
      suggestions.first().focus();
    } else if (event.key === 'ArrowUp' && suggestions.first().is(event.target)) {
      searchInput.focus();
    } else {
      return;
    }
    event.preventDefault();
    event.stopPropagation();
  };

  /**
   * Makes the columns of the list reorderable by dragging their handle, or by pressing the up and down arrow keys while
   * their handle is focused.
   */
  const initColumnsList = function (list, onReorder) {
    let draggedColumn = null;
    list.on('pointerdown', '.kanban-column-handle', function () {
      // Only the handle starts the drag, so that the color input and the column name keep working as usual.
      $(this).closest('.kanban-column').attr('draggable', 'true');
    }).on('pointerup', '.kanban-column-handle', function () {
      $(this).closest('.kanban-column').removeAttr('draggable');
    }).on('dragstart', '.kanban-column', function (event) {
      draggedColumn = $(this).addClass('dragging');
      const dataTransfer = event.originalEvent.dataTransfer;
      dataTransfer.effectAllowed = 'move';
      // Firefox doesn't start the drag without data.
      dataTransfer.setData('text/plain', draggedColumn.attr('data-value'));
    }).on('dragover', function (event) {
      if (!draggedColumn) {
        // Something else is dragged over the list.
        return;
      }
      event.preventDefault();
      event.originalEvent.dataTransfer.dropEffect = 'move';
      // Move the dragged column before the first column whose middle is below the pointer.
      const pointerY = event.originalEvent.clientY;
      const nextColumn = list.children().not(draggedColumn).filter(function () {
        const box = this.getBoundingClientRect();
        return pointerY < box.top + box.height / 2;
      }).first();
      if (!nextColumn.length) {
        draggedColumn.appendTo(list);
      } else if (draggedColumn.next()[0] !== nextColumn[0]) {
        draggedColumn.insertBefore(nextColumn);
      }
    }).on('drop', function (event) {
      if (draggedColumn) {
        // Prevent the browser from opening the dragged data.
        event.preventDefault();
      }
    }).on('dragend', '.kanban-column', function () {
      $(this).removeClass('dragging').removeAttr('draggable');
      draggedColumn = null;
      onReorder();
    }).on('keydown', '.kanban-column-handle', function (event) {
      const column = $(this).closest('.kanban-column');
      // Move the sibling column instead of the focused one, which would lose the focus.
      if (event.key === 'ArrowUp' && column.prev().length) {
        column.prev().insertAfter(column);
      } else if (event.key === 'ArrowDown' && column.next().length) {
        column.next().insertBefore(column);
      } else {
        return;
      }
      event.preventDefault();
      onReorder();
    });
  };

  const initAllColumnsEditors = function (container) {
    $(container).find('input.kanban-column-colors').each(function () {
      const colorsInput = $(this);
      if (!colorsInput.data('kanbanColumnsEditorInitialized')) {
        colorsInput.data('kanbanColumnsEditorInitialized', true);
        initColumnsEditor(colorsInput);
      }
    });
  };

  $(document).on('xwiki:dom:updated', function (event, data) {
    initAllColumnsEditors(data ? data.elements : document.body);
  });
  // The macro editor might have been displayed before this code was loaded.
  initAllColumnsEditors(document.body);
});
