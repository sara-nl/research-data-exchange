import NavBarComponent from '../../components/navBar';
import DataStewardDashboard from '../../components/dashboard/dataSteward';
import Footer from '../../components/footer';
import Error, { ErrorProps } from 'next/error';
import Head from 'next/head';
import { GetServerSideProps } from 'next';
import { DatasetsPerPolicy } from '../../types';


type Props = {
  role: string;
  datasetsPerPolicy?: Array<DatasetsPerPolicy>
  error?: ErrorProps;
};

const Access: React.FC<Props> = ({ role, datasetsPerPolicy, error }) => {
  if (error) {
    return <Error {...error} title={error.title} />;
  } else {
    let dashboard;
    if (role === 'data_steward') {
      dashboard = <DataStewardDashboard datasetsPerPolicy={datasetsPerPolicy} />;
    }
    if (role === 'researcher') {
      // TODO in other issue
      dashboard = null;
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
        <NavBarComponent email={role} />
        {dashboard}
        <Footer />
      </main>
    );
  }
};

export const getServerSideProps: GetServerSideProps = async (context) => {
  const token = context.query['token'];
  const role = context.query['role'];

  if (role != 'data_steward' && role != 'researcher') {
    return {
      props: { error: { statusCode: 400, title: `role ${role} invalid` } },
    };
  }

  const obj = {
    method: 'GET',
    headers: {
      'Accept': 'application/json',
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + token
    }
  }

  const res = await fetch(`${process.env.RDX_BACKEND_URL}/api/dashboard/${role}/datasets`, obj);

  if (!res.ok) {
    const body = await res.json();
    return {
      props: { error: { statusCode: res.status, title: body.detail } },
    };
  }

  return {
    props: {
      role: role,
      datasetsPerPolicy: await res.json(),
    },
  };
};
export default Access;
