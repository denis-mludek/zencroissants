var webpack = require('webpack')
var ExtractTextPlugin = require('extract-text-webpack-plugin')

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

  plugins: [
    new ExtractTextPlugin('../stylesheets/main.css'),
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': '"production"'
    }),
    new webpack.optimize.UglifyJsPlugin({
      compress: { warnings: false },
      output: { comments: false },
      minimize: true,
      sourceMap: false
    })
  ],

  devtool: false
};
