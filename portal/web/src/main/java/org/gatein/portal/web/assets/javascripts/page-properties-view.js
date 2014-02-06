(function(Backbone, $, _, editorView) {
  
  var PageInfo = Backbone.Model.extend({
    
    initAttributes : function() {
      if (editorView.getPageView() != undefined) {
        var pageModel = editorView.getPageView().model;
        this.attributes = _.clone(pageModel.attributes);
      }
    },
    
    toPageModel : function () {
      if (editorView.getPageView() != undefined) {
        var pageModel = editorView.getPageView().model;
        pageModel.set(this.attributes);
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
      this.model = new PageInfo();
    },

    //TODO: need to refactor this
    accessPermissions: [],
    editPermissions: [],
    
    cancel : function() {
      this.$el.removeData('modal');
      $('#pagePropertiesModal').modal('hide');
      var editMode = editorView.model.get('editMode');
      if (editMode == editorView.EditorState.EDIT_NEW_PAGE) {
        editorView.model.set('editMode', editorView.EditorState.NORMAL);
      }
    },
    
    render : function() {
      this.model.initAttributes();

      var _this = this;
      var editMode = editorView.model.get('editMode');
      if (editMode == editorView.EditorState.EDIT_NEW_PAGE) {
        $.ajax({
          url : this.$el.attr('data-parentLinks'),
          dataType : 'json',
          success : function(data) {  
            var template = $("#page-properties-modal-template").html();
            var html = _.template(template, {parentLinks: data.parentLinks});
            _this.$el.find('.modal-body').html(html);
            _this.bindingForm();
            
            //TODO: Need to re-factory
            if (editorView.getPageView() != undefined) {
              var pageModel = editorView.getPageView().model;
              _this.accessPermissions = pageModel.get('accessPermissions');
              _this.editPermissions = pageModel.get('editPermissions');
            }
            //
          }
        });
      } else if (editMode == editorView.EditorState.EDIT_CURRENT_PAGE) {
        var template = $("#page-properties-modal-template").html();
        var pageModel = editorView.getPageView().model;
        var html = _.template(template, {parentLinks: [pageModel.get('parentLink')]});
        this.$el.find('.modal-body').html(html);
        this.bindingForm();
        //TODO: Need to re-factory
        this.accessPermissions = pageModel.get('accessPermissions');
        this.editPermissions = pageModel.get('editPermissions');
      }

      //Need load All group and membershipType
      var everyone = {
        //Default everyone can access to page
        access: (this.accessPermissions.length == 1 && this.accessPermissions[0] == 'Everyone') || (this.accessPermissions.length == 0),
        edit: (this.editPermissions.length == 1 && this.editPermissions[0] == 'Everyone') || (this.editPermissions.length == 0)
      };
      this.accessPermissions = _.without(this.accessPermissions, 'Everyone');
      this.editPermissions = _.without(this.editPermissions, 'Everyone');

      $.ajax({
        url: this.$el.attr('data-allGroupAndMembershipType'),
        dataType: 'json',
        success: function(data) {
          var template = $("#page-properties-modal-permissions").html();
          var html = _.template(template, {
            groups: data.groups,
            membershipTypes: data.membershipTypes,
            accessPermissions: _this.accessPermissions,
            editPermissions: _this.editPermissions,
            everyone: everyone
          });
          _this.$el.find('.modal-permissions').html(html);
        }
      });
    },
    
    bindingForm : function (editMode) {
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
      this.setPermissions();
    },
    
    updatePageInfo : function () {
      this.model.set('pageDisplayName', $(".modal-body input[name='pageDisplayName']").val());
      this.model.set('factoryId', $(".modal-body select[name='factoryId']").val());
      this.setPermissions();
    },
    
    setPermissions : function () {
     //Access permission
      if(this.$el.find('input[name="accessPermission"]').is(":checked")) {
        this.model.set('accessPermissions', ['Everyone']);
      } else {
        this.model.set('accessPermissions', this.accessPermissions);
      }

      //Edit permission
      if(this.$el.find('input[name="editPermission"]').is(":checked")) {
        this.model.set('editPermissions', ['Everyone']);
      } else {
        this.model.set('editPermissions', this.editPermissions);
      }
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
      this.$el.find('.modal-body .alert').remove();
      this.$el.find('.modal-body').prepend(alertBox);
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
      //Unique
      var existing = true;
      if($form.attr('name') == 'accessPermission' && !_.contains(this.accessPermissions, permission)) {
        existing = false;
        this.accessPermissions.push(permission);
      } else if($form.attr('name') == 'editPermission' && !_.contains(this.editPermissions, permission)) {
        existing = false;
        this.editPermissions.push(permission);
      }

      if(!existing) {
        var $permission = $('<li class="permission">' + permission + '</li>')
        $permissions.find('ul.list-permissions').append($permission);
      }

      $form.trigger('reset');
    },

    removePermission: function(e) {
      var $permission = $(e.target);
      var perm = $permission.html();

      var $form = $permission.closest("div.permissions").find('form.form-permission');
      if($form.attr('name') == 'accessPermission') {
        this.accessPermissions = _.without(this.accessPermissions, perm);
      } else if($form.attr('name') == 'editPermission') {
        this.editPermissions = _.without(this.editPermissions, perm);
      }


      $permission.remove();
    }
  });
  
  return PagePropertiesModal;
})(Backbone, $, _, editorView);