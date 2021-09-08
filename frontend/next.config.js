module.exports = {
  future: {
    webpack5: true,
  },
  // images: {
  //   loader: 'imgix',
  //   path: 'https://noop/',
  // },

  // Here we redefine vars from .env files to make them available on client side
  env: {
    PDF_HEADERS_PROXY_URL: process.env.RDX_PDF_HEADERS_PROXY_URL,
    BACKEND_URL: process.env.RDX_BACKEND_URL,
  },
  rewrites: async () => [
    {
      source: '/api/:path*',
      destination: process.env.RDX_BACKEND_URL + '/:path*',
    },
  ],
  webpack: (config) => {
    // load worker files as a urls with `file-loader`
    config.module.rules.unshift({
      test: /pdf\.worker\.(min\.)?js/,
      use: [
        {
          loader: 'file-loader',
          options: {
            name: '[contenthash].[ext]',
            publicPath: '/_next/static/worker',
            outputPath: 'static/worker',
          },
        },
      ],
    });

    return config;
  },
};
