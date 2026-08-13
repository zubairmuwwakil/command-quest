/*
 * Tests for the parts of docs/app.js that can be reasoned about on their own:
 * the starting tree, and the rule deciding which entries the sidebar shows.
 *
 * The sidebar matters to the easter egg as much as ls does. ViewCommand can
 * filter .team out of every listing it prints and the secret still dies the
 * moment the file tree on the left draws it anyway - so the same hidden rule
 * has to hold on both sides, and this is the half that lives in JavaScript.
 *
 * Run with:
 *
 *   node --test test/app.test.js
 */
'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');

const { SEED, visible } = require('../docs/app.js');

test('visible() hides dot-prefixed entries', () => {
  assert.deepEqual(
    visible(['todo.md', '.team', 'photos'], false),
    ['photos', 'todo.md']);
});

test('visible() reveals everything when asked', () => {
  assert.deepEqual(
    visible(['todo.md', '.team', 'photos'], true),
    ['.team', 'photos', 'todo.md']);
});

test('visible() sorts what it returns', () => {
  assert.deepEqual(visible(['c', 'a', 'b'], false), ['a', 'b', 'c']);
});

test('visible() accepts the keys of a subFolders object', () => {
  // The tree passes Object.keys(...) here, not an array literal.
  const subFolders = { photos: {}, '.team': {} };
  assert.deepEqual(visible(Object.keys(subFolders), false), ['photos']);
});

test('the starting tree hides the team folder', () => {
  assert.ok(SEED.subFolders['.team'], 'the team folder is seeded');
  assert.deepEqual(
    visible(Object.keys(SEED.subFolders), false),
    [],
    'but a player who has not found -a sees no folders at all');
});

test('the team folder holds a file for each teammate', () => {
  assert.deepEqual(
    SEED.subFolders['.team'].files.slice().sort(),
    ['armando.md', 'seun.md', 'victoria.md']);
});
