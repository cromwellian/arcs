/**
 * Copyright (c) 2019 Google Inc. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 * Code distributed by Google as part of this project is also
 * subject to an additional IP rights grant found at
 * http://polymer.github.io/PATENTS.txt
 */

'use strict';

defineParticle(({DomParticle, html, log}) => {
  const tmpl = html`
  <div style="padding: 16px;">
    <h3>Input the path/to/style-transfer/model/</h3> 
    <input style="width: 80%; padding: 8px;" on-change="onChange">
    <h5 style="margin: 8px 0;">Please choose the folder where the model is located.</h5>
    <button on-click="onSubmit">Submit</button>
    <br/>
    <p>Path: {{model}}</p>
  </div>
  `;

  return class extends DomParticle {
    get template() {
      return tmpl;
    }
    render(props, state) {
      return state;
    }
    onChange({data: {value}}) {
      this.setState({inputModelUrl: value});
    }
    onSubmit() {
      const model = this.state.inputModelUrl;
      this.updateVariable('model', {model});
      this.setState({model});
    }
  };

});
