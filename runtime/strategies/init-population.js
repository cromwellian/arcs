// Copyright (c) 2017 Google Inc. All rights reserved.
// This code may only be used under the BSD style license found at
// http://polymer.github.io/LICENSE.txt
// Code distributed by Google as part of this project is also
// subject to an additional IP rights grant found at
// http://polymer.github.io/PATENTS.txt

import {Strategy} from '../../strategizer/strategizer.js';
import {assert} from '../../platform/assert-web.js';

export class InitPopulation extends Strategy {
  constructor(arc, {contextual = false}) {
    super();
    this._arc = arc;
    this._contextual = contextual;
    this._loadedParticles = new Set(this._arc.loadedParticles().map(spec => spec.implFile));
  }

  async generate({generation}) {
    if (generation != 0) {
      return [];
    }

    await this._arc.recipeIndex.ready;
    const results = this._contextual
        ? this._contextualResults()
        : this._allResults();

    return results.map(({recipe, score = 1}) => ({
      result: recipe,
      score,
      derivation: [{strategy: this, parent: undefined}],
      hash: recipe.digest(),
      valid: Object.isFrozen(recipe)
    }));
  }

  _contextualResults() {
    const results = [];
    for (const slot of this._arc.activeRecipe.slots.filter(s => s.sourceConnection)) {
      results.push(...this._arc.recipeIndex.findConsumeSlotConnectionMatch(slot).map(
          ({slotConn}) => ({recipe: slotConn.recipe})));
    }
    let innerArcHandles = [];
    for (const recipe of this._arc._recipes) {
      for (const innerArc of [...recipe.innerArcs.values()]) {
        innerArcHandles = innerArcHandles.concat(innerArc.activeRecipe.handles);
      }
    }
    for (const handle of this._arc.activeRecipe.handles.concat(innerArcHandles)) {
      results.push(...this._arc.recipeIndex.findHandleMatch(handle, ['use', '?']).map(
          otherHandle => ({recipe: otherHandle.recipe})));
    }
    return results;
  }

  _allResults() {
    return this._arc.recipeIndex.recipes.map(recipe => ({
      recipe,
      score: 1 - recipe.particles.filter(
          particle => particle.spec && this._loadedParticles.has(particle.spec.implFile)).length
    }));
  }
}
