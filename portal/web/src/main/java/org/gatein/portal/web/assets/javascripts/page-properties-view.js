(function(Backbone, $, _, editorView) {

  var PageInfo = Backbone.Model.extend({

    defaults: {
      everyone: {access: true, edit: false}
    },

    initialize: function(attributes, options) {

      var attributes = attributes || {};

      //Process everyone permission
      attributes.everyone = {access: true, edit: false};
      if(attributes.accessPermissions) {
        if(attributes.accessPermissions.length == 0 || (attributes.accessPermissions.length == 1 && attributes.accessPermissions[0] === 'Everyone')) {
          attributes.everyone.access = true;
        } else {
          attributes.everyone.access = false;
        }
        attributes.accessPermissions = _.without(attributes.accessPermissions, 'Everyone');
      }

      if(attributes.editPermissions && attributes.editPermissions.length == 1 && attributes.editPermissions[0] === 'Everyone') {
        attributes.everyone.edit = true;
        attributes.editPermissions = _.without(attributes.editPermissions, 'Everyone');
      }

      this.set(attributes, options);
    },

    setEveryonePermission: function(attr, val) {
      var attr = attr === 'accessPermission' ? 'access' : 'edit';
      var everyone = this.get('everyone');
      everyone[attr] = val;
    },
    addPermission: function(attr, permission) {
      if(!attr || (attr != 'accessPermissions' && attr != 'editPermissions')) {
        return false;
      }

      var permissions = this.get(attr) || [];
      if(permission && !_.contains(permissions, permission)) {
        permissions.push(permission);
        this.set(attr, permissions);
        return true;
      }

      return false;
    },
    removePermission: function(attr, permission) {
      if(!attr || (attr != 'accessPermissions' && attr != 'editPermissions')) {
        return false;
      }

      var permissions = this.get(attr) || [];
      if(permission && _.contains(permissions, permission)) {
        _.without(permissions, permission);
        this.set(attr, permissions);
        return true;
      }

      return false;
    },

    toPageModel : function () {
      //Process permission before bind
      var everyone = this.get('everyone');
      if(everyone.access) {
        this.set('accessPermissions', ['Everyone']);
      }
      if(everyone.edit) {
        this.set('editPermissions', ['Everyone']);
      }

      if (editorView.getPageView() != undefined) {
        var pageModel = editorView.getPageView().model;

        var attrs = _.clone(this.attributes);
        delete attrs.everyone;
        pageModel.set(attrs);

        return pageModel;
      }
      return undefined;
    }
  });
  
  var PagePropertiesModal = Backbone.View.extend({
    events : { 
      "click .cancel" : "cancel",
      "click .next" : "nextStep",
      'click a.permission-nav': 'changePermissionTab',
      'change input[type="checkbox"].everyone': "changePermission",
      "submit form.form-permission": "addPermission",
      "click li.permission": "removePermission"
    },
    
    initialize : function () {
      var attrs = {};
      if(editorView.getPageView() != undefined) {
        attrs = _.clone(editorView.getPageView().model.attributes);
      }
      this.model = new PageInfo(attrs);
    },
    
    cancel : function() {
      this.$el.removeData('modal');
      $('#pagePropertiesModal').modal('hide');
      var editMode = editorView.model.get('editMode');
      if (editMode == editorView.EditorState.EDIT_NEW_PAGE) {
        editorView.model.set('editMode', editorView.EditorState.NORMAL);
      }
    },
    
    render : function() {
      var _this = this;
      var editMode = editorView.model.get('editMode');

      //How to render page properties
      if (editMode == editorView.EditorState.EDIT_NEW_PAGE) {
        $.ajax({
          url : this.$el.attr('data-parentLinks'),
          dataType : 'json',
          success : function(data) {  
            var template = $("#page-properties-modal-template").html();
            var html = _.template(template, {parentLinks: data.parentLinks});
            _this.$el.find('.modal-body').html(html);
            _this.bindingForm();
          }
        });
      } else if (editMode == editorView.EditorState.EDIT_CURRENT_PAGE) {
        var template = $("#page-properties-modal-template").html();
        var pageModel = editorView.getPageView().model;
        var html = _.template(template, {parentLinks: [pageModel.get('parentLink')]});
        this.$el.find('.modal-body').html(html);
        this.bindingForm();
      }

      //Tab permissions
      $.ajax({
        url: this.$el.attr('data-allGroupAndMembershipType'),
        dataType: 'json',
        success: function(data) {
          var template = $("#page-properties-modal-permissions").html();
          var html = _.template(template, {
            groups: data.groups,
            membershipTypes: data.membershipTypes,
            accessPermissions: _this.model.get('accessPermissions'),
            editPermissions: _this.model.get('editPermissions'),
            everyone: _this.model.get('everyone')
          });
          _this.$el.find('.modal-permissions').html(html);
        }
      });
    },
    
    bindingForm : function (editMode) {
      var editMode = editorView.model.get('editMode');
      this.$el.find("input[name='pageName']").val(this.model.get('pageName')).prop('disabled', (editMode == editorView.EditorState.EDIT_CURRENT_PAGE));
      this.$el.find("input[name='pageDisplayName']").val(this.model.get('pageDisplayName'));
      this.$el.find("select[name='parentLink']").val(this.model.get('parentLink')).prop('disabled', (editMode == editorView.EditorState.EDIT_CURRENT_PAGE));
      this.$el.find("select[name='factoryId']").val(this.model.get('factoryId'));
    },
    
    initPageInfo : function () {
      this.model.set("id", "newpage"); 
      this.model.set("factoryId", $(".modal-body select[name='factoryId']").val()); 
      this.model.set("pageKey", "portal::classic::" + $(".modal-body input[name='pageName']").val()); 
      this.model.set("pageName", $(".modal-body input[name='pageName']").val());
      this.model.set("pageDisplayName", $(".modal-body input[name='pageDisplayName']").val());
      this.model.set("parentLink", $(".modal-body select[name='parentLink']").val());
    },
    
    updatePageInfo : function () {
      this.model.set('pageDisplayName', $(".modal-body input[name='pageDisplayName']").val());
      this.model.set('factoryId', $(".modal-body select[name='factoryId']").val());
    },

    nextStep : function() {
      var editMode = editorView.model.get('editMode');
      if (editMode == editorView.EditorState.EDIT_CURRENT_PAGE) {
        var pageModel = editorView.getPageView().model;
        this.updatePageInfo();
        this.model.toPageModel();
        this.$el.modal('hide');
        editorView.getComposerView().setFactoryId();
      } else if (editMode == editorView.EditorState.EDIT_NEW_PAGE) {
        var pageNameInput = this.$el.find("input[name='pageName']");
        if (this.verifyPageName(pageNameInput)) {
          var _this = this;
          $.ajax({
            url : _this.$el.attr('data-checkpage-url'),
            dataType : "json",
            data : {
              pageName : $(pageNameInput).val()
            },
            success : function(data) {
              if (data.pageExisted) {
                _this.message("Page is existed");
                $(pageNameInput).select();
              } else {
                require(['layout-view', 'composer-view'], function(LayoutView, ComposerView){
                  _this.initPageInfo();
                  if (!editorView.isEditing()) {
                    editorView.switchMode(LayoutView, ComposerView);
                    //clear apps
                    var pageModel = editorView.getPageView().model;
                    var containers = pageModel.getChildren();
                    $(containers).each(function() {
                      if (!this.isEmpty()) {
                        var container = this;
                        var apps = this.getChildren();
                        $(apps).each(function() {
                          container.removeChild(this);
                        });
                      }
                    });
                  }
                  
                  _this.model.toPageModel();
                  _this.$el.modal('hide');
                  editorView.getComposerView().setFactoryId();
                });
              }
            }
          });
        }
      }
    },
    
    verifyPageName : function(input) {
      var regex = new RegExp('^[a-zA-Z0-9._-]{3,120}$');
      var pageName = $(input).val();
      if (!pageName) {
        this.message("Page name is required!");
        setTimeout(function(){
          $(input).select();
        }, 0);
        return false;
      }
      if (!regex.test(pageName)) {
        this.message("Only alpha, digit, dash and underscore characters (3 - 120) allowed for page name.");
        //workaround to select input
        setTimeout(function(){
          $(input).select();
        }, 0);
        return false;
      }
      return true;
    },
    
    message : function(msg) {
      var alertBox = $("<div class='alert alert-error'></div>")
      alertBox.text(msg);
      this.$el.find('.modal-messages .alert').remove();
      this.$el.find('.modal-messages').prepend(alertBox);
    },

    changePermissionTab: function(e) {
      e.preventDefault();
      var $target = $(e.target);
      var $li = $target.closest('li');
      var $ul = $li.closest('ul');
      var $pagePermissions = $ul.closest('div#pagepermissions');

      $ul.find('li.active').removeClass('active');
      $li.addClass('active');

      $pagePermissions.find('div.row-permissions').addClass('hide');
      $pagePermissions.find($target.attr('href')).removeClass('hide');

      return false;
    },

    changePermission: function(e) {
      var $target = $(e.target);
      this.model.setEveryonePermission($target.attr('name'), $target.is(':checked'));
      var $permissions = $target.closest('div.row-permissions').find('div.permissions');
      $permissions.toggleClass('hide');
    },

    addPermission: function(e) {
      e.preventDefault();

      var $form = $(e.target);
      var $permissions = $form.closest('div.permissions');

      var group = $permissions.find('select[name="group"]').val();
      var membership = $permissions.find('select[name="membershipType"]').val();
      if(group == '' || membership == '') {
        return;
      }

      var permission = membership + ":" + group;
      var permissionAttr = $form.attr('name');
      if(this.model.addPermission(permissionAttr, permission)) {
        var $permission = $('<li class="permission">' + permission + '</li>')
        $permissions.find('ul.list-permissions').append($permission);
      }

      $form.trigger('reset');
    },

    removePermission: function(e) {
      var $permission = $(e.target);
      var perm = $permission.html();

      var $form = $permission.closest("div.permissions").find('form.form-permission');
      var permissionAttr = $form.attr('name');
      if(this.model.removePermission(permissionAttr, perm)) {
        $permission.remove();
      }
    }
  });
  
  return PagePropertiesModal;
})(Backbone, $, _, editorView);