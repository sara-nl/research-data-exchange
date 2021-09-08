import React, { useState } from 'react';
import Image from 'next/image';
import { Dataset } from '../../types';
import Error, { ErrorProps } from 'next/error';
import { Container, Row, Button, Col, Form, InputGroup } from 'react-bootstrap';
import dynamic from 'next/dynamic';
import { Alert } from 'react-bootstrap';

const PDFViewer = dynamic(() => import('../pdf-view'), {
  ssr: false,
});

type Props = {
  dataset?: Dataset;
  submitUrl: string;
  error?: ErrorProps;
};

const Access: React.FC<Props> = ({ dataset, submitUrl }) => {
  const [scrollBottom, setScrollBottom] = useState(0);
  const [agree, setAgree] = useState(false);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');

  const [validated, setValidated] = useState(false);
  const handleSubmit = (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    if (form.checkValidity() === false) {
      event.stopPropagation();
    } else {
      console.log('value', agree, name, email);
    }
    setValidated(true);
  };

  const onDocumentLoadSuccess = () => {
    console.log('success load');
  };

  const handleScroll = (e) => {
    const { target } = e;
    if (target.scrollHeight - target.scrollTop === target.clientHeight) {
      let i = scrollBottom;
      i += 1;
      setScrollBottom(i);
    }
  };

  return (
    <section className="mt-5">
      <Container>
        <Row>
          <Col>
            <h2>You are about to request access to a sensitive dataset</h2>
          </Col>
        </Row>
        <Row className="access-layout mt-3">
          <Col sm={8} className="mb-5 mt-3 right-side">
            <div>
              <Alert variant="info" className="px-5">
                Please read the following use conditions carefully. You can
                download the dataset only if you fully agree to them. Your name
                and email address along with the fact of agreement will be
                stored in our system in order to ensure that the data is used
                appropriately.
              </Alert>
            </div>
            <div className="pdf-view mb-5" onScroll={handleScroll}>
              <PDFViewer conditionsUrl={dataset.conditionsUrl} />
            </div>
            <Form noValidate validated={validated} onSubmit={handleSubmit}>
              <Form.Row>
                <Form.Group as={Col} controlId="formBasicCheckbox">
                  <Form.Check
                    type="checkbox"
                    label="I hereby agree to the terms and conditions *"
                    checked={agree}
                    disabled={scrollBottom >= 1 ? false : true}
                    onChange={(e) => setAgree(e.target.checked)}
                  />
                </Form.Group>
              </Form.Row>
              <Form.Row>
                <Form.Group as={Col} controlId="validationCustom01">
                  <Form.Label>Name</Form.Label>
                  <Form.Control
                    required
                    type="text"
                    className="name-text"
                    value={name}
                    disabled={!agree}
                    onChange={(e) => setName(e.target.value)}
                  />
                </Form.Group>
              </Form.Row>
              <Form.Row>
                <Form.Group as={Col} controlId="validationCustom02">
                  <Form.Label>Email</Form.Label>
                  <Form.Control
                    required
                    type="email"
                    className="email-text"
                    value={email}
                    disabled={!agree}
                    onChange={(e) => setEmail(e.target.value)}
                  />
                </Form.Group>
              </Form.Row>
              <Button type="submit" variant="primary">
                Request Access
              </Button>
            </Form>
          </Col>
          <Col sm={4} className="mt-3 left-side">
            <div className="dataset-content">
              <div>
                <div className="dataset mb-5">
                  <div className="dataset-title">
                    <h4>Dataset "{dataset.title}"</h4>
                    <p>{dataset.description}</p>
                  </div>
                  <div className="group mt-3">
                    {dataset.files.map((file) => (
                      <div
                        key={file}
                        className="d-flex aligin-items-center mt-3"
                      >
                        <Image
                          src="/images/file.svg"
                          width="25px"
                          height="25px"
                        />
                        <span className="ml-2">{file}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
            <div className="mt-5">
              <div className="attached-title">
                <h4 style={{ color: '#76BDF3' }}>
                  Use conditions in a nutshell
                </h4>
              </div>
              <div className="d-flex align-items-center mt-3 attached-item">
                <Image src="/images/warning.png" width="50px" height="50px" />
                <span className="ml-2" style={{ color: '#424FB0' }}>
                  You are still obliged to read and agree to the full text.
                </span>
              </div>
              <div className="d-flex align-items-center mt-3 attached-item">
                <Image src="/images/r-icon.svg" width="40px" height="40px" />
                <span className="ml-3">
                  Always give credits to the authors.
                </span>
              </div>
              <div className="d-flex align-items-center mt-3 attached-item">
                <Image src="/images/remove.jpg" width="70px" height="70px" />
                <span className="ml-2">
                  Remove the files as soon as you are done with your research.
                </span>
              </div>
              <div className="d-flex align-items-center mt-3 attached-item">
                <Image src="/images/personal.jpg" width="70px" height="70px" />
                <span className="ml-2">
                  The dataset files should never leave your personal computer.
                </span>
              </div>
            </div>
          </Col>
        </Row>
      </Container>
    </section>
  );
};

export default Access;
