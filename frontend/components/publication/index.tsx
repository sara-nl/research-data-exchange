import { Fragment } from 'react';
import Image from 'next/image';
import { Container, Row, Col } from 'react-bootstrap';
import { Dataset, Metadata } from '../../types';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import PublicationForm, { Values as FormValues } from './form';
import MetadataForm, { Values as MetadataFormValues } from './metadata';
import PublicationConfirmation from './confirmation';

type Props = {
  dataset: Dataset;
  baseUrl: string;
  submitUrl: string;
  metadataUrl: string;
  token: string;
  updateDataset: (dataset: Dataset) => void
};

dayjs.extend(relativeTime);

const Publication: React.FC<Props> = ({ dataset, baseUrl, submitUrl, metadataUrl, token, updateDataset }) => {

  type StoreValues = (values: FormValues) => Promise<Dataset>;
  const storeValues: StoreValues = async (values) =>
    fetch(submitUrl, {
      body: JSON.stringify(values),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token,
      },
      method: 'PATCH',
    }).then((res) =>
      res.ok
        ? Promise.resolve(res.json())
        : res.text().then(Promise.reject.bind(Promise)),
    );

  type StoreMetadataValues = (values: MetadataFormValues) => Promise<Metadata>;
  const storeMetadata: StoreMetadataValues = async (values) => {
    const url = `${metadataUrl}?${new URLSearchParams(values)}`
    return fetch(url, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token,
      },
      method: 'GET',
    }).then((res) =>
      res.ok
        ? Promise.resolve(res.json())
        : res.text().then(Promise.reject.bind(Promise))
    );
  }

  const addMetadatToDatatset = (metadata: Metadata) => {
    updateDataset({ ...dataset, ...metadata })
  }

  const timeSince = dayjs().from(dayjs(dataset.rdx_share.share_time), true);
  const pulicationTime = dayjs(dataset.published_at);

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
            {dataset.published ? (
              <PublicationConfirmation
                accessUrl={baseUrl + '/access/' + dataset.doi}
              />
            ) : (
              <Fragment>
                <MetadataForm
                  storeValues={storeMetadata}
                  onSuccessSubmission={(metadata) => addMetadatToDatatset(metadata)}
                />
                <PublicationForm
                  header={`Please fill in some details about the dataset. Once you hit
                "Publish", it will become possible for anyone to see the use
                conditions and request access to the dataset.`}
                  dataset={dataset}
                  storeValues={storeValues}
                  onSuccessSubmission={(dataset) => updateDataset(dataset)}
                />
              </Fragment>
            )}
          </Col>
          <Col sm={4} className="mt-3 left-side">
            <div className="dataset-content">
              <div className="mb-5">
                <div className="dataset">
                  <div className="dataset-title">
                    <h4>Dataset "{dataset.rdx_share.path}"</h4>
                    <p>
                      {/* Shared with Reaserch Data Exchange <br /> */}
                      <span className="text-nowrap font-weight-light">
                        Shared {timeSince} ago
                      </span>
                    </p>
                    <p>
                      <span className="text-nowrap font-weight-light">
                        {dataset.published ? `Published at ${pulicationTime}` : 'Not published'}
                      </span>
                    </p>
                  </div>
                  <div className="group mt-5">
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
                <div className="attached mt-5">
                  <div className="attached-title">
                    <h4>Attached use conditions</h4>
                  </div>
                  <div className="d-flex aligin-items-center mt-3">
                    <Image src="/images/file.svg" width="25px" height="25px" />
                    <span className="ml-2"><a href={dataset.conditions_url}>conditions.pdf</a></span>
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
