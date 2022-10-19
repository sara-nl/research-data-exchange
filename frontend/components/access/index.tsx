import React, { useState } from 'react';
import Image from 'next/image';
import { Dataset } from '../../types';
import AccessForm, { Values as FormValues } from './form';
import { Container, Row, Button, Col, Form, InputGroup } from 'react-bootstrap';
import { Alert } from 'react-bootstrap';

type Props = {
  dataset?: Dataset;
  submitUrl: string;
};

const Access: React.FC<Props> = ({ dataset, submitUrl }) => {
  const [submitted, setSubmitted] = useState<Boolean | undefined>(false);
  type StoreValues = (values: FormValues) => Promise<FormValues>;
  // FIXME Can be extracted as common code
  const storeValues: StoreValues = async (values) =>
    fetch(submitUrl, {
      body: JSON.stringify(values),
      headers: {
        'Content-Type': 'application/json',
      },
      method: 'POST',
    }).then((res) =>
      res.ok
        ? Promise.resolve(values)
        : res.text().then(Promise.reject.bind(Promise)),
    );

  return (
    <section>
      <Container>
        <Row className="access-layout my-5">
          <Col sm={8} className="right-side pr-3">
            {Boolean(submitted) ? (
              <Alert variant="success" className="px-5">
                <h4 className="alert-heading">Request submitted!</h4>
                <p>
                  📬 We've sent an email with the access link for downloading
                  dataset files and the conditions document to your address.
                </p>
                <p>
                  If the email doesn't arrive within a few minutes, please check
                  your spam folder.
                </p>
              </Alert>
            ) : (
              <AccessForm
                dataset={dataset}
                storeValues={storeValues}
                onSuccessSubmission={(values) => {
                  setSubmitted(values != undefined);
                }}
              />
            )}
          </Col>
          <Col sm={4} className="left-side">
            <div className="dataset-content">
              <div>
                <div className="dataset mb-5">
                  <div className="dataset-title">
                    <h4>{dataset.title}</h4>
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
                        <span className="dataset-file ml-2">{file}</span>
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
