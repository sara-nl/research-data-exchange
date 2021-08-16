import React from 'react';
import NavBarComponent from '../components/navBar';
import Publication from '../components/publication';
import Footer from '../components/footer';
import Head from 'next/head';

const Home: React.FC = () => (
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
    <Publication />
    <Footer />
  </main>
);

export default Home;
