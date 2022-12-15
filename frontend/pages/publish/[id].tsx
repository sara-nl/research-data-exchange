import NavBarComponent from '../../components/navBar';
import Publication from '../../components/publication';
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
  metadataUrl: string;
  baseUrl?: string;
  token: string;
  error?: ErrorProps;
};


const Publish: React.FC<Props> = ({ rdxDataset, baseUrl, submitUrl, metadataUrl, token, error }) => {
  const [dataset, setDataset] = useState<Dataset>(rdxDataset)
  const updateDataset = (dataset: Dataset) => setDataset(dataset)

  if (error) {
    return <Error {...error} title={error.title} />;
  }

  const userEmail = dataset.rdx_share.additional_info_owner || dataset.rdx_share.uid_owner || null

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
      <NavBarComponent email={userEmail} />
      <Publication dataset={dataset} submitUrl={submitUrl} metadataUrl={metadataUrl} baseUrl={baseUrl} token={token} updateDataset={updateDataset} />
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

  const res = await fetch(`${process.env.RDX_BACKEND_URL}/api/dataset/${id}`, obj);

  if (!res.ok) {
    const body = await res.json()
    return {
      props: { error: { statusCode: res.status, title: body.detail } },
    };
  }

  return {
    props: {
      rdxDataset: await res.json(),
      baseUrl: origin,
      submitUrl: `${process.env.RDX_BACKEND_URL}/api/dataset/${id}`,
      metadataUrl: `${process.env.RDX_BACKEND_URL}/api/metadata`,
      token: token
    },
  };
};

export default Publish;
