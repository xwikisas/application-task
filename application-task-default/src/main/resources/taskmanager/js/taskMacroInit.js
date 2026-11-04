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
define('xwiki-task-notification-messages', {
  prefix: 'taskmanager.notification.',
  keys: [
    'referenceGeneration.fail',
    'referenceGeneration.inprogress',
    'referenceGeneration.invalid'
  ]
});
require(['jquery', 'xwiki-l10n!xwiki-task-notification-messages', 'xwiki-meta', 'moment', 'moment-jdateformatparser'], function ($, l10n, xm, moment) {
  let inputModified = false;
  let validationAttached = false;
  let initReference = function (modal, force) {
    let idInput = modal.find("input[name='reference']");
    if (idInput.val() && !force) return;
    let refGenURL = XWiki.currentDocument.getRestURL() + '/task-reference';
    //idInput.prop('disabled', true);
    idInput.addClass('loading');
    modal.find('.btn-primary').prop('disabled', true);
    let not = new XWiki.widgets.Notification(l10n.get('referenceGeneration.inprogress'), 'inprogress');
    $.ajax({
      url: refGenURL,
      method: 'GET',
      dataType: 'text'
    })
      .done(function (ref) {
        not.hide();
        idInput.val(ref);
        if (!force) {
          idInput.closest('.macro-parameter').addClass('hidden');
        }
        idInput.trigger('change');
      })
      .fail(function (response) {
        // idInput.prop('disabled', false);
        not.replace(new XWiki.widgets.Notification(l10n.get('referenceGeneration.fail'), 'error'));
      })
      .always(function () {
        idInput.removeClass('loading');
        modal.find('.btn-primary').prop('disabled', false);
      });
  };
  let initCompleteDate = function () {
    // TODO: Replace 'Done' with an user configurable value.
    if ($(this).val() == 'Done') {
      if (!$('.modal :input[name="completeDate"]').val()) {
        var dateFormat = moment().toMomentFormatString($('.modal').find("input[name='reference']").data('dateformat'));
        $('.modal :input[name="completeDate"]').val(moment(new Date()).format(dateFormat)).prop('disabled', false);
      }
    } else {
      $('.modal :input[name="completeDate"]').val('').prop('disabled', true);
    }
  };
  let init = function () {
    inputModified = false;
    let isTaskMacroModal = !!$('.task-reference').length;
    if (isTaskMacroModal) {
      let modal = $('.modal');
      $('.modal select[name="status"]').each(initCompleteDate)
      initReference(modal, false);
      let idInput = modal.find("input[name='reference']");
      if (idInput.val()) return;
      let createDateInput = modal.find("input[name='createDate']");
      let creatorInput = modal.find("input[name='reporter']");
      var dateFormat = moment().toMomentFormatString($(idInput).data('dateformat'));
      createDateInput.val(moment(new Date()).format(dateFormat));
      let creatorValue = xm.userReference.relativeTo(new XWiki.WikiReference(XWiki.currentWiki)).toString();
      creatorInput.val(creatorValue);
    }
  };

  init();
  // Create a relationship between status and completeDate fields.

  $(document).on('change', '.modal .task-reference', function () {
    if (!validationAttached) {
      $(this).closest('.modal').find('.btn-primary').on('click', function (event) {
        if (!inputModified) {
          return;
        }
        const btn = $(this);
        const form = btn.closest('.modal-content');
        let ref = form.find('.task-reference').val();
        // If the reference is empty, it will get handled by the fact that its a required parameter.
        if (!ref) {
          return;
        }
        let validateUrl = XWiki.currentDocument.getRestURL() + '/task-reference/' + encodeURIComponent(ref);
        btn.addClass('loading');
        event.preventDefault();
        $.get(validateUrl)
          .done(function () {
            form[0].requestSubmit();
          })
          .fail(function (xhr) {
            form.find("input[name='reference']").closest('.macro-parameter').removeClass('hidden');
            new XWiki.widgets.Notification(l10n.get('referenceGeneration.invalid'), 'error');
          }).always(function () {
            btn.removeClass('loading');
          });
      });
      validationAttached = true;
    }
    inputModified = true;
  });
  $(document).on('change', '.modal select[name="status"]', initCompleteDate);
  $(document).on('click', '.modal .task-reference-group .btn', function () { initReference($('.modal'), true); });
  $(document).on('shown.bs.modal', '.modal', function () {
    let modal = $(this);
    setTimeout(function () {
      const content = modal.find('.macro-editor');
      if (!content || content.length <= 0) {
        console.log('Modal content not found');
        return;
      }
      if (!content[0].classList.contains('loading')) {
        init();
        return;
      }
      const observer = new MutationObserver(() => {
        if (!content[0].classList.contains('loading')) {
          observer.disconnect();
          init();
        }
      });

      observer.observe(content[0], {
        attributes: true,
        attributeFilter: ['class'],
        subtree: true
      });
    });
  });
});
