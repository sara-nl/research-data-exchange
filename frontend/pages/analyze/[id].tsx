import NavBarComponent from '../../components/navBar';
import Lab from '../../components/lab';
import Footer from '../../components/footer';
import Head from 'next/head';
import { Dataset } from '../../types';
import { GetServerSideProps } from 'next';
import Error, { ErrorProps } from 'next/error';
import absoluteUrl from 'next-absolute-url';
import { useState } from 'react';

type Props = {
  rdxDataset?: Dataset;
  submitUrl: string;
  baseUrl?: string;
  token: string;
  error?: ErrorProps;
};


const Analyze: React.FC<Props> = ({ rdxDataset, submitUrl, token, error }) => {

  if (error) {
    return <Error {...error} title={error.title} />;
  }

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
      <NavBarComponent email="Analyst" />
      <Lab dataset={rdxDataset} submitUrl={submitUrl} token={token}  />
      <Footer />
    </main>
  );
};

export const getServerSideProps: GetServerSideProps = async (context) => {
  const id = context.query['id'];
  const token = context.query['token'];

  const { origin } = absoluteUrl(context.req);

  const obj = {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
    }
  }

  const res = await fetch(`${process.env.RDX_BACKEND_URL}/api/lab/${id}`, obj);

  if (!res.ok) {
    const body = await res.json()
    return {
      props: { error: { statusCode: res.status, title: body.detail } },
    };
  }

  return {
    props: {
      rdxDataset: await res.json(),
      submitUrl: `${process.env.RDX_BACKEND_URL}/api/lab/${id}/job`,
      token: token
    },
  };
};

export default Analyze;
