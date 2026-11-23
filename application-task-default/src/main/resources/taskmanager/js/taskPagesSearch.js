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
require(['jquery', 'xwiki-selectize'], function($) {
  var searchServiceDocRef = XWiki.Model.resolve('TaskManager.TaskPagesSearch', XWiki.EntityType.DOCUMENT,
    XWiki.currentDocument.getDocumentReference());
  var searchServiceURL = new XWiki.Document(searchServiceDocRef).getURL('get', 'outputSyntax=plain');
  var errorHandler = function(response, callback) {
    console.log('Failed to get the JSON from [' + searchServiceURL + ']. Reason: ' + response.status + ' ' + response.statusText);
    callback([]);
  };
  var getSettings = function(picker) {
    var valueType = picker.attr('data-value-type') || 'reference';
    return {
      load: function(typedText, callback) {
        $.getJSON(searchServiceURL, {
          text: typedText,
          valueType: valueType
        }).done(callback).fail(function (response) {
          errorHandler(response, callback);
        });
      },
      loadSelected: function(selectedValue, callback) {
        $.getJSON(searchServiceURL, {
          text: selectedValue,
          valueType: valueType,
          exactMatch: true
        }).done(callback).fail(function (response) {
          errorHandler(response, callback);
        });
      }
    };
  };
  var init = function(container) {
    $(container).find('select.suggest-task-pages').each(function() {
      var picker = $(this);
      if (!picker.data('taskPickerInitialized')) {
        picker.data('taskPickerInitialized', true);
        picker.xwikiSelectize(getSettings(picker));
      }
    });
  };
  // The macro editor fires this event once it has inserted the parameter fields, which can be after the modal is shown.
  $(document).on('xwiki:dom:updated', function(event, data) {
    init(data ? data.elements : document.body);
  });
  // The picker might have been displayed before this code was loaded.
  init(document.body);
});
