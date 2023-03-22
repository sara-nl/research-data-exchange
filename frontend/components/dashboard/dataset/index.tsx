import React, { useState } from 'react';
import Image from 'next/image';
import { Dataset, Job, AccessLicense } from '../../../types';
import JobOverview from './table';
import { Container, Row, Button, Col, Form, InputGroup } from 'react-bootstrap';
import { Alert } from 'react-bootstrap';

type Props = {
  dataset?: Dataset;
  jobs?: Array<Job>;
};

const DatasetOverview: React.FC<Props> = ({ dataset, jobs }) => {
  return (
    <section>
      <Container>
        <Row className="mt-5">
          <Col sm={8} className="right-side pr-3">
            <Row>
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

export default DatasetOverview;
