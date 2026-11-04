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
define('xwiki-task-template-list-notification-messages', {
  prefix: 'taskmanager.templateList.notification.',
  keys: [
    'inprogress',
    'done',
    'fail',
    'fail.conflict',
    'fail.unauthorized',
  ]
});
require(['jquery', 'xwiki-l10n!xwiki-task-template-list-notification-messages'], function ($, l10n) {
  var escapeHTML = function(text) {
    let textHTMLEscape = new Element('span');
    textHTMLEscape.textContent = text;
    return textHTMLEscape.innerHTML;
  };
  $(document).on('click', '#button-submit-template', function (event) {
    const endpointURL = new XWiki.Document(
        XWiki.Model.resolve('TaskManager.CreateTaskTemplateEndpoint', XWiki.EntityType.DOCUMENT)
    ).getURL('get');
    event.preventDefault();
    // Check that the template name is not empty.
    if (event.target.parentElement.reportValidity()) {
      let notification = new XWiki.widgets.Notification(escapeHTML(l10n.get('inprogress')), 'inprogress');
      $.post(endpointURL, event.target.parentElement.serialize()).done((data) => {
        notification.replace(new XWiki.widgets.Notification(escapeHTML(l10n.get('done')), 'done'));
        document.location = new XWiki.Document(
            XWiki.Model.resolve(data.templateDocument.reference, XWiki.EntityType.DOCUMENT)
        ).getURL('edit');
      }).fail((data) => {
        let reason = l10n.get('fail');
        if (data.status === 401) {
            reason = l10n.get('fail.unauthorized');
        } else if (data.status === 409) {
            reason = l10n.get('fail.conflict');
        }
        notification.replace(new XWiki.widgets.Notification(escapeHTML(reason), 'error'));
      });
    }
  })
});
