import React from 'react';
import NavBarComponent from '../../components/navBar';
import Publication from '../../components/publication';
import Footer from '../../components/footer';
import Head from 'next/head';
import { Share } from '../../types';
import { GetServerSideProps } from 'next';
import Error, { ErrorProps } from 'next/error';
import absoluteUrl from 'next-absolute-url';

type Props = { share?: Share; submitUrl: string; baseUrl?: string; error?: ErrorProps };

const Publish: React.FC<Props> = ({ share, baseUrl, submitUrl, error }) => {
  if (error) {
    return <Error {...error} title={error.title} />;
  } else {
    return (
      <main>
        <Head>
          <link
            rel="shortcut icon"
            href="/images/rdx-logo.png"
            type="image/x-icon"
          />
          <title>RDX</title>
        </Head>
        <NavBarComponent owner={share.owner} />
        <Publication share={share} submitUrl={submitUrl} baseUrl={baseUrl} />
        <Footer />
      </main>
    );
  }
};

export const getServerSideProps: GetServerSideProps = async (context) => {
  const token = context.query['token'];
  const { origin } = absoluteUrl(context.req);

  if (!token) {
    return {
      props: {
        error: {
          statusCode: 400,
          title: 'Invalid URL',
        },
      },
    };
  }

  const res = await fetch(process.env.RDX_BACKEND_URL + `/share/` + token);

  if (!res.ok) {
    return {
      props: { error: { statusCode: res.status, title: await res.json() } },
    };
}

  return { props: { share: await res.json(), baseUrl: origin, submitUrl :"/api/dataset/" + token } };
};

export default Publish;
