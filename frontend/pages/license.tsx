import React from 'react';
import Footer from '../components/footer';
import Head from 'next/head';
import { Carousel, Col, Container, Row } from 'react-bootstrap';

const Home: React.FC = () => (
  <main>
    <Head>
      <link
        rel="shortcut icon"
        href="/images/rdx-logo.png"
        type="image/x-icon"
      />
      <title>Research Data Exchange MVP</title>
    </Head>
    <section className="mt-5">
      <Container>
        <Row className="mt-5">
          <Col>
            <h2 className="display-4 text-center">Research Data Exchange</h2>
            <p className="lead mt-5">
              Access to this data is provided through Research Data Exchange
              (RDX). Privacy or other considerations may prevent the public
              disclosure of the data: a description of the data is published,
              but the dataset itself is only accessible through RDX.
            </p>
            <p className="lead mt-5">
              If based on the description of the data you feel the data could be
              useful to you in your research project, you are free to follow the
              link under “references” and request the data be made available to
              you. If it is possible to make the data available, a RDX license
              will apply. The terms of this license will need to be agreed upon,
              prior to the data being made available to you.
            </p>
          </Col>
        </Row>
      </Container>
    </section>
    <Footer />
  </main>
);

export default Home;
