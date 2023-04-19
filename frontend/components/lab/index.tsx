import React, { useState } from 'react';
import Image from 'next/image';
import { Dataset, Job, AccessLicense } from '../../types';
import JobForm, { Values as FormValues } from './form';
import JobOverview from './table';
import { Container, Row, Button, Col, Form, InputGroup } from 'react-bootstrap';
import { Alert } from 'react-bootstrap';

type Props = {
  dataset?: Dataset;
  jobs?: Array<Job>;
  submitUrl: string;
  token: string;
};

const Lab: React.FC<Props> = ({ dataset, jobs, submitUrl, token }) => {
  const [submitted, setSubmitted] = useState<Boolean | undefined>(false);
  type StoreValues = (values: FormValues) => Promise<FormValues>;
  // FIXME Can be extracted as common code
  const storeValues: StoreValues = async (values) =>
    fetch(submitUrl, {
      body: JSON.stringify(values),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token,
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
            <Row>
              {Boolean(submitted) ? (
                <Alert variant="success" className="px-5">
                  <h4 className="alert-heading">Request submitted!</h4>
                  <p>
                    We've submitted your job to analyze the dataset. Depending on your algorithm, the job can take fifteen minutes up to several hours.
                  </p>
                  {dataset.access_license_id == AccessLicense.analyze_blind_no_output_check ? (
                    <p>
                      When the results are ready, you will receive an email.
                      If the email doesn't arrive within a couple hours, please check
                      your spam folder.
                    </p>
                  ) : (
                    <p>
                      When the results are ready, the owner of this dataset will receive an email.
                      They will verify the output of your analysis and decide whether to share the results with you.
                    </p>
                  )
                  }
                </Alert>
              ) : (
                <JobForm
                  storeValues={storeValues}
                  onSuccessSubmission={(values) => {
                    setSubmitted(values != undefined);
                  }}
                />
              )}
            </Row>
            <Row className="mt-5">
              <JobOverview
                jobs={jobs}
              />


            </Row>
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
                    <div className="dataset-title">
                      <h5>Files</h5>
                    </div>
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
          </Col>
        </Row>
      </Container>
    </section>
  );
};

export default Lab;
