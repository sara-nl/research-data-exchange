import NavBarComponent from '../../components/navBar';
import Footer from '../../components/footer';
import Error, { ErrorProps } from 'next/error';
import Head from 'next/head';
import { GetServerSideProps } from 'next';

type Props = {
  role?: string;
  submitUrl?: string;
  error?: ErrorProps;
};

const Access: React.FC<Props> = ({ role, submitUrl, error }) => {
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
        <NavBarComponent email={role} />
        {/* TODO */}
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

  return {
    props: {
      role: role,
    //   submitUrl: `${process.env.RDX_BACKEND_URL}/api/dataset/${encodeURIComponent(doi.join('/'))}/access`,
    },
  };
};
export default Access;
