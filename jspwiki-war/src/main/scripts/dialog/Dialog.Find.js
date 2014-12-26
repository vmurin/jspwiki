/*
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); fyou may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
*/
/*
Function: Dialog.Find
	Perform the find and replace operation on either the full textarea
	or the selection of the textarea. It supports
	regular expressions, case-(in)sensitive replace and global replace.
	This is an event handler, typically linked with the submit button of the
	find and replace dialog.

Arguments:

DOM-structure:

*/
Dialog.Find = new Class({

	Extends: Dialog,
    Binds: ['find','replace'],

	options: {
	
		//dialog: mandatory DOM element, caption and body are not allowed
		draggable:true,
		controls:{
			f:  '[name=tbFIND]',
			r:  '[name=tbREPLACE]',
			h:  '.tbHITS', 
			re: '[name=tbREGEXP]',
			i:  '[name=tbMatchCASE]',
			one:'[name=replace]',
			all:'[name=replaceall]'
		},
		data:{
			get:function(){},
			set:function(){}
		}
	},

	initialize:function(options){

		var self = this.setOptions(options), 
		    dialog = self.options.dialog,
			controls; 

        this.setClass('.find',options);

		//convert to $(elements)
		controls = self.controls = Object.map( self.options.controls, function(el){
			return dialog.getElement(el);
		});

		self.parent( options );
		
		console.log("Dialog.Find initialize:",controls);
		
		controls.f.addEvents({ 
		    keyup: self.find, 
		    focus: self.find
		});
		dialog.addEvents({
		    'change:relay([type=checkbox])': function(){ controls.f.focus(); } ,
		    'click:relay(button)': self.replace  		
		}); 

	},

	show: function(){
		//1. make sure the find controls are visible
		this.parent();
		//2. focus the find input field, and auto-trigger find()
		this.controls.f.focus();
	},

	// keypress, focus
	find: function(){

		var self = this,
			controls = self.controls,
			find = controls.f,
			hits = controls.h,
			findText = find.value,
			result = '';

		if( findText != '' ){

			result = self.buildRE( findText );
			if( typeOf(result)=='regexp' ){
				result = self.options.data.get().match( self.buildRE(findText,true) );
				if(result){ result = result.length; }
			}

		}

		if( hits ){ hits.set('html',result); }
		
		controls.r.ifClass(!result,'disabled');		
		controls.one.ifClass(!result,'disabled');		
		controls.all.ifClass(!result,'disabled');
		//TODO : one,all should remain disabled until r has content ..	
			
		find.focus();
	},

	// click replace or replace-all button
	replace: function(e){

		var self = this,
			controls = self.controls,
			replace = controls.r,
			find = controls.f,
			data = self.options.data;
		
		data.set(
			data.get().replace(
				self.buildRE(find.value, e.target == controls.all), 
				replace ? replace.value : ''
			)
		);
		find.focus();
	},

	buildRE: function( findText, global ){

		var controls = this.controls,
			isRegExp = controls.re && controls.re.checked,
			reGlobal = global ? 'g':'',
			reMatchCase	= (controls.i && controls.i.checked) ? '':'i';

		try {
		
			return RegExp(
				isRegExp ? findText : findText.escapeRegExp(),
				reGlobal + reMatchCase + 'm'
			);

		} catch(e){

			return "<span title='" + e + "'>!#@</span>";

		}

	}

});
