import React, { useState } from 'react';
import Image from 'next/image';
import { Container, Row, Button, Col, Alert } from 'react-bootstrap';
import { Share } from '../../types';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import PublicationForm, { Values as FormValues } from './form';
import PublicationConfirmation from './confirmation';

type Props = {
  share: Share;
  baseUrl: string;
  submitUrl: string;
};

dayjs.extend(relativeTime);

const Publication: React.FC<Props> = ({ share, baseUrl, submitUrl }) => {
  const [publishedDoi, setPublishedDoi] = useState<string | undefined>(
    undefined,
  );

  type StoreValues = (values: FormValues) => Promise<FormValues>;

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

  const timeSince = dayjs().from(dayjs(share.createdAt), true);

  return (
    <section className="mt-5">
      <Container>
        <Row>
          <Col>
            <h2>Publish dataset</h2>
          </Col>
        </Row>
        <Row className="publication-layout mt-5">
          <Col sm={8} className="mb-5 mt-3 right-side">
            {Boolean(publishedDoi) ? (
              <PublicationConfirmation
                accessUrl={baseUrl + '/' + publishedDoi}
              />
            ) : (
              <React.Fragment>
                <PublicationForm
                  header={`Please fill in some details about the dataset. Once you hit
                "Publish", it will become possible for anyone to see the use
                conditions and request access to the dataset.`}
                  storeValues={storeValues}
                  onSuccessSubmission={(values) => setPublishedDoi(values.doi)}
                />
              </React.Fragment>
            )}
          </Col>
          <Col sm={4} className="mt-3 left-side">
            <div className="dataset-content">
              <div className="mb-5">
                <div className="dataset">
                  <div className="dataset-title">
                    <h4>Dataset "{share.path}"</h4>
                    <p>
                      {/* Shared with Reaserch Data Exchange <br /> */}
                      <span className="text-nowrap font-weight-light">
                        Shared {timeSince} ago
                      </span>
                    </p>
                  </div>
                  <div className="group mt-5">
                    {share.files.map((file) => (
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
                <div className="attached mt-5">
                  <div className="attached-title">
                    <h4>Attached use conditions</h4>
                  </div>
                  <div className="d-flex aligin-items-center mt-3">
                    <Image src="/images/file.svg" width="25px" height="25px" />
                    <span className="ml-2">{share.conditionsDocument}</span>
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

export default Publication;
