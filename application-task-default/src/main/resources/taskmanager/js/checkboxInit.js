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
  // Generate the id on the client side because the macro editor caches the HTML of the parameter displayers, so an
  // id generated on the server side would be the same for every new taskbox macro.
  let generateId = function () {
    return String(Math.random()).slice(-5);
  };
  let init = function (container) {
    $(container).find('.taskbox-id').each(function () {
      if (!this.value) {
        this.value = generateId();
      }
    });
  };
  // The macro editor fires this event once it has inserted the parameter fields, which can be after the modal is shown.
  $(document).on('xwiki:dom:updated', function (event, data) {
    init(data ? data.elements : document.body);
  });
  // The macro editor might have been displayed before this code was loaded.
  init(document.body);
  $(document).on('click', '.taskbox-id-group .btn', function () {
    $(this).closest('.taskbox-id-group').find('.taskbox-id').val(generateId()).trigger('change');
  });
});
