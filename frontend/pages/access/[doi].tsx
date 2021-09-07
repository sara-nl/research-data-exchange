import NavBarComponent from '../../components/navBar';
import AccessComponent from '../../components/access';
import Footer from '../../components/footer';
import { Dataset } from '../../types';
import Error, { ErrorProps } from 'next/error';
import Head from 'next/head';
import { GetServerSideProps } from 'next';

type Props = {
  dataset?: Dataset;
  submitUrl: string;
  error?: ErrorProps;
};

const Access: React.FC<Props> = ({ dataset, submitUrl, error }) => {
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
        <NavBarComponent owner={dataset.owner} />
        <AccessComponent dataset={dataset} submitUrl={submitUrl} />
        <Footer />
      </main>
    );
  }
};

function transformDownloadLink(conditionsUrl: string) {
  // take the last element of the path to rewrite the url
  try {
    const path = new URL(conditionsUrl).pathname.split('/').reverse()[0];
    if (path) {
      return process.env.DOWNLOAD_PROXY + path + '/download';
    }
  } catch (error) {
    console.error('Conditions url is not valid', error);
  }
  return '';
}

export const getServerSideProps: GetServerSideProps = async (context) => {
  const doi = context.query['doi'] as string;

  const res = await fetch(
    process.env.BACKEND_URL + `/dataset/` + encodeURIComponent(doi),
  );

  if (!res.ok) {
    return {
      props: { error: { statusCode: res.status, title: await res.statusText } },
    };
  }

  const dataset = await res.json();
  dataset.conditionsUrl = transformDownloadLink(dataset.conditionsUrl);

  return {
    props: {
      dataset: dataset,
      submitUrl: '/api/dataset/' + encodeURIComponent(doi),
    },
  };
};
export default Access;
