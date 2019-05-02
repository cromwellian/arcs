/*
@license
Copyright (c) 2018 The Polymer Project Authors. All rights reserved.
This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
Code distributed by Google as part of the polymer project is also
subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
*/

import Xen from '../xen/xen.js';

let template = Xen.html`
<video id="player" controls autoplay style="display:none" width="400" height="300"></video>
<canvas id="canvas" width="400" height = "300"></canvas>
<button id="capturebutton" on-click="capture">Take Picture</button>
`;
template = Xen.Template.createTemplate(template);

//const log = Xen.logFactory('MicInput', 'blue');

class CameraInput extends Xen.Base {

  get template() {
    return template;
  }

  _didMount() {
    this.player = this.host.getElementById('player');
    this.canvas = this.host.getElementById('canvas');
    this.ctx = this.canvas.getContext('2d');
    const constraints = {
      video: true,
    };
    navigator.mediaDevices.getUserMedia(constraints)
      .then((stream) => {
      this.player.srcObject = stream;
  });
  }
  _update(props, state) {
  }

  _render(props, state) {
    return state;
  }

  start() {

  }

  stop() {

  }

  capture() {
      // Draw whatever is in the video element on to the canvas.
      this.ctx.drawImage(this.player, 0, 0);
      const imageData = this.ctx.getImageData(0,0, 400, 300);
      this.value = {pixels: imageData.data, width: imageData.width, height: imageData.height,
        url: this.canvas.toDataURL("image/png")};
      this._fire('capture', this.value);
  }

}
customElements.define('camera-input', CameraInput);
