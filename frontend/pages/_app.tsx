import { AppProps } from 'next/dist/next-server/lib/router/router';

import '../scss/main.scss';
import '../scss/reset.scss';

function MyApp({ Component, pageProps }: AppProps): JSX.Element {
  return <Component {...pageProps} />;
}

export default MyApp;
