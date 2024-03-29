import React from 'react';
import Footer from '../components/footer';
import Head from 'next/head';
import { Card, Col, Container, ListGroup, Row } from 'react-bootstrap';
import NavBarComponent from '../components/navBar';

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
    <NavBarComponent />
    <section className="mt-5">
      <Container>
        <Row className="mt-5">
          <Col xs={10} className="m-2">
            <h2 className="display-4">RDX Access Policies</h2>
            <p>
              There are two policies for data access in RDX.
              The researcher and data steward choose the appropriate policy for a dataset before making it available.
              It is essential to understand the similarities and differences between the two policies.
            </p>
          </Col>
        </Row>
        <Row className="mt-3">
          <Col>
            <Card className="m-4" >
              <Card.Header as="h3">Sign + Download</Card.Header>
              <Card.Img className="pt-3 pb-3" height="200rem" variant='top' src="/images/download.svg"></Card.Img>
              <ListGroup>
                <ListGroup.Item>To access a dataset with a <em>sign + download</em> access policy, a data consumer is required to digitally sign the conditions specified by the data provider.</ ListGroup.Item>
                <ListGroup.Item>After agreeing to the conditions, the data consumer will receive a link to download the dataset for analysis to their own device or computer systems.</ ListGroup.Item>
                <ListGroup.Item>The RDX notifies the data provider when someone has requested access to their dataset.</ ListGroup.Item>
                <ListGroup.Item>No further checks or monitoring is done by the RDX.</ ListGroup.Item>
              </ListGroup>
            </Card>
          </Col>
          <Col md>
            <Card className="m-4" >
              <Card.Header as="h3">Sign + Analyze</Card.Header>
              <Card.Img height="200rem" variant='top' src="/images/analyze.svg"></Card.Img>
              <ListGroup>
                <ListGroup.Item>To access a dataset with a <em>sign + analyze</em> access policy, a data consumer is required to digitally sign the conditions specified by the data provider.</ListGroup.Item>
                <ListGroup.Item>After agreeing to the conditions, the data consumer will receive a link to analyze the dataset within a secure analysis environment. This analysis environment runs on the infrastructure of a trusted third party, and the data consumer will not be able to download the dataset to their own device.</ListGroup.Item>
                <ListGroup.Item>The RDX notifies the data provider when someone has requested access to their dataset.</ListGroup.Item>
                <ListGroup.Item>The RDX provides the data provider with an overview of which algorithms are run on their dataset and what the output of the analysis was.</ListGroup.Item>
              </ListGroup>
            </Card>
          </Col>
        </Row>
      </Container>
    </section>
    <Footer />
  </main >
);

export default Home;
