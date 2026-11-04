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
require(['jquery'], function ($) {
  /**
   * Returns the raw value of a macro parameter, as written in the macro call edited by the macro editor holding the
   * given element, or the default value of the parameter when the macro call doesn't set it.
   */
  const getMacroParameterValue = function (element, parameterId) {
    const macroCall = (element.closest('.macro-editor-modal').data('input') || {}).macroCall || {};
    const parameters = macroCall.parameters || {};
    // Macro parameter names are case insensitive.
    const parameterName = Object.keys(parameters).find(function (name) {
      return name.toLowerCase() === parameterId.toLowerCase();
    });
    if (parameterName) {
      // The macro call holds the parameter name (as written) and value.
      const parameter = parameters[parameterName];
      return typeof parameter === 'object' ? (parameter || {}).value : parameter;
    }
    const macroDescriptor = element.closest('.macro-editor').data('macroDescriptor') || {};
    return ((macroDescriptor.parameterDescriptorMap || {})[parameterId.toLowerCase()] || {}).defaultValue;
  };

  /**
   * Returns the selected statuses in the order from the macro call. The values are taken from the selected statuses,
   * since their case might differ from the macro call.
   */
  const getOrderedValue = function (select) {
    const selectedStatuses = select.val() || [];
    const value = getMacroParameterValue(select, select.attr('name'));
    const orderedStatuses = [];
    (typeof value === 'string' ? value.split(',') : []).forEach(function (status) {
      const selectedStatus = selectedStatuses.find(function (selectedStatus) {
        return selectedStatus.toLowerCase() === status.trim().toLowerCase();
      });
      if (selectedStatus !== undefined) {
        if (!orderedStatuses.includes(selectedStatus)) {
          orderedStatuses.push(selectedStatus);
        }
      }
    });
    // Keep the selected statuses we couldn't match, at the end.
    return orderedStatuses.concat(selectedStatuses.filter(function (status) {
      return !orderedStatuses.includes(status);
    }));
  };

  /**
   * The select lists its options in the order of the statuses, so the selectize widget displays the selected statuses
   * in that order instead of the order from the macro call, which matters (e.g. for the columns of the Kanban board).
   * Once the order is restored, the select gets the 'taskStatusPicker:ready' event and the 'taskStatusPickerReady'
   * data, so that code depending on the order of the selected statuses can wait for it.
   */
  const initStatusPicker = function (select) {
    const orderedValue = getOrderedValue(select);
    const restoreOrder = function () {
      const selectize = select[0].selectize;
      if (!selectize) {
        return false;
      }
      // Selectize adds the items in the given order, and updates the select options to match it.
      selectize.setValue(orderedValue, true);
      select.data('taskStatusPickerReady', true).trigger('taskStatusPicker:ready');
      return true;
    };
    if (!restoreOrder()) {
      // The selectize widget is not initialized yet. It inserts its markup next to the select when it is.
      const observer = new MutationObserver(function () {
        if (restoreOrder()) {
          observer.disconnect();
        }
      });
      observer.observe(select.parent()[0], {childList: true});
    }
  };

  const init = function (container) {
    $(container).find('select.task-status-picker').each(function () {
      const select = $(this);
      if (!select.data('taskStatusPickerInitialized')) {
        select.data('taskStatusPickerInitialized', true);
        initStatusPicker(select);
      }
    });
  };

  $(document).on('xwiki:dom:updated', function (event, data) {
    init(data ? data.elements : document.body);
  });
  // The macro editor might have been displayed before this code was loaded.
  init(document.body);
});
