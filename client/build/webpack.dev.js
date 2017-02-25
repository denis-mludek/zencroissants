var webpack = require('webpack')
var ExtractTextPlugin = require('extract-text-webpack-plugin')
var path = require("path")

module.exports = {
  context: path.resolve(__dirname, '../'),

  entry: './css/main.sass',

  output: {
    path: '../server/public/javascripts',
    filename: '[name].js'
  },

  resolve: {
    extensions: ['.sass']
  },

  module: {
    rules: [
      { 
        test: /\.sass$/,
        exclude: /node_modules/,
        use: ExtractTextPlugin.extract({
          use: ['css-loader', 'sass-loader']
        })
      }
    ]
  },

  devtool: 'source-map',

  plugins: [
    new ExtractTextPlugin('../stylesheets/main.css')
  ]
};