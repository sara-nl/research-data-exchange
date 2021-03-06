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
        <NavBarComponent email="" />
        <AccessComponent dataset={dataset} submitUrl={submitUrl} />
        <Footer />
      </main>
    );
  }
};

function transformDownloadLink(conditionsUrl: string) {
  try {
    // take the last element of the path to rewrite the url
    const path = new URL(conditionsUrl).pathname
      .split('index.php')
      .reverse()[0];
    if (path) {
      return process.env.PDF_HEADERS_PROXY_URL + path + '/download';
    }
  } catch (error) {
    console.error('Conditions url is not valid', error);
  }
  return '';
}

export const getServerSideProps: GetServerSideProps = async (context) => {
  const doi = (context.query.doi as string[]) || [];

  const res = await fetch(
    process.env.BACKEND_URL + `/dataset/` + encodeURIComponent(doi.join('/')),
  );

  if (!res.ok) {
    return {
      props: { error: { statusCode: res.status, title: await res.statusText } },
    };
  }

  var response = await res.json();

  var dataset = {
    title: response.title,
    description: response.description,
    files: response.files,
    conditionsUrlProxy: transformDownloadLink(response.conditionsUrl),
    conditionsUrl: response.conditionsUrl + '/download',
  };

  return {
    props: {
      dataset: dataset,
      submitUrl: '/api/access/' + encodeURIComponent(doi.join('/')),
    },
  };
};
export default Access;
