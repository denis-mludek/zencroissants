/* global module */
module.exports = {
  'rules': {
    'indent': [
      2,
      2,
      {
        'SwitchCase': 1
      }
    ],
    'quotes': [
      2,
      'single'
    ],
    'linebreak-style': [
      2,
      'unix'
    ],
    'semi': [
      2,
      'always'
    ]
  },
  'env': {
    'es6': true,
    'browser': true
  },
  'parser': 'babel-eslint',
  'extends': 'eslint:recommended',
  'ecmaFeatures': {
    'jsx': true,
    'experimentalObjectRestSpread': true,
    'impliedStrict': true,
    'modules': true
  },
  'plugins': [
    'react'
  ],
  'globals': {
    'Router': true
  }
};
