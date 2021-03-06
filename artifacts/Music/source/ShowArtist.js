// @license
// Copyright (c) 2018 Google Inc. All rights reserved.
// This code may only be used under the BSD style license found at
// http://polymer.github.io/LICENSE.txt
// Code distributed by Google as part of this project is also
// subject to an additional IP rights grant found at
// http://polymer.github.io/PATENTS.txt

'use strict';

defineParticle(({DomParticle, html}) => {

  const host = `[show-artist]`;

  const styles = html`
  <style>
    ${host} {
      border-radius: 16px;
      border: 1px solid #ddd;
      overflow: hidden;
      margin: 8px;
    }
    ${host} [header] {
      min-height: 35vh;
      background: cadetblue;
      display: flex;
      flex-direction: column;
      justify-content: flex-end;
      padding: 24px;
      font-size: 24px;
      color: white;
      position: relative;
      z-index: 0;
    }
    ${host} [header] [cover] {
      position: absolute;
      top: 0;
      right: 0;
      bottom: 0;
      left: 0;
      z-index: -1;
    }
    ${host} [header] [photo] {
      background-size: cover;
      background-repeat: no-repeat;
      background-position: center center;
    }
    ${host} [header] [scrim] {
      background: linear-gradient(to top, rgba(0,0,0,0.5) 0%,rgba(0,0,0,0) 100%);
    }
    ${host} [header] [name] {
      margin: 4px 0 8px;
    }
    ${host} [header] [description] {
      font-size: 16px;
    }
    ${host} [now-playing] {
      border-top: 1px solid #ddd;
      padding: 24px 24px 16px;
    }
    ${host} [now-playing] [list] {
      margin: 0 -8px;
    }
  </style>
  `;

  const template = html`
<div show-artist>
${styles}
  <div header>
    <div cover photo style={{photoStyle}}></div>
    <div cover scrim></div>
    <div description>{{description}}</div>
    <div name>{{name}}</div>
    <div description>{{detailedDescription}}</div>
  </div>
  <div slotid="nearbyShows"></div>
  <div now-playing>
    From <b>Now playing</b>
    <div list slotid="nowPlayingList"></div>
  </div>
  <div slotid="extrasForArtist"></div>
</div>
  `;
  return class extends DomParticle {
    get template() {
      return template;
    }
    shouldRender(props) {
      return Boolean(props.artist);
    }
    willReceiveProps(props) {
      if (props.artistPlayHistory.length && props.artist) {
        let mostRecent = props.artistPlayHistory[0];
        for (const song of props.artistPlayHistory) {
          if (Number(song.dateTime) > Number(mostRecent.dateTime)) mostRecent = song;
        }
        this.setParticleDescription({
            template: `You listened to <b>${mostRecent.song}</b> by <b>${props.artist.name}</b> ${this._formatTime(Number(mostRecent.dateTime))}`,
            model: {}
        });
      }
    }

    _formatTime(dateTime) {
      const delta = Date.now() - dateTime;
      if (delta < 60 * 60 * 1000) {
        let minutes =  Math.round(delta / (60 * 1000));
        if (minutes === 0) minutes = 1;
        return `${minutes} minute${minutes === 1 ? '' : 's'} ago`;
      } else if (delta < 24 * 60 * 60 * 1000) {
        const hours =  Math.round(delta / (60 * 60 * 1000));
        return `${hours} hour${hours === 1 ? '' : 's'} ago`;
      } else {
        return `on ${new Date(Number(dateTime)).toLocaleDateString()}`;
      }
    }
    render({artist}) {
      return {
        name: artist.name,
        description: artist.description,
        detailedDescription: artist.detailedDescription || '',
        photoStyle: {
          backgroundImage: artist.imageUrl ? `url(${artist.imageUrl})` : 'none'
        },
      };
    }
  };
});
