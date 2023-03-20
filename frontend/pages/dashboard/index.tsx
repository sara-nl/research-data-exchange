import NavBarComponent from '../../components/navBar';
import SignInComponent from '../../components/dashboard/signin';
import Footer from '../../components/footer';
import { Dataset } from '../../types';
import Error, { ErrorProps } from 'next/error';
import Head from 'next/head';
import { GetServerSideProps } from 'next';

type Props = {
  submitUrl?: string;
};

const Access: React.FC<Props> = ({ submitUrl }) => {
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
      <SignInComponent submitUrl={submitUrl} />
      <Footer />
    </main>
  );
};


export const getServerSideProps: GetServerSideProps = async (context) => {
  return {
    props: {
      submitUrl: `${process.env.RDX_BACKEND_URL}/api/dashboard/signin`,
    },
  };
};

export default Access;
