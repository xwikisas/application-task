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
define('xwiki-task-status-change-messages', {
  prefix: 'taskmanager.notification.statusChange.',
  keys: [
    'noId',
    'inprogress',
    'success',
    'restFail'
  ]
});
require(['jquery', 'xwiki-l10n!xwiki-task-status-change-messages'], function ($, l10n) {
  $(document).on('change', '.task-status', function () {
    let checkbox = $(this);
    // In the WYSIWYG editor the checkbox updates the macro parameters instead (see ckeditorPluginEnabler.js).
    if (checkbox.closest('.cke_editable').length > 0) {
      return;
    }
    let taskId = checkbox.data('taskid');
    let previousVal = !checkbox.prop('checked');
    if (!taskId) {
      checkbox.prop('checked', previousVal);
      new XWiki.widgets.Notification(l10n.get('noId'), 'error');
      return;
    }
    let task = checkbox.closest('.task-macro');
    let restURL = '';
    if (checkbox.closest('.taskbox').length > 0) {
      // Read the attribute, since data() converts the ids that look like numbers.
      let rawId = checkbox.attr('data-rawid') || '';
      // The id is a single path segment. The dot segments can't be used since they are resolved even when encoded.
      if (rawId != '' && rawId != '.' && rawId != '..') {
        restURL = XWiki.currentDocument.getRestURL() + '/taskbox/' + encodeURIComponent(rawId) + '?checked=' +
          checkbox.prop('checked');
      }
    } else if (task.length > 0) {
      let taskDocument = XWiki.Model.resolve(taskId, XWiki.EntityType.DOCUMENT, XWiki.currentDocument.documentReference);
      if (taskDocument.getReversedReferenceChain()[0].type != XWiki.EntityType.WIKI) {
        taskDocument.getReversedReferenceChain()[0].parent = new XWiki.WikiReference(XWiki.currentWiki);
      }
      let baseURL = new XWiki.Document(taskDocument).getRestURL();
      restURL = baseURL + '/task?status=toggle';
    }
    if (restURL == '') {
      checkbox.prop('checked', previousVal);
      new XWiki.widgets.Notification(l10n.get('noId'), 'error');
      return;
    }

    checkbox.prop('disabled', true);
    let notification = new XWiki.widgets.Notification(l10n.get('inprogress'), 'inprogress');
    $.ajax({
      url: restURL,
      type: 'PUT',
      success: function () {
        notification.replace(new XWiki.widgets.Notification(l10n.get('success'), 'done'));
      },
      error: function () {
        checkbox.prop('checked', previousVal);
        notification.replace(new XWiki.widgets.Notification(l10n.get('restFail'), 'error'));
      },
      complete: function () {
        checkbox.prop('disabled', false);
      }
    });
  });
});
