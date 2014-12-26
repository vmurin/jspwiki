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
Class: Dialog.Font
	The Dialog.Font is a Dialog.Selection object, to selecting a font.
	Each selectable item is redered in its proper font.

Arguments:
	options - optional, see options below

Options:
	fonts - (object) set of font definitions with name/value
	others - see Dialog.Selection options

Inherits from:
	[Dialog.Selection]

Example
	(start code)
	dialog= new Dialog.Font({
		fonts:{'Font name1':'font1', 'Font name2':'font2'},
		caption:"Select a Font",
		onSelect:function(value){ alert( value ); }
	});
	(end)
*/
Dialog.Font = new Class({

	Extends:Dialog.Selection,
	
	options: {
		fonts: {
			'arial':'Arial',
			'comic sans ms':'Comic Sans',
			'courier new':'Courier New',
			'garamond':'Garamond',
			'georgia':'Georgia',
			'helvetica':'Helvetica',
			'impact':'Impact',
			'times new roman':'Times',
			'tahoma':'Tahoma',
			'trebuchet ms':'Trebuchet',
			'verdana':'Verdana'
		}
	},

	initialize:function(options){

		var self = this, fonts = options.fonts;

        //options.cssClass = '.font'+(options.cssClass||'')
        this.setClass('.font',options);
		options.body = fonts ? fonts : self.options.fonts;

		self.parent(options);

		self.getItems().each(function(li){
			li.setStyle('font-family', li.get('title') );
		});

	}

});
