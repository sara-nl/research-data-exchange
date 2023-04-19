import NavBarComponent from '../../../components/navBar';
import DatasetOverview from '../../../components/dashboard/dataset';
import Footer from '../../../components/footer';
import { Dataset, Job } from '../../../types';
import Head from 'next/head';
import { GetServerSideProps } from 'next';

type Props = {
  rdxDataset: Dataset;
  rdxJobs: Array<Job>;
};

const DatasetDashboard: React.FC<Props> = ({ rdxDataset, rdxJobs }) => {
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
      <NavBarComponent />
      <DatasetOverview dataset={rdxDataset} jobs={rdxJobs} />
      <Footer />
    </main>
  );
};


export const getServerSideProps: GetServerSideProps = async (context) => {
    const id = context.query['id'];
    const token = context.query['token'];

    const obj = {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
      }
    };

    const res = await fetch(`${process.env.RDX_BACKEND_URL}/api/dataset/${id}`, obj);

    if (!res.ok) {
      const body = await res.json();
      return {
        props: { error: { statusCode: res.status, title: body.detail } },
      };
    }

    const res_jobs = await fetch(`${process.env.RDX_BACKEND_URL}/api/dashboard/dataset/${id}/jobs`, obj);

    if (!res_jobs.ok) {
        const body = await res.json();
        return {
          props: { error: { statusCode: res.status, title: body.detail } },
        };
      }

    const rdxJobs = await res_jobs.json();

    return {
      props: {
        rdxDataset: await res.json(),
        rdxJobs: rdxJobs,
      },
    };
  };

export default DatasetDashboard;
