/**
 * @type {Cypress.PluginConfig}
 */
let server; // static reference to the mock server
// so we can close and re-assign on 2nd call

module.exports = (on, config) => {
  on('task', {
    mockServer({ interceptUrl, fixture }) {
      const fs = require('fs');
      const http = require('http');
      const { URL } = require('url');

      if (server) server.close(); // close any previous instance

      const url = new URL(interceptUrl);
      //creating server to intercept backend calls with pre-defined fixtures
      server = http.createServer((req, res) => {
        if (req.url === url.pathname) {
          const data = fs.readFileSync(`./cypress/fixtures/${fixture}`);
          res.end(data);
        } else {
          res.end();
        }
      });

      server.listen(url.port);
      console.log(`listening at port ${url.port}`);

      return null;
    },
  });
};
