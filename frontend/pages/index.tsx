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
        <Row>
          <Col>
            <h1 className="display-2">
              &nbsp;&nbsp;Research <br /> &nbsp;&nbsp;Data <br /> eXchange
            </h1>
          </Col>
          <Col>
            <img
              className="d-block w-100 rounded mt-3"
              style={{ minWidth: '300px' }}
              src="https://amsterdameconomicboard.com/app/uploads/2021/02/amdex-header.jpg"
              alt="First slide"
            />
          </Col>
        </Row>
        <Row className="mt-5">
          <Col>
            <h2 className="display-4 text-center">
              For researchers and institutions who want to share data as much as
              possible but securely and controlled, taking into account legal
              and sovereignty issues.
            </h2>
          </Col>
        </Row>
        <Row className="mt-5">
          <Col>
            <p className="lead">
              Enforces the data sharing conditions and does not require blind
              trust in the users and/or a can of lawyers.
            </p>
          </Col>
          <Col>
            <p className="lead">
              Reduces effort of data stewards to adhere to FAIR principles and
              automates standard access patterns and make informed risk
              assessment.
            </p>
          </Col>
          <Col>
            <p className="lead">
              Enables researchers to safely share data and get acknowledgement
              for your work.
            </p>
          </Col>
        </Row>
      </Container>
    </section>
    <section className="mt-5">
      <Container>
        <Row>
          <Col></Col>
          <Col className="text-center">
            <img className="home-logo" src="/images/logos/eu.png" />
          </Col>
          <Col className="text-center">
            <img className="home-logo" src="/images/logos/surf.png" />
          </Col>
          <Col className="text-center">
            <img className="home-logo" src="/images/logos/amdex.svg" />
          </Col>
          <Col></Col>
        </Row>
      </Container>
    </section>

    <Footer />
  </main>
);

export default Home;
