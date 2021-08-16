import React, { useState } from 'react';
import Image from 'next/image';
import { Container, Row, Button, Col, Form } from 'react-bootstrap';

const files = [
  {
    conditionsDocument: 'https://xxxx.com/condtions.pdf',
    title: 'Sensitive Data 001',
    description: 'The SPSS file includes the raw data...',
    files: ['conditions.pdf', 'file1.xls', 'file2.xls', 'file50.xls'],
  },
];

const Publication: React.FC = () => {
  const [doi, setDoi] = useState('');
  const [title, setTitle] = useState('');
  const [authors, setAuthors] = useState('');
  const [description, setDescription] = useState('');

  const [validated, setValidated] = useState(false);

  const handleSubmit = (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    if (form.checkValidity() === false) {
      event.stopPropagation();
    } else {
      console.log('value', doi, title, authors, description);
    }
    console.log(doi);
    setValidated(true);
  };
  return (
    <section className="mt-5">
      <Container>
        <Row>
          <Col>
            <h2>Publish Dataset</h2>
          </Col>
        </Row>
        <Row className="publication-layout mt-5">
          <Col sm={8} className="mb-5 mt-3 right-side">
            <div className="mb-5">
              <p>
                Please fill in the metadata about the dataset. Once you hit
                "Publish". It will become possible for anyone to agree to the
                attached use conditions and download the dataset using the URL:
              </p>
              <div style={{ overflow: 'auto' }}>
                <a
                  href="https://rdx.labs.surf.nl/datasets/10.21942/uva.14680362.v3"
                  style={{ color: 'blue', wordWrap: 'break-word' }}
                >
                  https://rdx.labs.surf.nl/datasets/10.21942/uva.14680362.v3
                </a>
              </div>
            </div>
            <Form noValidate validated={validated} onSubmit={handleSubmit}>
              <Form.Row>
                <Form.Group as={Col} controlId="validationCustom01">
                  <Form.Label>DOI</Form.Label>
                  <Form.Control
                    required
                    type="text"
                    value={doi}
                    onChange={(e) => setDoi(e.target.value)}
                  />
                </Form.Group>
              </Form.Row>
              <Form.Row>
                <Form.Group as={Col} controlId="validationCustom02">
                  <Form.Label>Title</Form.Label>
                  <Form.Control
                    required
                    type="text"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                  />
                </Form.Group>
              </Form.Row>
              <Form.Row>
                <Form.Group as={Col} controlId="validationCustom02">
                  <Form.Label>Authors</Form.Label>
                  <Form.Control
                    required
                    type="text"
                    value={authors}
                    onChange={(e) => setAuthors(e.target.value)}
                  />
                </Form.Group>
              </Form.Row>
              <Form.Row>
                <Form.Group as={Col} controlId="exampleForm.ControlTextarea1">
                  <Form.Label>Description</Form.Label>
                  <Form.Control
                    required
                    as="textarea"
                    rows={3}
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                  />
                </Form.Group>
              </Form.Row>
              <Button type="submit" variant="primary">
                Publish
              </Button>
            </Form>
          </Col>
          <Col sm={4} className="mt-3 left-side">
            <div className="dataset-content">
              {files.map((item) => (
                <div className="mb-5">
                  <div className="dataset">
                    <div className="dataset-title">
                      <h4>Dataset "{item.title}"</h4>
                      <p>{item.description}</p>
                    </div>
                    <div className="group mt-3">
                      {item.files.map((file) => (
                        <div className="d-flex aligin-items-center mt-3">
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
                  <div className="attached mt-5">
                    <div className="attached-title">
                      <h4>Attached use conditions</h4>
                    </div>
                    <div className="d-flex aligin-items-center mt-3">
                      <Image
                        src="/images/file.svg"
                        width="25px"
                        height="25px"
                      />
                      <a href={item.conditionsDocument} target="_blank">
                        <span className="ml-2">conditions.pdf</span>
                      </a>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </Col>
        </Row>
      </Container>
    </section>
  );
};

export default Publication;
