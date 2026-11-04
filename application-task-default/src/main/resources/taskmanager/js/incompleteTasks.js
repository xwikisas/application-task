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
define('taskmanager-incomplete-tasks-translation-keys', {
  prefix: 'taskmanager.incompleteTasks.',
  keys: [
    'notification.find.inprogress',
    'notification.find.done',
    'notification.find.error',
    'notification.fix.inprogress',
    'notification.fix.done',
    'notification.fix.error',
    'notification.fixAll.inprogress',
    'notification.fixAll.done',
    'notification.fixAll.error',
    'find.noTasks',
    'fix',
    'pagination.label'
  ]
});
require(['jquery', 'xwiki-meta', 'xwiki-l10n!taskmanager-incomplete-tasks-translation-keys'], function ($, xcontext, l10n) {
  let displayIncompleteTasks = function (pages, serviceDoc) {
    $('.incompleteTaskOwnerLink').remove();
    $('.incompleteTaskOwnerFix').remove();
    let template = document.querySelector('#incompleteTaskOwnerTemplate').content;
    pages.each(function (page) {
      let element = $(template.cloneNode(true));
      element.find('.incompleteTaskOwnerLink').attr('href', page.url).text(page.title);
      element.find('.incompleteTaskOwnerFix').attr('data-url', serviceDoc.getURL('get', $.param({ docRef: page.ref }))).text(l10n.get('fix'));
      $('.incompleteTaskOwners').append(element);
    });
  };
  let sendGetRequest = function(serviceDoc, offset, limit) {
    var serviceURL = serviceDoc.getURL('get', $.param({ outputSyntax: 'plain', getPages: '1', offset: offset, limit: limit }));
    let notification = new XWiki.widgets.Notification(l10n.get('notification.find.inprogress'), "inprogress");
    $('.incompleteTaskOwnersSectionMessage').addClass('hidden');
    $.getJSON(serviceURL)
      .done(function (pages) {
        $('.previousTasks').prop("disabled", false);
        $('.nextTasks').prop("disabled", false);
        displayIncompleteTasks(pages.entries, serviceDoc);
        notification.replace(new XWiki.widgets.Notification(l10n.get('notification.find.done'), "done"));
        if (pages.count <= 0) {
          $('.incompleteTaskOwnersSectionMessage').removeClass('hidden').html('<div class="box infomessage">' + l10n.get('find.noTasks') + '</div>');
        } else {
          $('.incompleteTaskOwnersSectionMessage').removeClass('hidden')
            .html('<span class="incomplete-task-owners-hint">' + l10n.get('pagination.label', pages.offset, pages.offset + pages.count, pages.total) + '</span>');
        }
        if (offset <= 0) {
          $('.previousTasks').prop("disabled", true);
        }
        if (pages.total <= offset + limit) {
          $('.nextTasks').prop("disabled", true);
        }
      })
      .fail(function (err) {
        notification.replace(new XWiki.widgets.Notification(l10n.get('notification.find.error'), "error"));
      });
  };
  $(function () {
    let serviceDocRef = XWiki.Model.resolve('TaskManager.IncompleteTasksService', XWiki.EntityType.DOCUMENT, XWiki.currentDocument.getDocumentReference());
    let serviceDoc = new XWiki.Document(serviceDocRef);
    sendGetRequest(serviceDoc, 0, 15);
    $('.incompleteTaskOwners').removeClass('loading');
    // Find incomplete task owners and display them when clicking refresh.
    let offset = 0;
    let limit = 15;
    $('.nextTasks').on('click', function () {
      offset += limit;
      sendGetRequest(serviceDoc, offset, limit);
    });
    $('.previousTasks').on('click', function () {
      offset = Math.max(0, offset - limit);
      sendGetRequest(serviceDoc, offset, limit);
    });
    $('.refreshTasks').on('click', function () {
      offset = 0;
      sendGetRequest(serviceDoc, offset, limit);
    });
    // Start the job and create a jobrunner object to periodically check the status of the job.
    $('.fixAllTasks').on('click', function () {
      let button = $(this);
      require(['xwiki-job-runner'], function (JobRunner) {
        var serviceURL = serviceDoc.getURL('get', $.param({ outputSyntax: 'plain', fixAll: '1', form_token: xcontext.form_token }));
        let notification = new XWiki.widgets.Notification(l10n.get('notification.fixAll.inprogress'), "inprogress");
        $.getJSON(serviceURL)
          .done(function (result) {
            notification.replace(new XWiki.widgets.Notification(l10n.get('notification.fixAll.done'), "done"));
            let url = serviceDoc.getURL('get', $.param({
              xpage: 'job_status_json',
              outputSyntax: 'plain',
              jobId: result.jobId,
              translationPrefix: 'taskmanager.incompleteTasks.job'
            }));
            var runnerConfig = {};

            runnerConfig.createStatusRequest = function () {
              return {
                url: url.replaceAll('%5B%5D', '')
              };
            };
            $('.incompleteTaskOwnersSectionMessage').removeClass('hidden').html('').addClass('loading');
            button.prop('disabled', true);
            new JobRunner(runnerConfig).resume()
              .progress(function () { })
              .then(
                function (job) {
                  button.prop('disabled', false);
                  $('.incompleteTaskOwnersSectionMessage').removeClass('hidden').removeClass('loading').html(job.message);
                  displayIncompleteTasks([], '');
                },
                function () {
                  button.prop('disabled', false);
                  $('.incompleteTaskOwnersSectionMessage').removeClass('loading')
                });
          })
          .fail(function (err) {
            notification.replace(new XWiki.widgets.Notification(l10n.get('notification.fixAll.error'), "error"));
          });
      });
    });
    $(document).on('click', '.incompleteTaskOwnerFix', function (event) {
      let button = $(this);
      event.preventDefault();
      var serviceURL = $(this).data('url') + '&' + $.param({
        'form_token': xcontext.form_token
      });
      let notification = new XWiki.widgets.Notification(l10n.get('notification.fix.inprogress'), "inprogress");
      $.get(serviceURL)
        .done(function () {
          // Remove the associated link and the button.
          button.prevAll()[0].remove();
          button.remove();
          notification.replace(new XWiki.widgets.Notification(l10n.get('notification.fix.done'), "done"));
        })
        .fail(function () {
          notification.replace(new XWiki.widgets.Notification(l10n.get('notification.fix.error'), "error"));
        });
    });
  });

});
