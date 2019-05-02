/**
 * Copyright (c) 2019 Google Inc. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 * Code distributed by Google as part of this project is also
 * subject to an additional IP rights grant found at
 * http://polymer.github.io/PATENTS.txt
 */

'use strict';

/* global defineParticle */
defineParticle(({DomParticle, html, log}) => {

  const template = html`
    <camera-input on-capture="onCapture"></camera-input>
    <div style="padding: 16px;">
      <h2>Arcs Image Processing Demo</h2>
      <h3>Input an image url</h3>
      <input style="width: 80%; padding: 8px;" type="text" value="{{url}}"  on-change="onChange">
      <button on-click="onSubmit">Submit</button>
      <br><br>
      <img src="{{url}}">
      <div slotid="imageView"></div>
    </div>
  `;

  return class extends DomParticle {
    get template() {
      return template;
    }

    render(props, state) {
      if (!this.state.url) {
        this.state.url = "http://localhost:8786/particles/Processing/assets/kitten.jpg";
      }
      return state;
    }

    onChange({data: {value}}) {
      this.setState({url: value});
    }

    onSubmit() {
      const url = this.state.url;
      this.updateVariable('image', {url: this.state.url});
      // this.updateVariable('blob', this.state.blob);
      this.setState({url});
    }

    onCapture(data) {
      const {pixels, width, height, url} = data.data.value;
      this.setState({url: url, blob: {
          blob: new Uint8Array(pixels.buffer),
          width: width,
          height: height
        }});
    }
  };
});
