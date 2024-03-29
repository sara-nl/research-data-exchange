import NavBarComponent from '../../components/navBar';
import AccessComponent from '../../components/access';
import Footer from '../../components/footer';
import { AccessLicense, Dataset } from '../../types';
import Error, { ErrorProps } from 'next/error';
import Head from 'next/head';
import { GetServerSideProps } from 'next';

type Props = {
  dataset?: Dataset;
  tinkerLicense?: boolean;
  submitUrl: string;
  error?: ErrorProps;
};

const Access: React.FC<Props> = ({ dataset, tinkerLicense, submitUrl, error }) => {
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
        <AccessComponent dataset={dataset} tinkerLicense={tinkerLicense} submitUrl={submitUrl} />
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
    `${process.env.RDX_BACKEND_URL}/api/dataset/${encodeURIComponent(doi.join('/'))}/access`
  );

  if (!res.ok) {
    return {
      props: { error: { statusCode: res.status, title: await res.statusText } },
    };
  }

  var response = await res.json();

  const dataset: Dataset = {
    title: response.title,
    description: response.description,
    files: response.files,
    conditionsUrlProxy: transformDownloadLink(response.conditions_url),
    conditions_url: response.conditions_url + '/download',
    access_license_id: response.access_license_id,
  };

  return {
    props: {
      dataset: dataset,
      tinkerLicense: [AccessLicense.analyze_tinker_no_output_check, AccessLicense.analyze_tinker_with_output_check].includes(dataset.access_license_id),
      submitUrl: `${process.env.RDX_BACKEND_URL}/api/dataset/${encodeURIComponent(doi.join('/'))}/access`,
    },
  };
};
export default Access;
