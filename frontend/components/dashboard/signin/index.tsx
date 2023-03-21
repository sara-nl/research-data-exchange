import React, { useState } from 'react';
import Image from 'next/image';
import { AccessLicenseUtil, Dataset } from '../../../types';
import SignInForm, { Values as FormValues } from './form';
import { Container, Row, Button, Col, Form, InputGroup } from 'react-bootstrap';
import { Alert } from 'react-bootstrap';
import { InfoCircle } from 'react-bootstrap-icons';

type Props = {
  dataset?: Dataset;
  submitUrl: string;
};

const SignIn: React.FC<Props> = ({ dataset, submitUrl }) => {
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
                  📬 We've sent an email with the access link to the RDX dashboard.
                </p>
                <p>
                  If the email doesn't arrive within a few minutes, please check
                  your spam folder.
                </p>
              </Alert>
            ) : (
              <SignInForm
                storeValues={storeValues}
                onSuccessSubmission={(values) => {
                  setSubmitted(values != undefined);
                }}
              />
            )}
          </Col>
        </Row>
      </Container>
    </section>
  );
};

export default SignIn;
