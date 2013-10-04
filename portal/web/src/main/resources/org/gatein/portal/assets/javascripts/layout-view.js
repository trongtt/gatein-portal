(function() {
	var  LayoutView = Backbone.View.extend({
		el : '.editing',
		
        initialize : function() {
        	var editing = this.$el.hasClass("editing");
        	var view = this;
        	if (editing) {
        		this.$( ".sortable" ).sortable({
        			placeholder: "portlet-placeholder",
        			revert: true,
        			update : function(event, ui) {
        	    		var cont = view.model.getDescendant(this.id);
        	    		var prev = $(ui.item).prev('.portlet');
        	    		var idx = prev.length ? $('#' + cont.getId() + ' > .portlet').index(prev.get(0)) + 1 : 0;
        	    		
        	    		cont.addChild(ui.item.attr('id'), {at : idx});
        	    	}
        		});
        	}
        	this.listenTo(this.model, 'addChild.eXo.Container', this.onAddChild);
        	this.listenTo(this.model, 'removeChild.eXo.Container', this.onRemoveChild);
        },
        
        onAddChild : function(child, container) {
        	var $app = $('#' + child.getId());
        	var prev = container.at(child.getIndex() - 1);
        	if (prev) {
        		$app.insertAfter($('#' + prev.getId()));
        	} else {
        		var $cont = $('#' + container.getId());
        		$cont.prepend($app);
        	}
        },
        
        onRemoveChild : function(child, container) {
        	var $app = $('#' + child.getId());
        	$app.remove();
        }
	});
	
	//Bootstrap view and model of the editor 
	$(function() {
		var root = $('.editing');
		var container = new Container();
		$('.sortable').each(function() {
			var cont = new Container({id : this.id});			
			$(this).children('.portlet').each(function() {			
				var app = new Application({'id' : this.id});
				cont.addChild(app);
			});
			container.addChild(cont);
		});
		
		window.layoutView = new LayoutView({model : container});
		
		//switch layout
		$('.switch').each(function() {
			var href = $(this).attr('href');
			$(this).on('click', function(e) {
				e.preventDefault();
				$.ajax({
						url : href,
						dataType : "html",
						success: function(data) {
							var newContainer = new Container();
							$(data).find('.sortable').each(function() {
								var cont = new Container({id : this.id});
								newContainer.addChild(cont);
							});
							
							var row = $('.row');
							row.find('.sortable').each(function() {
								var id = $(this).attr('id');
								$(this).attr('id', id + '-old');
							});
							row.addClass('removeable');
							$(row).after(data);
							
							var container = window.layoutView.model;
							window.layoutView = new LayoutView({model : newContainer});
							
							$(container.get("_childrens").models).each(function() {
									var apps = this.get("_childrens").models;
									var id = this.id;
									var cont = this;
									$(apps).each(function() {
										var newCont = newContainer.getChild(id);
										if (newCont) {
											newCont.addChild(this);
										} else {
											//Add applications into last container
											var lastId = newContainer.get("_childrens").length;
											newCont = newContainer.getChild(lastId);
											newCont.addChild(this);
										}
									});
							});
							
							//remove old container
							$('.removeable').remove();
						}
				});
			});
		});
		//end switch layout
		
	});
})();
